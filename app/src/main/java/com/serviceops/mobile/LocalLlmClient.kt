package com.serviceops.mobile

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class LlmDraft(
    val reply: String,
    val title: String,
    val details: String
)

object LocalLlmClient {
    suspend fun createLocationDraft(
        baseUrl: String,
        model: String,
        location: Location?,
        recentLogs: List<ServiceLog>,
        recentChat: List<ChatMessage>,
        userText: String,
        photoPaths: List<String>
    ): LlmDraft = withContext(Dispatchers.IO) {
        val endpoint = if (baseUrl.trimEnd('/').endsWith("/chat/completions")) {
            baseUrl.trimEnd('/')
        } else {
            "${baseUrl.trimEnd('/')}/chat/completions"
        }

        val context = buildString {
            appendLine("Selected location:")
            if (location == null) {
                appendLine("General business history (no location selected)")
            } else {
                appendLine("Name: ${location.name}")
                appendLine("Customer: ${location.customer}")
                appendLine("Address: ${location.address}")
                appendLine("Service type: ${location.serviceType}")
            }
            appendLine()
            appendLine("Recent history:")
            if (recentLogs.isEmpty()) appendLine("No prior entries.")
            recentLogs.take(10).forEach { log -> appendLine("- ${log.title}: ${log.details}") }
            appendLine()
            appendLine("Current field update:")
            append(userText)
        }

        val system = """
            You are the private field assistant for Service Ops LLC. Turn the owner's field update into a concise, factual service-history draft. Never invent readings, parts, dates, prices, work, or observations. If something is uncertain, say so in the reply and leave it out of the factual draft. Photos may be attached; only describe visible details when image input is actually available to you. Return only valid JSON with exactly these string fields: "reply", "title", and "details". The reply should briefly summarize what you prepared. The title should be a short work-performed heading. The details should preserve useful condition, work, parts, measurements, photos, and follow-up information.
        """.trimIndent()

        val messages = JSONArray().put(JSONObject().apply {
            put("role", "system")
            put("content", system)
        })
        recentChat.takeLast(8).forEach { message ->
            if (message.text.isNotBlank()) messages.put(JSONObject().apply {
                put("role", if (message.role == "assistant") "assistant" else "user")
                put("content", message.text)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            if (photoPaths.isEmpty()) {
                put("content", context)
            } else {
                put("content", JSONArray().apply {
                    put(JSONObject().apply { put("type", "text"); put("text", context) })
                    photoPaths.forEach { path ->
                        val file = File(path)
                        if (file.exists()) put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().put("url", "data:${mimeType(file)};base64,${Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)}"))
                        })
                    }
                })
            }
        })

        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.2)
            put("max_tokens", 350)
            put("response_format", JSONObject().put("type", "json_object"))
            put("chat_template_kwargs", JSONObject().put("enable_thinking", false))
            put("messages", messages)
        }.toString()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.bufferedWriter().use { it.write(body) }
            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("Local model returned HTTP ${connection.responseCode}: ${error.take(300)}")
            }
            val content = JSONObject(responseText)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            val jsonText = content.substring(content.indexOf('{'), content.lastIndexOf('}') + 1)
            val draft = JSONObject(jsonText)
            LlmDraft(
                reply = draft.optString("reply").ifBlank { "I prepared a location-history draft." },
                title = draft.optString("title").ifBlank { "Service update" },
                details = draft.optString("details").ifBlank { userText.trim() }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun mimeType(file: File): String = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
}
