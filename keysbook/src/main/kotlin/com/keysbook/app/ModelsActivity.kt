package com.keysbook.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * Model browser: fetches the models a key can actually reach, and tests any
 * model on tap. Network work runs off the UI thread.
 */
class ModelsActivity : Activity() {

    private lateinit var vault: Vault
    private lateinit var entry: Vault.Entry
    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var list: ListView
    private lateinit var adapter: ModelAdapter

    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private val models = mutableListOf<String>()

    /** model -> true (works) / false (fails), so the row can show the last result. */
    private val results = mutableMapOf<String, Boolean>()
    private val testing = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_models)

        vault = AppState.get(this)
        val id = intent.getStringExtra(EXTRA_ID)
        val found = id?.let { vault.find(it) }
        if (!vault.unlocked || found == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        entry = found

        title = findViewById(R.id.title)
        status = findViewById(R.id.status)
        list = findViewById(R.id.list)
        adapter = ModelAdapter()
        list.adapter = adapter

        title.text = getString(R.string.models_title, entry.provider)
        findViewById<Button>(R.id.refresh).setOnClickListener { loadModels() }

        // Show the endpoint this key is using, with a one-tap copy.
        val baseUrlView = findViewById<TextView>(R.id.base_url)
        baseUrlView.text = entry.baseUrl.ifBlank { getString(R.string.custom_needs_url) }
        findViewById<Button>(R.id.copy_base).setOnClickListener { copyBaseUrl() }

        loadModels()
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdownNow()
    }

    private fun loadModels() {
        status.text = getString(R.string.loading_models)
        models.clear()
        results.clear()
        testing.clear()
        adapter.notifyDataSetChanged()

        io.execute {
            val result = ModelClient.fetchModels(entry.provider, entry.baseUrl, entry.apiKey)
            ui.post {
                if (isFinishing) return@post
                models.clear()
                models.addAll(result.models)
                adapter.notifyDataSetChanged()

                status.text = when {
                    result.error != null -> result.error
                    models.isEmpty() -> getString(R.string.no_models)
                    else -> getString(R.string.tap_model_hint)
                }
            }
        }
    }

    private fun testModel(model: String) {
        if (!testing.add(model)) return
        results.remove(model)
        status.text = getString(R.string.testing, model)
        adapter.notifyDataSetChanged()

        io.execute {
            val result = ModelClient.testModel(entry.provider, entry.baseUrl, entry.apiKey, model)
            ui.post {
                testing.remove(model)
                if (isFinishing) return@post

                val ok = result.error == null
                results[model] = ok
                adapter.notifyDataSetChanged()

                val msg = if (ok) {
                    getString(R.string.model_ok, model)
                } else {
                    getString(R.string.model_failed, model) + "\n\n" + result.error
                }
                status.text = msg

                AlertDialog.Builder(this)
                    .setTitle(R.string.test_result_title)
                    .setMessage(msg)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }
    }

    private fun copyModel(model: String) {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(getString(R.string.model_copy_label), model))
        toast(getString(R.string.model_copied, model))
    }

    private fun copyBaseUrl() {
        val text = entry.baseUrl
        if (text.isBlank()) {
            toast(getString(R.string.custom_needs_url))
            return
        }
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(getString(R.string.base_url), text))
        toast(getString(R.string.base_url_copied, text))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private inner class ModelAdapter : BaseAdapter() {
        override fun getCount(): Int = models.size
        override fun getItem(position: Int): Any = models[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ModelsActivity)
                .inflate(R.layout.item_model, parent, false)

            val model = models[position]
            val name = view.findViewById<TextView>(R.id.name)
            name.text = model

            // Row state: testing / ok / failed
            val colorRes = when {
                model in testing -> R.color.text_muted
                results[model] == true -> R.color.accent
                results[model] == false -> R.color.danger
                else -> R.color.text
            }
            name.setTextColor(getColor(colorRes))

            // Tapping the row (anywhere except the copy button) tests the model.
            view.setOnClickListener { testModel(model) }

            // Copy button copies the model name/ID (not the Base URL).
            view.findViewById<Button>(R.id.copy).setOnClickListener { copyModel(model) }

            return view
        }
    }

    companion object {
        const val EXTRA_ID = "entry_id"
    }
}