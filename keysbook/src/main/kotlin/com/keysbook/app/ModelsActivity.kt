package com.keysbook.app

import android.app.Activity
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

        list.setOnItemClickListener { _, _, position, _ -> testModel(models[position]) }

        loadModels()
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdownNow()
    }

    private fun loadModels() {
        status.text = getString(R.string.loading_models)
        models.clear()
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
        status.text = getString(R.string.testing, model)

        // Visual feedback: mark the tapped row as testing
        val position = models.indexOf(model)
        if (position >= 0) {
            ui.post {
                val child = list.getChildAt(position - list.firstVisiblePosition)
                child?.findViewById<TextView>(R.id.name)?.setTextColor(
                    getColor(R.color.text_muted)
                )
            }
        }

        io.execute {
            val result = ModelClient.testModel(entry.provider, entry.baseUrl, entry.apiKey, model)
            ui.post {
                if (isFinishing) return@post
                val msg = if (result.error == null) {
                    getString(R.string.model_ok, model)
                } else {
                    getString(R.string.model_failed, model)
                }
                status.text = msg
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                // Reset text color after test
                if (position >= 0) {
                    val child = list.getChildAt(position - list.firstVisiblePosition)
                    child?.findViewById<TextView>(R.id.name)?.setTextColor(
                        getColor(R.color.text)
                    )
                }
            }
        }
    }

    private inner class ModelAdapter : BaseAdapter() {
        override fun getCount(): Int = models.size
        override fun getItem(position: Int): Any = models[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ModelsActivity)
                .inflate(R.layout.item_model, parent, false)
            view.findViewById<TextView>(R.id.name).text = models[position]

            // Copy model name to clipboard
            view.findViewById<Button>(R.id.copy).setOnClickListener {
                val modelName = models[position]
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(
                    getString(R.string.model_copy_label), modelName)
                clipboard.primaryClip = clip
                Toast.makeText(this@ModelsActivity,
                    getString(R.string.model_copied, modelName), Toast.LENGTH_SHORT).show()
            }
            return view
        }
    }

    companion object {
        const val EXTRA_ID = "entry_id"
    }
}
