package com.keysbook.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Model-listing client, ported from the Termux app's endpoint probing.
 *
 * The Python app hits the provider's models endpoint and only reports models the
 * key actually returns. Three response shapes are handled:
 *   - OpenAI-compatible : { "data": [ { "id": ... }, ... ] }
 *   - Ollama            : { "models": [ { "name": ... }, ... ] }
 *   - Anthropic         : { "data": [ { "id": ... }, ... ] } with x-api-key auth
 */
object ModelClient {

    data class Result(val models: List<String>, val error: String?)

    private const val TIMEOUT_MS = 20_000

    fun fetchModels(provider: String, baseUrl: String, apiKey: String): Result {
        if (baseUrl.isBlank()) {
            return Result(emptyList(), "This provider needs a custom Base URL.")
        }
        val url = when {
            Providers.isOllama(provider) -> trim(baseUrl) + "/tags"
            Providers.isAnthropic(provider) -> trim(baseUrl) + "/v1/models"
            else -> trim(baseUrl) + "/models"
        }
        return request(url, provider, apiKey)
    }

    /** Probe a single model with a tiny chat call to confirm access. */
    fun testModel(provider: String, baseUrl: String, apiKey: String, model: String): Result {
        if (baseUrl.isBlank()) {
            return Result(emptyList(), "This provider needs a custom Base URL.")
        }
        if (Providers.isOllama(provider)) {
            val url = trim(baseUrl) + "/show"
            return post(url, provider, apiKey, JSONObject().put("model", model),
                listOf("model"))
        }
        val url = trim(baseUrl) + "/chat/completions"
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 1)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", "ping")
            }))
        }
        return post(url, provider, apiKey, body, listOf("model"))
    }

    private fun request(url: String, provider: String, apiKey: String): Result = try {
        val conn = open(url, provider, apiKey, "GET")
        val code = conn.responseCode
        val text = readBody(conn, code)
        if (code !in 200..299) {
            Result(emptyList(), "HTTP $code: ${short(text)}")
        } else {
            Result(parseModels(text, provider), null)
        }
    } catch (t: Throwable) {
        Result(emptyList(), t.message ?: t.javaClass.simpleName)
    }

    private fun post(
        url: String,
        provider: String,
        apiKey: String,
        body: JSONObject,
        echoKeys: List<String>
    ): Result = try {
        val conn = open(url, provider, apiKey, "POST")
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val text = readBody(conn, code)
        if (code !in 200..299) {
            Result(emptyList(), "HTTP $code: ${short(text)}")
        } else {
            Result(listOf(echoKeys.firstOrNull() ?: "ok"), null)
        }
    } catch (t: Throwable) {
        Result(emptyList(), t.message ?: t.javaClass.simpleName)
    }

    private fun open(url: String, provider: String, apiKey: String, method: String): HttpURLConnection {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        conn.requestMethod = method
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.setRequestProperty("Accept", "application/json")
        if (apiKey.isNotBlank()) {
            if (Providers.isAnthropic(provider)) {
                conn.setRequestProperty("x-api-key", apiKey)
                conn.setRequestProperty("anthropic-version", "2023-06-01")
            } else {
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }
        return conn
    }

    private fun readBody(conn: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        if (stream == null) return ""
        return BufferedReader(stream.reader(Charsets.UTF_8)).use { it.readText() }
    }

    private fun parseModels(text: String, provider: String): List<String> {
        if (text.isBlank()) return emptyList()
        val root = JSONObject(text)
        val out = mutableListOf<String>()

        root.optJSONArray("data")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isNotBlank()) out.add(id)
            }
        }
        root.optJSONArray("models")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").ifBlank { o.optString("model") }
                if (name.isNotBlank()) out.add(name)
            }
        }
        return out.distinct().sorted()
    }

    private fun trim(baseUrl: String): String = baseUrl.trimEnd('/')

    private fun short(text: String): String =
        text.replace(Regex("\\s+"), " ").take(180)
}
