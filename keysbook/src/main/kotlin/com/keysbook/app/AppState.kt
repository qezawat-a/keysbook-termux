package com.keysbook.app

import android.content.Context
import java.io.File

/**
 * Holds the single unlocked vault for the process, mirroring the Python app's
 * one-vault-per-session model (~/.keysbook/vault.json).
 */
object AppState {
    private const val VAULT_NAME = "vault.json"

    @Volatile
    var vault: Vault? = null
        private set

    fun vaultFile(context: Context): File = File(context.filesDir, VAULT_NAME)

    /** Returns the existing session vault, or opens one over the on-disk file. */
    fun get(context: Context): Vault {
        val existing = vault
        if (existing != null) return existing
        val fresh = Vault(vaultFile(context.applicationContext))
        vault = fresh
        return fresh
    }

    fun lock() {
        vault?.lock()
        vault = null
    }

    /** Masks a key for display, the way the Python UI never printed keys in full. */
    fun maskKey(key: String): String {
        if (key.isBlank()) return "(empty)"
        if (key.length <= 10) return "•".repeat(key.length)
        return key.take(6) + "…" + key.takeLast(4)
    }
}
