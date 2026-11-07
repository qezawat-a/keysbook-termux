package com.keysbook.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** List saved API keys. Equivalent to the Python app's key list screen. */
class KeysActivity : Activity() {

    private lateinit var vault: Vault
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var adapter: KeyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keys)

        vault = AppState.get(this)
        if (!vault.unlocked) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        listView = findViewById(R.id.list)
        emptyView = findViewById(R.id.empty)
        findViewById<Button>(R.id.lock).setOnClickListener { lockVault() }
        findViewById<Button>(R.id.add).setOnClickListener { addKey() }

        adapter = KeyAdapter()
        listView.adapter = adapter
        listView.emptyView = emptyView
        listView.setOnItemClickListener { _, _, pos, _ ->
            editKey(adapter.entries[pos])
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        adapter.entries = vault.entries
        adapter.notifyDataSetChanged()
    }

    private fun addKey() {
        startActivity(Intent(this, EditKeyActivity::class.java))
    }

    private fun editKey(entry: Vault.Entry) {
        val intent = Intent(this, EditKeyActivity::class.java).apply {
            putExtra(EditKeyActivity.EXTRA_ID, entry.id)
        }
        startActivity(intent)
    }

    private fun confirmDelete(entry: Vault.Entry) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setPositiveButton(R.string.delete) { _, _ ->
                vault.remove(entry.id)
                refreshList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun lockVault() {
        vault.lock()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    private inner class KeyAdapter : BaseAdapter() {
        var entries = emptyList<Vault.Entry>()

        override fun getCount(): Int = entries.size
        override fun getItem(pos: Int): Any = entries[pos]
        override fun getItemId(pos: Int): Long = pos.toLong()

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@KeysActivity).inflate(R.layout.item_key, parent, false)
            val entry = entries[pos]
            view.findViewById<TextView>(R.id.provider).text = entry.provider
            view.findViewById<TextView>(R.id.label).text = entry.label
            view.findViewById<TextView>(R.id.url).text = entry.baseUrl
            val maskedKey = if (entry.apiKey.length > 8)
                entry.apiKey.take(4) + "****" + entry.apiKey.takeLast(4)
            else
                entry.apiKey
            view.findViewById<TextView>(R.id.key).text = maskedKey

            view.findViewById<Button>(R.id.copy).setOnClickListener {
                if (entry.apiKey.isBlank()) {
                    toast(getString(R.string.copy_empty))
                } else {
                    copyToClipboard(getString(R.string.copied_label), entry.apiKey)
                    toast(getString(R.string.copied_full, maskedKey))
                }
            }
            view.findViewById<Button>(R.id.copy_url).setOnClickListener {
                if (entry.baseUrl.isBlank()) {
                    toast(getString(R.string.custom_needs_url))
                } else {
                    copyToClipboard(getString(R.string.base_url), entry.baseUrl)
                    toast(getString(R.string.base_url_copied, entry.baseUrl))
                }
            }
            view.findViewById<Button>(R.id.models).setOnClickListener {
                startActivity(Intent(this@KeysActivity, ModelsActivity::class.java).apply {
                    putExtra(ModelsActivity.EXTRA_ID, entry.id)
                })
            }
            view.findViewById<Button>(R.id.edit).setOnClickListener { editKey(entry) }
            view.findViewById<Button>(R.id.delete).setOnClickListener { confirmDelete(entry) }
            return view
        }
    }
}