package com.keysbook.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*

/** Add / edit a key. Mirrors the Python app's provider-then-key flow. */
class EditKeyActivity : Activity() {

    private lateinit var vault: Vault
    private lateinit var title: TextView
    private lateinit var provider: Spinner
    private lateinit var label: EditText
    private lateinit var url: EditText
    private lateinit var keysContainer: LinearLayout
    private lateinit var addKeyButton: Button
    private lateinit var notes: EditText

    private var editingId: String? = null

    /** One EditText per key box currently shown on screen. */
    private val keyInputs = mutableListOf<EditText>()

    /**
     * Name of the provider currently reflected in the Base URL field. Spinner
     * callbacks fire on layout (including while restoring a saved key), so the
     * URL is only overwritten when the provider actually changes.
     */
    private var lastProvider: String? = null

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
        keysContainer = findViewById(R.id.keys_container)
        addKeyButton = findViewById(R.id.add_key)
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

        addKeyButton.setOnClickListener {
            if (keyInputs.size >= MAX_KEYS_PER_PROVIDER) {
                toast(getString(R.string.key_box_limit, MAX_KEYS_PER_PROVIDER))
            } else {
                addKeyBox("")
            }
        }

        editingId = intent.getStringExtra(EXTRA_ID)
        editingId?.let { load(it) }
        // A new key starts with one empty box; editing shows the saved key's box.
        if (keyInputs.isEmpty()) addKeyBox("")

        findViewById<Button>(R.id.save).setOnClickListener { save() }
        findViewById<Button>(R.id.cancel).setOnClickListener { finish() }
    }

    /** Adds a framed key box. Each box has its own Remove button. */
    private fun addKeyBox(value: String) {
        val box = LayoutInflater.from(this).inflate(R.layout.item_key_input, keysContainer, false)
        val input = box.findViewById<EditText>(R.id.key_input)
        input.setText(value)
        box.findViewById<Button>(R.id.remove_key).setOnClickListener {
            keysContainer.removeView(box)
            keyInputs.remove(input)
        }
        keysContainer.addView(box)
        keyInputs.add(input)
    }

    private fun load(id: String) {
        val e = vault.find(id) ?: return
        title.setText(R.string.edit_key)
        val idx = Providers.ALL.indexOfFirst { it.name == e.provider }
        if (idx >= 0) {
            // Record the provider before selecting it so the posted callback
            // treats this as a no-op and leaves the saved Base URL intact.
            lastProvider = e.provider
            provider.setSelection(idx)
        }
        label.setText(e.label)
        url.setText(e.baseUrl)
        addKeyBox(e.apiKey)
        notes.setText(e.notes)
    }

    private fun save() {
        val providerName = provider.selectedItem?.toString().orEmpty()
        val apiKeys = keyInputs.map { it.text.toString().trim() }.filter { it.isNotBlank() }
        val baseUrl = url.text.toString().trim()

        if (providerName.isBlank() || apiKeys.isEmpty()) {
            toast(getString(R.string.missing_fields))
            return
        }
        if (baseUrl.isBlank()) {
            toast(getString(R.string.custom_needs_url))
            return
        }

        // Each provider may hold at most MAX_KEYS_PER_PROVIDER keys (the edited entry is not counted twice).
        val keysForProvider = vault.entries.count { it.provider == providerName && it.id != editingId }
        val roomLeft = (MAX_KEYS_PER_PROVIDER - keysForProvider).coerceAtLeast(0)
        if (apiKeys.size > roomLeft) {
            toast(getString(R.string.provider_key_limit, keysForProvider, roomLeft, MAX_KEYS_PER_PROVIDER))
            return
        }

        val label = label.text.toString().trim()
        val notes = notes.text.toString().trim()
        if (editingId == null) {
            apiKeys.forEach { k ->
                vault.add(Vault.Entry(Vault.newId(), providerName, label, baseUrl, k, notes))
            }
        } else {
            // Editing keeps the original entry; extra boxes become new entries.
            vault.update(Vault.Entry(editingId!!, providerName, label, baseUrl, apiKeys[0], notes))
            apiKeys.drop(1).forEach { k ->
                vault.add(Vault.Entry(Vault.newId(), providerName, label, baseUrl, k, notes))
            }
        }
        toast(getString(R.string.saved))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_ID = "entry_id"
        const val MAX_KEYS_PER_PROVIDER = 10
    }
}
