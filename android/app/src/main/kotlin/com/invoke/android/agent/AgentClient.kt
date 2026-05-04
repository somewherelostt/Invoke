package com.invoke.android.agent

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Data class representing a classified user intent.
 *
 * @param tool       The identified tool/action name (e.g. GMAIL_SEND_EMAIL, GITHUB_CREATE_ISSUE)
 * @param confidence Confidence score from the LLM classification (0.0 – 1.0)
 * @param params     Extracted parameters for the action
 * @param rawText    The original user input text
 * @param actionType One of "dictate" (insert text only), "action" (execute via Composio), "unknown"
 */
data class ClassifiedIntent(
    val tool: String,
    val confidence: Float,
    val params: Map<String, String>,
    val rawText: String,
    val actionType: String
)

/**
 * Result of executing (or preparing) an action.
 *
 * @param success      Whether the operation completed without errors
 * @param text         Human-readable result or error text
 * @param tool         The tool that was identified / executed
 * @param isDictation  True when the result is plain dictation text (no external action taken)
 */
data class ActionResult(
    val success: Boolean,
    val text: String,
    val tool: String,
    val isDictation: Boolean
)

// ---------------------------------------------------------------------------
// Gson helper models for serialising the Ollama request
// ---------------------------------------------------------------------------

private data class OllamaMessage(
    val role: String,
    val content: String
)

private data class OllamaOptions(
    val temperature: Double
)

private data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean,
    val options: OllamaOptions
)

private data class ComposioExecuteRequest(
    val arguments: Map<String, String>
)

// ---------------------------------------------------------------------------
// AgentClient
// ---------------------------------------------------------------------------

class AgentClient {

    companion object {
        private const val TAG = "INVOKE"

        private const val OLLAMA_ENDPOINT_DEFAULT = ""
        private const val OLLAMA_MODEL_DEFAULT = "qwen3:0.6b"

        private const val PREF_KEY_ENDPOINT = "ollama_endpoint"
        private const val PREF_KEY_MODEL = "ollama_model"
        private const val PREF_KEY_COMPOSIO = "composio_api_key"

        private const val COMPOSIO_BASE_URL = "https://backend.composio.dev/api/v3.1"

        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 120L    // Local LLM responses can be slow
        private const val WRITE_TIMEOUT_SECONDS = 30L

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        // -------------------------------------------------------------------
        // System prompt that instructs the local model to classify user text
        // into a structured JSON response.
        // -------------------------------------------------------------------
        private const val SYSTEM_PROMPT = """You are INVOKE, an intent-classification agent running on-device.
Your job is to analyse the user's free-form text and decide which tool action best matches their request.

Respond ONLY with valid JSON — no markdown, no explanation, no extra text. The JSON schema is:

{
  "tool": "<TOOL_NAME>",
  "confidence": <0.0-1.0>,
  "params": { "key": "value" },
  "actionType": "action"
}

Available tools (use EXACTLY these names):
- GMAIL_SEND_EMAIL        → params: recipient_email, subject, body
- GMAIL_READ_EMAILS       → params: query, maxResults
- GITHUB_CREATE_AN_ISSUE  → params: repo_name, title, body
- GITHUB_LIST_ISSUES      → params: owner, repo, state
- GITHUB_CREATE_PR        → params: owner, repo, title, head, base, body
- SLACK_SEND_MESSAGE      → params: channel, text
- SLACK_READ_MESSAGES     → params: channel, limit
- NOTION_CREATE_PAGE      → params: databaseId, title, content
- NOTION_SEARCH           → params: query
- CALENDAR_CREATE_EVENT   → params: summary, start, end
- CALENDAR_LIST_EVENTS    → params: timeMin, timeMax, maxResults
- TODOIST_CREATE_TASK     → params: content, dueDate, priority
- TODOIST_LIST_TASKS      → params: filter
- COMPOSIO_SEARCH_WEB     → params: query

If the user's text does NOT clearly map to any of these tools (e.g. it is just plain speech or dictation),
respond with:

{
  "tool": "DICTATE",
  "confidence": 1.0,
  "params": {},
  "actionType": "dictate"
}

If you are completely unsure, respond with:

{
  "tool": "UNKNOWN",
  "confidence": 0.0,
  "params": {},
  "actionType": "unknown"
}

Important rules:
- Set "actionType" to "action" for any real tool above, "dictate" for DICTATE, "unknown" for UNKNOWN.
- Extract as many relevant params as you can derive from the text.
- Be generous with confidence when the intent is clear; use low confidence only when genuinely ambiguous."""
    }

    // Single Gson instance, reused across calls
    private val gson = Gson()

    // OkHttp client configured with generous timeouts for local LLM responses
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    // -----------------------------------------------------------------------
    // 1. Classify Intent via Ollama
    // -----------------------------------------------------------------------

    /**
     * Sends [text] to a local Ollama instance and returns a [ClassifiedIntent].
     *
     * On any failure (network, parse, timeout) the method falls back to a
     * "dictate" intent so the user's words are never lost.
     */
    suspend fun classifyIntent(
        text: String,
        endpoint: String,
        model: String
    ): ClassifiedIntent = withContext(Dispatchers.IO) {
        try {
            val requestPayload = OllamaChatRequest(
                model = model,
                messages = listOf(
                    OllamaMessage(role = "system", content = SYSTEM_PROMPT),
                    OllamaMessage(role = "user", content = text)
                ),
                stream = false,
                options = OllamaOptions(temperature = 0.1)
            )

            val jsonBody = gson.toJson(requestPayload)
            Log.d(TAG, "Ollama request body: $jsonBody")

            val url = "http://$endpoint/api/chat"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "no body"
                    Log.e(TAG, "Ollama HTTP ${response.code}: $errorBody")
                    return@use dictateFallback(text)
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    Log.e(TAG, "Ollama returned empty body")
                    return@use dictateFallback(text)
                }

                Log.d(TAG, "Ollama raw response: $responseBody")
                parseClassifiedIntent(responseBody, text)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during classifyIntent", e)
            dictateFallback(text)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during classifyIntent", e)
            dictateFallback(text)
        }
    }

    // -----------------------------------------------------------------------
    // 2. Execute Action via Composio (or return dictation)
    // -----------------------------------------------------------------------

    /**
     * Executes a [ClassifiedIntent].
     *
     * - For "dictate" intents the raw text is returned immediately.
     * - For "action" intents a REST call is made to the Composio backend.
     * - For "unknown" intents an error result is returned.
     */
    suspend fun executeAction(
        intent: ClassifiedIntent,
        composioKey: String
    ): ActionResult = withContext(Dispatchers.IO) {
        when (intent.actionType) {
            "dictate" -> {
                Log.d(TAG, "Dictation intent – returning raw text")
                ActionResult(
                    success = true,
                    text = intent.rawText,
                    tool = intent.tool,
                    isDictation = true
                )
            }

            "action" -> {
                Log.d(TAG, "Executing action [${intent.tool}] via Composio")
                try {
                    val payload = ComposioExecuteRequest(arguments = intent.params)
                    val jsonBody = gson.toJson(payload)
                    Log.d(TAG, "Composio request: $jsonBody")

                    val request = Request.Builder()
                        .url("$COMPOSIO_BASE_URL/tools/execute/${intent.tool}")
                        .header("x-api-key", composioKey)
                        .header("Content-Type", "application/json")
                        .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: "{}"
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Composio HTTP ${response.code}: $body")
                            ActionResult(
                                success = false,
                                text = "Composio error ${response.code}: ${truncate(body, 500)}",
                                tool = intent.tool,
                                isDictation = false
                            )
                        } else {
                            Log.d(TAG, "Composio success: ${truncate(body, 500)}")
                            val resultText = extractComposioResultText(body)
                            ActionResult(
                                success = true,
                                text = resultText,
                                tool = intent.tool,
                                isDictation = intent.tool == "COMPOSIO_SEARCH_WEB"
                            )
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Network error executing action", e)
                    ActionResult(
                        success = false,
                        text = "Network error: ${e.message}",
                        tool = intent.tool,
                        isDictation = false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error executing action", e)
                    ActionResult(
                        success = false,
                        text = "Error: ${e.message}",
                        tool = intent.tool,
                        isDictation = false
                    )
                }
            }

            else -> {
                Log.w(TAG, "Unknown action type: ${intent.actionType}")
                ActionResult(
                    success = false,
                    text = "Unrecognised action type: ${intent.actionType}",
                    tool = intent.tool,
                    isDictation = false
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // 3. End-to-end: classify + optionally execute
    // -----------------------------------------------------------------------

    /**
     * Convenience method that reads configuration from [prefs],
     * classifies the user's [text], and — when a Composio API key is
     * configured — executes the resulting action.
     *
     * If no Composio key is stored the method stops at classification
     * and returns a dictation-style result.
     */
    suspend fun classifyAndExecute(
        text: String,
        prefs: SharedPreferences
    ): ActionResult = withContext(Dispatchers.IO) {
        val endpoint = prefs.getString(PREF_KEY_ENDPOINT, OLLAMA_ENDPOINT_DEFAULT)
            ?: OLLAMA_ENDPOINT_DEFAULT
        val model = prefs.getString(PREF_KEY_MODEL, OLLAMA_MODEL_DEFAULT)
            ?: OLLAMA_MODEL_DEFAULT
        val composioKey = prefs.getString(PREF_KEY_COMPOSIO, "").orEmpty()

        Log.d(TAG, "classifyAndExecute → endpoint=$endpoint, model=$model, " +
                "composioKey=${if (composioKey.isNotBlank()) "***" else "(empty)"}")

        // Step 1 – classify. Obvious command forms are routed deterministically so
        // tiny local models cannot miss common actions such as web search.
        val intent = classifyRuleBased(text) ?: classifyIntent(text, endpoint, model)
        Log.d(TAG, "Classified → tool=${intent.tool}, confidence=${intent.confidence}, " +
                "actionType=${intent.actionType}, params=${intent.params}")

        // Step 2 – execute (only if Composio key is available and action is actionable)
        if (composioKey.isBlank()) {
            Log.d(TAG, "No Composio key – returning dictation result")
            ActionResult(
                success = true,
                text = intent.rawText,
                tool = intent.tool,
                isDictation = true
            )
        } else {
            executeAction(intent, composioKey)
        }
    }

    // ===================================================================
    // Internal helpers
    // ===================================================================

    /**
     * Attempts to parse the Ollama chat-completion response JSON and
     * extract a [ClassifiedIntent].
     *
     * The Ollama `/api/chat` response shape (non-streaming) is:
     * ```
     * {
     *   "message": { "role": "assistant", "content": "{ ... }" },
     *   "model": "...",
     *   ...
     * }
     * ```
     * The `content` field contains the JSON the model generated.
     */
    private fun parseClassifiedIntent(responseBody: String, rawText: String): ClassifiedIntent {
        try {
            val root = JsonParser.parseString(responseBody).asJsonObject

            // Extract the assistant message content
            val content = if (root.has("message") && root["message"].isJsonObject) {
                root["message"].asJsonObject.get("content")?.asString ?: ""
            } else if (root.has("content")) {
                root["content"].asString
            } else {
                responseBody
            }

            // Strip any markdown code fences the model may have added
            val cleanedContent = stripMarkdownFence(content)

            // Parse the inner JSON
            val classified = JsonParser.parseString(cleanedContent).asJsonObject

            val tool = classified.get("tool")?.asString ?: "DICTATE"
            val confidence = classified.get("confidence")?.asFloat ?: 0.5f
            val actionType = classified.get("actionType")?.asString ?: "dictate"

            val params = mutableMapOf<String, String>()
            if (classified.has("params") && classified["params"].isJsonObject) {
                val paramsObj = classified["params"].asJsonObject
                for ((key, value) in paramsObj.entrySet()) {
                    params[key] = value.asString
                }
            }

            return ClassifiedIntent(
                tool = tool,
                confidence = confidence,
                params = params,
                rawText = rawText,
                actionType = actionType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Ollama classification response", e)
            return dictateFallback(rawText)
        }
    }

    private fun classifyRuleBased(rawText: String): ClassifiedIntent? {
        val normalized = rawText.lowercase().trim()
        val wantsWebSearch = listOf(
            "search the web",
            "web search",
            "search online",
            "look up",
            "google",
            "find online",
            "latest "
        ).any { normalized.contains(it) }

        if (wantsWebSearch) {
            val query = rawText
                .replace(Regex("(?i)^search the web for\\s+"), "")
                .replace(Regex("(?i)^web search\\s+"), "")
                .replace(Regex("(?i)^search online for\\s+"), "")
                .replace(Regex("(?i)^look up\\s+"), "")
                .replace(Regex("(?i)^google\\s+"), "")
                .replace(Regex("(?i)^find online\\s+"), "")
                .trim()

            return ClassifiedIntent(
                tool = "COMPOSIO_SEARCH_WEB",
                confidence = 1.0f,
                params = mapOf("query" to query.ifBlank { rawText }),
                rawText = rawText,
                actionType = "action"
            )
        }

        return null
    }

    /**
     * Returns a fallback "dictate" intent so the user's text is never lost.
     */
    private fun dictateFallback(rawText: String): ClassifiedIntent {
        return ClassifiedIntent(
            tool = "DICTATE",
            confidence = 1.0f,
            params = emptyMap(),
            rawText = rawText,
            actionType = "dictate"
        )
    }

    /**
     * Removes surrounding ```json ... ``` or ``` ... ``` fences that some
     * models tend to wrap around structured output.
     */
    private fun stripMarkdownFence(text: String): String {
        var s = text.trim()
        if (s.startsWith("```json", ignoreCase = true)) {
            s = s.removePrefix("```json").trimStart()
        } else if (s.startsWith("```")) {
            s = s.removePrefix("```").trimStart()
        }
        if (s.endsWith("```")) {
            s = s.removeSuffix("```").trimEnd()
        }
        return s.trim()
    }

    /**
     * Truncates a string to [maxLen] characters for safe logging.
     */
    private fun truncate(text: String, maxLen: Int): String {
        return if (text.length <= maxLen) text else text.substring(0, maxLen) + "…"
    }

    private fun extractComposioResultText(body: String): String {
        return try {
            val root = JsonParser.parseString(body).asJsonObject
            val data = root.get("data")
            if (data != null && data.isJsonObject) {
                val obj = data.asJsonObject
                obj.get("answer")?.asString
                    ?: obj.get("result")?.asString
                    ?: obj.get("text")?.asString
                    ?: truncate(obj.toString(), 1000)
            } else {
                truncate(body, 1000)
            }
        } catch (_: Exception) {
            truncate(body, 1000)
        }
    }
}
