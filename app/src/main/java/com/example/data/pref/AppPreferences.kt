package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.AiProviderType

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "panda_ai_prefs"

        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_PROVIDER_TYPE = "provider_type"
        private const val KEY_OPENAI_ENDPOINT = "openai_endpoint"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_LAST_BUBBLE_X = "last_bubble_x"
        private const val KEY_LAST_BUBBLE_Y = "last_bubble_y"
        private const val KEY_WINDOW_WIDTH = "window_width"
        private const val KEY_WINDOW_HEIGHT = "window_height"
        private const val KEY_FLOATING_OPACITY = "floating_opacity"
        private const val KEY_AUTO_MINIMIZE = "auto_minimize"
        private const val KEY_ACTIVE_CONVERSATION_ID = "active_conv_id"

        const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"
        const val DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
        const val DEFAULT_OPENAI_ENDPOINT = "https://api.openai.com/v1"

        const val DEFAULT_SYSTEM_PROMPT =
            "You are Panda AI, an expert Android native developer and systems architect specializing in C++, Android NDK, JNI, AML Mod Loader (Android Mod Loader), GTA San Andreas Android modding, plugin-SDK, ImGui, AIDE, and Android.mk/Application.mk build configurations. " +
            "Provide complete, practical, compilable code whenever possible. Respect Android compatibility (arm64-v8a / armeabi-v7a) and avoid Windows-only dependencies in Android projects. " +
            "Explain compilation errors clearly and provide corrected code with syntax formatting."
    }

    var geminiApiKey: String
        get() {
            val savedKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
            if (savedKey.isNotBlank()) return savedKey
            val buildConfigKey = try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                (field.get(null) as? String) ?: ""
            } catch (e: Throwable) {
                ""
            }
            return if (buildConfigKey == "PLACEHOLDER_KEY" || buildConfigKey == "MY_GEMINI_API_KEY") "" else buildConfigKey
        }
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() {
            val model = prefs.getString(KEY_SELECTED_MODEL, DEFAULT_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL
            val clean = model.removePrefix("models/")
            return if (clean == "gemini-2.0-flash" || clean == "gemini-2.5-flash" || clean == "gemini-1.5-flash" || clean.isBlank()) {
                prefs.edit().putString(KEY_SELECTED_MODEL, DEFAULT_GEMINI_MODEL).apply()
                DEFAULT_GEMINI_MODEL
            } else {
                clean
            }
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_MODEL, value.trim().removePrefix("models/")).apply()

    var providerType: AiProviderType
        get() {
            val name = prefs.getString(KEY_PROVIDER_TYPE, AiProviderType.GEMINI.name)
            return runCatching { AiProviderType.valueOf(name!!) }.getOrDefault(AiProviderType.GEMINI)
        }
        set(value) = prefs.edit().putString(KEY_PROVIDER_TYPE, value.name).apply()

    var openAiEndpoint: String
        get() = prefs.getString(KEY_OPENAI_ENDPOINT, DEFAULT_OPENAI_ENDPOINT) ?: DEFAULT_OPENAI_ENDPOINT
        set(value) = prefs.edit().putString(KEY_OPENAI_ENDPOINT, value.trim()).apply()

    var openAiApiKey: String
        get() = prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENAI_API_KEY, value.trim()).apply()

    var openAiModel: String
        get() = prefs.getString(KEY_OPENAI_MODEL, DEFAULT_OPENAI_MODEL) ?: DEFAULT_OPENAI_MODEL
        set(value) = prefs.edit().putString(KEY_OPENAI_MODEL, value.trim()).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, 0.4f)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    var lastBubbleX: Int
        get() = prefs.getInt(KEY_LAST_BUBBLE_X, 100)
        set(value) = prefs.edit().putInt(KEY_LAST_BUBBLE_X, value).apply()

    var lastBubbleY: Int
        get() = prefs.getInt(KEY_LAST_BUBBLE_Y, 300)
        set(value) = prefs.edit().putInt(KEY_LAST_BUBBLE_Y, value).apply()

    var windowWidth: Int
        get() = prefs.getInt(KEY_WINDOW_WIDTH, -1)
        set(value) = prefs.edit().putInt(KEY_WINDOW_WIDTH, value).apply()

    var windowHeight: Int
        get() = prefs.getInt(KEY_WINDOW_HEIGHT, -1)
        set(value) = prefs.edit().putInt(KEY_WINDOW_HEIGHT, value).apply()

    var floatingOpacity: Float
        get() = prefs.getFloat(KEY_FLOATING_OPACITY, 0.98f)
        set(value) = prefs.edit().putFloat(KEY_FLOATING_OPACITY, value).apply()

    var autoMinimizeOnOutsideTap: Boolean
        get() = prefs.getBoolean(KEY_AUTO_MINIMIZE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_MINIMIZE, value).apply()

    var activeConversationId: String?
        get() = prefs.getString(KEY_ACTIVE_CONVERSATION_ID, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_CONVERSATION_ID, value).apply()

    fun hasValidApiKey(): Boolean {
        return when (providerType) {
            AiProviderType.GEMINI -> geminiApiKey.isNotBlank()
            AiProviderType.OPENAI_COMPATIBLE -> openAiApiKey.isNotBlank()
        }
    }
}
