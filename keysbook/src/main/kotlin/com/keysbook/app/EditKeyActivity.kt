package com.keysbook.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

/** Add / edit a key. Mirrors the Python app's provider-then-key flow. */
class EditKeyActivity : Activity() {

    private lateinit var vault: Vault
    private lateinit var title: TextView
    private lateinit var provider: Spinner
    private lateinit var label: EditText
    private lateinit var url: EditText
    private lateinit var key: EditText
    private lateinit var notes: EditText

    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_key)

        vault = AppState.get(this)
        if (!vault.unlocked) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        title = findViewById(R.id.title)
        provider = findViewById(R.id.provider)
        label = findViewById(R.id.label)
        url = findViewById(R.id.url)
        key = findViewById(R.id.key)
        notes = findViewById(R.id.notes)

        provider.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Providers.ALL.map { it.name }
        )

        // Choosing a provider prefills the known Base URL.
        provider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val name = Providers.ALL[pos].name
                // Ignore the callback that just confirms the current selection,
                // including the one fired while restoring a saved key.
                if (name == lastProvider) return
                lastProvider = name
                val def = Providers.ALL[pos].baseUrl
                if (def.isNotBlank()) url.setText(def)
            }

            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }

        editingId = intent.getStringExtra(EXTRA_ID)
        editingId?.let { load(it) }

        findViewById<Button>(R.id.save).setOnClickListener { save() }
        findViewById<Button>(R.id.cancel).setOnClickListener { finish() }
    }

    private fun load(id: String) {
        val e = vault.find(id) ?: return
        title.setText(R.string.edit_key)
        val idx = Providers.ALL.indexOfFirst { it.name == e.provider }
        if (idx >= 0) {
            skipNextAutofill = true
            provider.setSelection(idx)
        }
        label.setText(e.label)
        url.setText(e.baseUrl)
        key.setText(e.apiKey)
        notes.setText(e.notes)
    }

    private fun save() {
        val providerName = provider.selectedItem?.toString().orEmpty()
        val apiKey = key.text.toString().trim()
        val baseUrl = url.text.toString().trim()

        if (providerName.isBlank() || apiKey.isBlank()) {
            toast(getString(R.string.missing_fields))
            return
        }
        if (baseUrl.isBlank()) {
            toast(getString(R.string.custom_needs_url))
            return
        }

        val entry = Vault.Entry(
            id = editingId ?: Vault.newId(),
            provider = providerName,
            label = label.text.toString().trim(),
            baseUrl = baseUrl,
            apiKey = apiKey,
            notes = notes.text.toString().trim()
        )
        if (editingId == null) vault.add(entry) else vault.update(entry)
        toast(getString(R.string.saved))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_ID = "entry_id"
    }
}
