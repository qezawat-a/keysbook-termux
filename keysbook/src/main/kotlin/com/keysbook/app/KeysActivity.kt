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

    /**
     * Copies the full stored key.
     *
     * The entry is re-read from the vault by id at click time instead of using the
     * captured object, so a recycled list row can never copy the wrong key. The
     * confirmation shows a masked form of the value so it is obvious that the
     * whole secret (not the masked label on screen) went to the clipboard.
     */
    /**
     * Copies the full stored key.
     *
     * The entry is re-read from the vault by id at click time instead of using the
     * captured object, so a recycled list row can never copy the wrong key. The
     * write is read back before reporting success; if the system refuses it, the
     * key is shown pre-selected so it can still be copied by hand.
     */
    private fun copyKey(entryId: String) {
        val stored = vault.find(entryId)
        val apiKey = stored?.apiKey.orEmpty()
        if (apiKey.isBlank()) {
            toast(getString(R.string.copy_empty))
            return
        }

        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // The clip is deliberately NOT flagged sensitive: a sensitive clip shows
        // "content hidden" on Android 13+, which makes a good copy look broken.
        val clip = ClipData.newPlainText(getString(R.string.copied_label), apiKey)
        val wrote = runCatching { cm.setPrimaryClip(clip) }.isSuccess

        val landed = wrote && runCatching {
            cm.primaryClip?.getItemAt(0)?.text?.toString() == apiKey
        }.getOrDefault(false)

        if (landed) {
            toast(getString(R.string.copied_full, AppState.maskKey(apiKey)))
        } else {
            showKeyFallback(apiKey)
        }
    }

    /** Manual copy path for devices that reject clipboard writes. */
    private fun showKeyFallback(apiKey: String) {
        val field = EditText(this)
        field.setText(apiKey)
        field.setSelection(0, apiKey.length)
        field.isSingleLine = false
        field.setPadding(48, 32, 48, 32)
        AlertDialog.Builder(this)
            .setTitle(R.string.copy_key)
            .setMessage(R.string.copy_manual_hint)
            .setView(field)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

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
            val keyText = view.findViewById<TextView>(R.id.key)
            keyText.text = AppState.maskKey(entry.apiKey)

            view.findViewById<Button>(R.id.copy).setOnClickListener { copyKey(entry.id) }
            // Long-pressing the key itself is a second, larger copy target.
            keyText.setOnLongClickListener {
                copyKey(entry.id)
                true
            }
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
