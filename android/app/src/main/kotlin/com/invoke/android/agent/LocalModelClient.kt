package com.invoke.android.agent

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class LocalModelTestResult(
    val success: Boolean,
    val message: String
)

class LocalModelClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun test(endpointInput: String, model: String): LocalModelTestResult = withContext(Dispatchers.IO) {
        val endpoint = normalizeEndpoint(endpointInput)
            ?: return@withContext LocalModelTestResult(false, "Enter an endpoint like your-computer-ip:11434")

        if (model.isBlank()) {
            return@withContext LocalModelTestResult(false, "Enter a model name")
        }

        try {
            val request = Request.Builder()
                .url("http://$endpoint/api/tags")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use LocalModelTestResult(false, "Ollama responded with ${response.code}")
                }

                val models = JsonParser.parseString(body).asJsonObject
                    .getAsJsonArray("models")
                    ?.mapNotNull { item ->
                        item.asJsonObject.get("name")?.asString
                    }
                    .orEmpty()

                if (models.any { it == model || it.startsWith("$model:") }) {
                    LocalModelTestResult(true, "Connected to $model")
                } else {
                    LocalModelTestResult(false, "Connected, but $model was not found")
                }
            }
        } catch (e: Exception) {
            LocalModelTestResult(false, "Connection failed. Check Wi-Fi and endpoint.")
        }
    }

    fun normalizeEndpoint(input: String): String? {
        val cleaned = input.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')

        if (cleaned.isBlank() || !cleaned.contains(":")) return null
        val parts = cleaned.split(":")
        if (parts.size != 2 || parts[0].isBlank() || parts[1].toIntOrNull() == null) return null
        return cleaned
    }
}
