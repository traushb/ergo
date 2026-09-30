package app.ergo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

data class KeyInfo(val label: String?, val usage: Double?, val limit: Double?, val limitRemaining: Double?)

/** [promptPrice]/[completionPrice] are USD per token; negative means variable (the Auto router). */
data class ModelInfo(val id: String, val name: String, val ctx: Int?, val promptPrice: Double, val completionPrice: Double)

data class ChatMessage(val role: String, val content: String)

data class ChatResult(val text: String, val model: String?, val cost: Double?)

const val AUTO_MODEL = "openrouter/auto"

val FALLBACK_MODELS = listOf(
    ModelInfo(AUTO_MODEL, "Автовыбор", null, -1.0, -1.0),
    ModelInfo("anthropic/claude-sonnet-4.5", "Anthropic: Claude Sonnet 4.5", 200000, 3e-6, 15e-6),
    ModelInfo("google/gemini-2.5-flash", "Google: Gemini 2.5 Flash", 1048576, 3e-7, 25e-7),
    ModelInfo("openai/gpt-4o-mini", "OpenAI: GPT-4o-mini", 128000, 15e-8, 6e-7),
    ModelInfo("meta-llama/llama-3.3-70b-instruct", "Meta: Llama 3.3 70B Instruct", 131072, 1e-7, 3e-7),
)

class OpenRouterException(message: String) : IOException(message)

/** Talks to OpenRouter directly from the device; there is no Ergo server in between. */
object OpenRouter {
    private const val BASE = "https://openrouter.ai/api/v1"

    suspend fun keyInfo(key: String): KeyInfo {
        val data = request("GET", "/key", key, null).optJSONObject("data") ?: JSONObject()
        return KeyInfo(
            label = data.optStringOrNull("label"),
            usage = data.optDoubleOrNull("usage"),
            limit = data.optDoubleOrNull("limit"),
            limitRemaining = data.optDoubleOrNull("limit_remaining"),
        )
    }

    suspend fun models(): List<ModelInfo> {
        val arr = request("GET", "/models", null, null).optJSONArray("data") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            val m = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = m.optString("id").ifEmpty { return@mapNotNull null }
            val pricing = m.optJSONObject("pricing")
            ModelInfo(
                id = id,
                name = m.optString("name").ifEmpty { id },
                ctx = if (m.has("context_length") && !m.isNull("context_length")) m.optInt("context_length") else null,
                promptPrice = pricing?.optString("prompt")?.toDoubleOrNull() ?: 0.0,
                completionPrice = pricing?.optString("completion")?.toDoubleOrNull() ?: 0.0,
            )
        }
    }

    suspend fun chat(key: String, model: String, messages: List<ChatMessage>, maxTokens: Int): ChatResult {
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray(messages.map { JSONObject().put("role", it.role).put("content", it.content) }))
            .put("max_tokens", maxTokens)
            .put("temperature", 0.8)
            .put("usage", JSONObject().put("include", true))
        val j = request("POST", "/chat/completions", key, body)
        val text = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
        val cost = j.optJSONObject("usage")?.optDoubleOrNull("cost")
        return ChatResult(text, j.optStringOrNull("model"), cost)
    }

    private suspend fun request(method: String, path: String, key: String?, body: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = URI(BASE + path).toURL().openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 15_000
                conn.readTimeout = 90_000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("X-Title", "Ergo")
                if (key != null) conn.setRequestProperty("Authorization", "Bearer $key")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val code = conn.responseCode
                val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val j = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
                val err = j.optJSONObject("error")
                if (code !in 200..299 || err != null) {
                    throw OpenRouterException(err?.optStringOrNull("message") ?: "HTTP $code")
                }
                j
            } finally {
                conn.disconnect()
            }
        }
}

private fun JSONObject.optStringOrNull(k: String): String? = if (has(k) && !isNull(k)) optString(k) else null

private fun JSONObject.optDoubleOrNull(k: String): Double? =
    if (has(k) && !isNull(k)) optDouble(k).takeUnless { it.isNaN() } else null

/** Models sometimes wrap JSON in prose or code fences; take the outermost object. */
fun parseModelJson(t: String): JSONObject {
    val cleaned = t.replace("```json", "").replace("```", "")
    val m = Regex("\\{[\\s\\S]*\\}").find(cleaned)
    return JSONObject(m?.value ?: cleaned)
}

fun JSONObject.stringList(k: String): List<String> {
    val a = optJSONArray(k) ?: return emptyList()
    return (0 until a.length()).mapNotNull { a.opt(it)?.takeUnless { v -> v == JSONObject.NULL }?.toString() }
}
