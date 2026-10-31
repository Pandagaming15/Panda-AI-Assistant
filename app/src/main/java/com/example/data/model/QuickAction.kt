package com.example.data.model

enum class AiProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI_COMPATIBLE("OpenAI-Compatible")
}

data class QuickAction(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val promptPrefix: String,
    val description: String
)

object QuickActionsList {
    val items = listOf(
        QuickAction(
            id = "explain",
            title = "Explain Code",
            iconEmoji = "⚡",
            promptPrefix = "Explain this code in detail, highlighting key logic, Android NDK/JNI mechanisms, and potential issues:\n\n",
            description = "Deconstruct code logic, memory safety, and NDK execution"
        ),
        QuickAction(
            id = "fix_error",
            title = "Fix Compilation Error",
            iconEmoji = "🛠️",
            promptPrefix = "I encountered this compilation error in AML Compiler / AIDE. Explain why it happened and provide the fully fixed, compilable code:\n\n",
            description = "Diagnose compiler errors, missing symbols, and linker bugs"
        ),
        QuickAction(
            id = "optimize",
            title = "Optimize Code",
            iconEmoji = "🚀",
            promptPrefix = "Optimize this C++/Java code for mobile performance, reducing CPU overhead and memory allocations:\n\n",
            description = "Profile performance, cache lookups, and streamline hooks"
        ),
        QuickAction(
            id = "gen_cpp",
            title = "Generate C++ Code",
            iconEmoji = "💻",
            promptPrefix = "Write complete, practical C++ code for an Android AML / native mod that accomplishes the following:\n\n",
            description = "Create AML mod loader C++ plugins, patches, and memory hooks"
        ),
        QuickAction(
            id = "gen_java",
            title = "Generate Android Java",
            iconEmoji = "☕",
            promptPrefix = "Write clean Android Java / JNI bridge code that performs the following task:\n\n",
            description = "Develop JNI methods, Java wrappers, and reflection code"
        ),
        QuickAction(
            id = "explain_ndk",
            title = "Explain Android NDK",
            iconEmoji = "📖",
            promptPrefix = "Explain the following Android NDK / JNI concept and provide a working example:\n\n",
            description = "Learn JNIEnv*, function signatures, Android.mk, and CMake"
        ),
        QuickAction(
            id = "aml_plugin",
            title = "AML Plugin-SDK",
            iconEmoji = "🧩",
            promptPrefix = "Show how to implement this using Android Mod Loader (AML) and plugin-SDK for GTA SA Android / RenderWare:\n\n",
            description = "Hook game functions, read/write memory addresses, and use plugin-SDK"
        ),
        QuickAction(
            id = "crash_log",
            title = "Analyze Crash Log",
            iconEmoji = "💥",
            promptPrefix = "Analyze this Android crash logcat / tombstone / SIGSEGV backtrace. Pinpoint the offending instruction or null pointer and suggest a fix:\n\n",
            description = "Decode SIGSEGV, SIGABRT, tombstone traces, and register dumps"
        )
    )
}
