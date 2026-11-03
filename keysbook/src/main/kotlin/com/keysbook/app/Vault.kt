package com.keysbook.app

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Model-key vault, ported from the Termux app's `vault.json` handling.
 *
 * The Python original shelled out to `openssl enc -aes-256-cbc -pbkdf2 -salt -a`
 * and wrote the base64 blob into ~/.keysbook/vault.json. This port performs the
 * identical scheme in-process so the two formats stay interchangeable:
 *
 *   base64( "Salted__" || salt[8] || AES-256-CBC( PBKDF2-HMAC-SHA256(pass, salt) ) )
 */
class Vault(private val vaultFile: File) {

    data class Entry(
        val id: String,
        val provider: String,
        val label: String,
        val baseUrl: String,
        val apiKey: String,
        val notes: String
    )

    var entries: MutableList<Entry> = mutableListOf()
        private set

    /** True once a vault file exists and has been unlocked with the right password. */
    var unlocked: Boolean = false
        private set

    private var password: String = ""

    val exists: Boolean get() = vaultFile.exists()

    /** Decrypts the vault with [pass]. Returns false when the password is wrong. */
    fun unlock(pass: String): Boolean {
        if (!vaultFile.exists()) {
            password = pass
            entries = mutableListOf()
            unlocked = true
            persist()
            return true
        }
        return try {
            val json = JSONObject(decrypt(vaultFile.readText().trim(), pass))
            entries = parseEntries(json)
            password = pass
            unlocked = true
            true
        } catch (t: Throwable) {
            unlocked = false
            false
        }
    }

    /** Creates a fresh vault, overwriting anything on disk. */
    fun createNew(pass: String) {
        password = pass
        entries = mutableListOf()
        unlocked = true
        persist()
    }

    fun lock() {
        password = ""
        entries = mutableListOf()
        unlocked = false
    }

    fun add(entry: Entry) {
        check(unlocked) { "Vault is locked" }
        entries.add(entry)
        persist()
    }

    fun update(entry: Entry) {
        check(unlocked) { "Vault is locked" }
        val i = entries.indexOfFirst { it.id == entry.id }
        if (i >= 0) entries[i] = entry else entries.add(entry)
        persist()
    }

    fun remove(id: String) {
        check(unlocked) { "Vault is locked" }
        entries.removeAll { it.id == id }
        persist()
    }

    fun find(id: String): Entry? = entries.firstOrNull { it.id == id }

    private fun persist() {
        if (!unlocked) return
        vaultFile.parentFile?.mkdirs()
        vaultFile.writeText(encrypt(toJson().toString(), password))
    }

    private fun toJson(): JSONObject {
        val arr = JSONArray()
        for (e in entries) {
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("provider", e.provider)
                put("label", e.label)
                put("base_url", e.baseUrl)
                put("api_key", e.apiKey)
                put("notes", e.notes)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("keys", arr)
        }
    }

    private fun parseEntries(json: JSONObject): MutableList<Entry> {
        val out = mutableListOf<Entry>()
        val arr = json.optJSONArray("keys") ?: return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                Entry(
                    id = o.optString("id"),
                    provider = o.optString("provider"),
                    label = o.optString("label"),
                    baseUrl = o.optString("base_url"),
                    apiKey = o.optString("api_key"),
                    notes = o.optString("notes")
                )
            )
        }
        return out
    }

    companion object {
        private const val MAGIC = "Salted__"
        private const val ITERATIONS = 10_000
        private const val KEY_BITS = 256

        fun newId(): String = java.util.UUID.randomUUID().toString()

        /** OpenSSL-compatible AES-256-CBC + PBKDF2 encrypt, base64 encoded. */
        fun encrypt(plain: String, pass: String): String {
            val salt = ByteArray(8).also { SecureRandom().nextBytes(it) }
            val (key, iv) = derive(pass, salt)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val out = MAGIC.toByteArray(Charsets.US_ASCII) + salt + ct
            return Base64.encodeToString(out, Base64.NO_WRAP)
        }

        /** Inverse of [encrypt]; throws IllegalArgumentException on a bad password. */
        fun decrypt(encoded: String, pass: String): String {
            val raw = Base64.decode(encoded, Base64.DEFAULT)
            require(raw.size > 16 && String(raw, 0, 8, Charsets.US_ASCII) == MAGIC) {
                "Not an OpenSSL salted payload"
            }
            val salt = raw.copyOfRange(8, 16)
            val ct = raw.copyOfRange(16, raw.size)
            val (key, iv) = derive(pass, salt)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            return String(cipher.doFinal(ct), Charsets.UTF_8)
        }

        /** OpenSSL -pbkdf2 derives a 48-byte block: 32-byte key then 16-byte IV. */
        private fun derive(pass: String, salt: ByteArray): Pair<ByteArray, ByteArray> {
            val spec = PBEKeySpec(pass.toCharArray(), salt, ITERATIONS, KEY_BITS + 128)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val block = factory.generateSecret(spec).encoded
            return block.copyOfRange(0, 32) to block.copyOfRange(32, 48)
        }
    }
}
