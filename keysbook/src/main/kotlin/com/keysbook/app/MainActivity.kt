package com.keysbook.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/** Unlock / create-vault screen. Equivalent to the Python app's password prompt. */
class MainActivity : Activity() {

    private lateinit var vault: Vault
    private lateinit var status: TextView
    private lateinit var password: EditText
    private lateinit var primary: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vault = AppState.get(this)
        // Already unlocked earlier in this session (e.g. returning from the list).
        if (vault.unlocked) {
            openKeys()
            return
        }

        setContentView(R.layout.activity_unlock)

        status = findViewById(R.id.status)
        password = findViewById(R.id.password)
        primary = findViewById(R.id.primary)

        primary.setOnClickListener { onPrimary() }
        findViewById<Button>(R.id.create).setOnClickListener { confirmCreate() }
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) renderIdleState()
    }

    private fun renderIdleState() {
        if (vault.exists) {
            status.text = getString(R.string.vault_exists)
        } else {
            status.text = getString(R.string.no_vault_yet)
        }
        primary.text = getString(R.string.unlock)
    }

    private fun onPrimary() {
        val pass = password.text.toString()
        if (pass.isEmpty()) {
            toast(getString(R.string.empty_password))
            return
        }
        if (vault.exists) {
            if (vault.unlock(pass)) {
                password.setText("")
                openKeys()
            } else {
                toast(getString(R.string.wrong_password))
            }
        } else {
            vault.createNew(pass)
            password.setText("")
            openKeys()
        }
    }

    private fun confirmCreate() {
        if (!vault.exists) {
            createWithTypedPassword()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.create_confirm_title)
            .setMessage(R.string.create_confirm_msg)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> createWithTypedPassword() }
            .show()
    }

    private fun createWithTypedPassword() {
        val pass = password.text.toString()
        if (pass.isEmpty()) {
            toast(getString(R.string.empty_password))
            return
        }
        vault.createNew(pass)
        password.setText("")
        openKeys()
    }

    private fun openKeys() {
        startActivity(Intent(this, KeysActivity::class.java))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
