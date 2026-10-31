package com.example.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiApiClient(
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String
) : AiService {

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun streamResponse(
        messages: List<MessagePayload>,
        systemInstruction: String,
        temperature: Float
    ): Flow<String> = callbackFlow {
        val apiKey = apiKeyProvider().trim()
        val model = modelProvider().trim().ifBlank { "gemini-2.5-flash" }

        if (apiKey.isBlank()) {
            close(IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings."))
            return@callbackFlow
        }

        val requestUrl = "$BASE_URL/$model:streamGenerateContent?key=$apiKey&alt=sse"

        val jsonBody = buildRequestBody(messages, systemInstruction, temperature)
        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(requestUrl)
            .post(body)
            .addHeader("Accept", "text/event-stream")
            .build()

        val call = client.newCall(request)

        val thread = Thread {
            var response: Response? = null
            try {
                response = call.execute()
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "HTTP ${response.code}"
                    val cleanMsg = parseErrorMessage(errBody)
                    close(IOException("Gemini API Error (${response.code}): $cleanMsg"))
                    return@Thread
                }

                val source = response.body?.source()
                if (source == null) {
                    close(IOException("Response body is empty"))
                    return@Thread
                }

                val reader = BufferedReader(source.inputStream().reader())
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: break
                    if (currentLine.startsWith("data: ")) {
                        val dataJson = currentLine.removePrefix("data: ").trim()
                        if (dataJson.isNotEmpty() && dataJson != "[DONE]") {
                            val textChunk = extractTextFromGeminiJson(dataJson)
                            if (textChunk.isNotEmpty()) {
                                trySend(textChunk)
                            }
                        }
                    }
                }
                close()
            } catch (e: Exception) {
                if (!call.isCanceled()) {
                    Log.e(TAG, "Streaming error: ${e.message}", e)
                    close(e)
                }
            } finally {
                response?.close()
            }
        }

        thread.start()

        awaitClose {
            call.cancel()
            thread.interrupt()
        }
    }

    override suspend fun generateResponse(
        messages: List<MessagePayload>,
        systemInstruction: String,
        temperature: Float
    ): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        val model = modelProvider().trim().ifBlank { "gemini-2.5-flash" }

        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings.")
        }

        val requestUrl = "$BASE_URL/$model:generateContent?key=$apiKey"
        val jsonBody = buildRequestBody(messages, systemInstruction, temperature)
        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(requestUrl)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val cleanMsg = parseErrorMessage(responseString)
                throw IOException("Gemini API Error (${response.code}): $cleanMsg")
            }
            extractTextFromGeminiJson(responseString)
        }
    }

    private fun buildRequestBody(
        messages: List<MessagePayload>,
        systemInstruction: String,
        temperature: Float
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        if (systemInstruction.isNotBlank()) {
            val sysPart = JSONObject().put("text", systemInstruction)
            val sysPartsArray = JSONArray().put(sysPart)
            val sysObj = JSONObject().put("parts", sysPartsArray)
            root.put("systemInstruction", sysObj)
        }

        // Contents
        val contentsArray = JSONArray()
        for (msg in messages) {
            val contentObj = JSONObject()
            // Map roles: user -> user, assistant/model -> model
            val role = if (msg.role.equals("model", ignoreCase = true) || msg.role.equals("assistant", ignoreCase = true)) {
                "model"
            } else {
                "user"
            }
            contentObj.put("role", role)
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", msg.content))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }
        root.put("contents", contentsArray)

        // Generation Config
        val config = JSONObject()
        config.put("temperature", temperature.toDouble())
        config.put("maxOutputTokens", 8192)
        root.put("generationConfig", config)

        return root
    }

    private fun extractTextFromGeminiJson(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            val candidates = obj.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                sb.append(part.optString("text", ""))
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseErrorMessage(errorJson: String): String {
        return try {
            val obj = JSONObject(errorJson)
            val error = obj.optJSONObject("error")
            if (error != null) {
                error.optString("message", errorJson)
            } else {
                errorJson
            }
        } catch (e: Exception) {
            errorJson
        }
    }
}
