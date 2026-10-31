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

class OpenAiApiClient(
    private val endpointProvider: () -> String,
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String
) : AiService {

    companion object {
        private const val TAG = "OpenAiApiClient"
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
        val rawEndpoint = endpointProvider().trim().removeSuffix("/")
        val apiKey = apiKeyProvider().trim()
        val model = modelProvider().trim().ifBlank { "gpt-4o-mini" }

        val requestUrl = if (rawEndpoint.endsWith("/chat/completions")) {
            rawEndpoint
        } else {
            "$rawEndpoint/chat/completions"
        }

        val jsonBody = buildRequestBody(messages, systemInstruction, temperature, model, stream = true)
        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val reqBuilder = Request.Builder()
            .url(requestUrl)
            .post(body)
            .addHeader("Accept", "text/event-stream")

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        val call = client.newCall(reqBuilder.build())

        val thread = Thread {
            var response: Response? = null
            try {
                response = call.execute()
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "HTTP ${response.code}"
                    close(IOException("OpenAI-Compatible API Error (${response.code}): $errBody"))
                    return@Thread
                }

                val source = response.body?.source()
                if (source == null) {
                    close(IOException("Empty response body"))
                    return@Thread
                }

                val reader = BufferedReader(source.inputStream().reader())
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: break
                    if (currentLine.startsWith("data: ")) {
                        val dataJson = currentLine.removePrefix("data: ").trim()
                        if (dataJson == "[DONE]") {
                            break
                        }
                        if (dataJson.isNotEmpty()) {
                            val textChunk = extractDeltaText(dataJson)
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
        val rawEndpoint = endpointProvider().trim().removeSuffix("/")
        val apiKey = apiKeyProvider().trim()
        val model = modelProvider().trim().ifBlank { "gpt-4o-mini" }

        val requestUrl = if (rawEndpoint.endsWith("/chat/completions")) {
            rawEndpoint
        } else {
            "$rawEndpoint/chat/completions"
        }

        val jsonBody = buildRequestBody(messages, systemInstruction, temperature, model, stream = false)
        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val reqBuilder = Request.Builder()
            .url(requestUrl)
            .post(body)

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IOException("OpenAI API Error (${response.code}): $responseString")
            }
            extractContentFromResponse(responseString)
        }
    }

    private fun buildRequestBody(
        messages: List<MessagePayload>,
        systemInstruction: String,
        temperature: Float,
        model: String,
        stream: Boolean
    ): JSONObject {
        val root = JSONObject()
        root.put("model", model)
        root.put("stream", stream)
        root.put("temperature", temperature.toDouble())

        val msgsArray = JSONArray()
        if (systemInstruction.isNotBlank()) {
            msgsArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }
        for (msg in messages) {
            msgsArray.put(JSONObject().apply {
                val role = if (msg.role.equals("model", ignoreCase = true)) "assistant" else msg.role
                put("role", role)
                put("content", msg.content)
            })
        }
        root.put("messages", msgsArray)
        return root
    }

    private fun extractDeltaText(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            val choices = obj.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val firstChoice = choices.getJSONObject(0)
            val delta = firstChoice.optJSONObject("delta") ?: return ""
            delta.optString("content", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractContentFromResponse(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            val choices = obj.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val message = choices.getJSONObject(0).optJSONObject("message") ?: return ""
            message.optString("content", "")
        } catch (e: Exception) {
            ""
        }
    }
}
