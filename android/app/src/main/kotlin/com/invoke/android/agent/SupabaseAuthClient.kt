package com.invoke.android.agent

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class SupabaseAuthResult(
    val success: Boolean,
    val message: String,
    val accessToken: String = "",
    val email: String = ""
)

class SupabaseAuthClient {
    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun signUp(projectUrl: String, anonKey: String, email: String, password: String): SupabaseAuthResult {
        return authRequest(
            endpoint = "${projectUrl.trimEnd('/')}/auth/v1/signup",
            anonKey = anonKey,
            payload = mapOf("email" to email, "password" to password),
            successMessage = "Account created. Check email verification if enabled."
        )
    }

    suspend fun signIn(projectUrl: String, anonKey: String, email: String, password: String): SupabaseAuthResult {
        return authRequest(
            endpoint = "${projectUrl.trimEnd('/')}/auth/v1/token?grant_type=password",
            anonKey = anonKey,
            payload = mapOf("email" to email, "password" to password),
            successMessage = "Signed in"
        )
    }

    private suspend fun authRequest(
        endpoint: String,
        anonKey: String,
        payload: Map<String, String>,
        successMessage: String
    ): SupabaseAuthResult = withContext(Dispatchers.IO) {
        if (!endpoint.startsWith("https://") || anonKey.isBlank()) {
            return@withContext SupabaseAuthResult(false, "Add Supabase URL and anon key first")
        }

        try {
            val request = Request.Builder()
                .url(endpoint)
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Content-Type", "application/json")
                .post(gson.toJson(payload).toRequestBody(jsonType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use SupabaseAuthResult(false, parseError(body, response.code))
                }

                val json = JsonParser.parseString(body).asJsonObject
                val token = json.get("access_token")?.asString.orEmpty()
                val userEmail = json.getAsJsonObject("user")?.get("email")?.asString
                    ?: payload["email"].orEmpty()

                SupabaseAuthResult(
                    success = true,
                    message = successMessage,
                    accessToken = token,
                    email = userEmail
                )
            }
        } catch (e: Exception) {
            SupabaseAuthResult(false, e.message ?: "Auth request failed")
        }
    }

    private fun parseError(body: String, code: Int): String {
        return try {
            val json = JsonParser.parseString(body).asJsonObject
            json.get("msg")?.asString
                ?: json.get("message")?.asString
                ?: "Supabase error $code"
        } catch (_: Exception) {
            "Supabase error $code"
        }
    }
}
