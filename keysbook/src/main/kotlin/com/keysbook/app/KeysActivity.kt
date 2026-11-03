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
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

/** Key list screen: the Python app's main menu table. */
class KeysActivity : Activity() {

    private lateinit var vault: Vault
    private lateinit var list: ListView
    private lateinit var empty: TextView
    private lateinit var adapter: KeyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keys)

        vault = AppState.get(this)
        if (!vault.unlocked) {
            // Session was locked; send the user back to the password screen.
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        list = findViewById(R.id.list)
        empty = findViewById(R.id.empty)
        adapter = KeyAdapter()
        list.adapter = adapter

        findViewById<Button>(R.id.add).setOnClickListener {
            startActivity(Intent(this, EditKeyActivity::class.java))
        }
        findViewById<Button>(R.id.lock).setOnClickListener {
            AppState.lock()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.notifyDataSetChanged()
        val isEmpty = vault.entries.isEmpty()
        empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        list.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(entry: Vault.Entry) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                vault.remove(entry.id)
                refresh()
            }
            .show()
    }

    private fun copyKey(entry: Vault.Entry) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("KeysBook key", entry.apiKey))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private inner class KeyAdapter : BaseAdapter() {
        override fun getCount(): Int = vault.entries.size
        override fun getItem(position: Int): Any = vault.entries[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@KeysActivity)
                .inflate(R.layout.item_key, parent, false)
            val entry = vault.entries[position]

            view.findViewById<TextView>(R.id.provider).text = entry.provider
            view.findViewById<TextView>(R.id.label).text = entry.label
            view.findViewById<TextView>(R.id.url).text =
                entry.baseUrl.ifBlank { getString(R.string.custom_needs_url) }
            view.findViewById<TextView>(R.id.key).text = AppState.maskKey(entry.apiKey)

            view.findViewById<Button>(R.id.copy).setOnClickListener { copyKey(entry) }
            view.findViewById<Button>(R.id.edit).setOnClickListener {
                startActivity(
                    Intent(this@KeysActivity, EditKeyActivity::class.java)
                        .putExtra(EditKeyActivity.EXTRA_ID, entry.id)
                )
            }
            view.findViewById<Button>(R.id.delete).setOnClickListener { confirmDelete(entry) }
            view.findViewById<Button>(R.id.models).setOnClickListener {
                startActivity(
                    Intent(this@KeysActivity, ModelsActivity::class.java)
                        .putExtra(ModelsActivity.EXTRA_ID, entry.id)
                )
            }
            return view
        }
    }
}
