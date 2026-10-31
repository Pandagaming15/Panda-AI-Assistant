package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ChatRepository
import com.example.ui.components.CodeBlockView
import com.example.ui.theme.CodeEditorBg
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.PandaBgCard
import com.example.ui.theme.PandaBgCardElevated
import com.example.ui.theme.PandaBgDeep
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.PandaGreenAccent
import com.example.ui.theme.PandaPurpleLight
import com.example.ui.theme.PandaPurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class CodeSnippet(
    val title: String,
    val description: String,
    val language: String,
    val code: String,
    val aiPrompt: String
)

@Composable
fun AmlToolkitScreen(
    repository: ChatRepository,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var crashInput by remember { mutableStateOf("") }

    val tabs = listOf("AML Plugins", "Memory Hooks", "ImGui Setup", "Build (mk)", "Crash Doctor")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PandaBgDeep)
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PandaBgCard)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = PandaCyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AML Compiler & NDK Toolkit",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Ready-to-compile boilerplate, memory hooks, and crash diagnosis",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = PandaBgCardElevated,
            contentColor = PandaCyanAccent,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PandaCyanAccent else TextSecondary
                            )
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            when (selectedTab) {
                0 -> SnippetList(snippets = amlPluginSnippets, onSendToAi = { prompt ->
                    coroutineScope.launch {
                        repository.getOrCreateActiveConversation()
                        repository.sendMessage(prompt)
                        onNavigateToChat()
                    }
                })

                1 -> SnippetList(snippets = hookSnippets, onSendToAi = { prompt ->
                    coroutineScope.launch {
                        repository.getOrCreateActiveConversation()
                        repository.sendMessage(prompt)
                        onNavigateToChat()
                    }
                })

                2 -> SnippetList(snippets = imguiSnippets, onSendToAi = { prompt ->
                    coroutineScope.launch {
                        repository.getOrCreateActiveConversation()
                        repository.sendMessage(prompt)
                        onNavigateToChat()
                    }
                })

                3 -> SnippetList(snippets = buildConfigSnippets, onSendToAi = { prompt ->
                    coroutineScope.launch {
                        repository.getOrCreateActiveConversation()
                        repository.sendMessage(prompt)
                        onNavigateToChat()
                    }
                })

                4 -> CrashDoctorTab(
                    crashInput = crashInput,
                    onCrashInputChange = { crashInput = it },
                    onAnalyze = {
                        if (crashInput.isNotBlank()) {
                            val prompt = "Analyze this Android native crash / logcat backtrace. Explain what caused the SIGSEGV/fault, which address or register failed, and provide fixed C++ code:\n\n```logcat\n$crashInput\n```"
                            coroutineScope.launch {
                                repository.getOrCreateActiveConversation()
                                repository.sendMessage(prompt)
                                onNavigateToChat()
                            }
                        } else {
                            Toast.makeText(context, "Please paste a crash log or tombstone trace first", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SnippetList(
    snippets: List<CodeSnippet>,
    onSendToAi: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(snippets) { snippet ->
            SnippetCard(snippet = snippet, onSendToAi = { onSendToAi(snippet.aiPrompt) })
        }
    }
}

@Composable
private fun SnippetCard(
    snippet: CodeSnippet,
    onSendToAi: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PandaBgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = snippet.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = snippet.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Button(
                    onClick = onSendToAi,
                    colors = ButtonDefaults.buttonColors(containerColor = PandaPurplePrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Customize", fontSize = 11.sp)
                }
            }

            CodeBlockView(
                code = snippet.code,
                language = snippet.language
            )
        }
    }
}

@Composable
private fun CrashDoctorTab(
    crashInput: String,
    onCrashInputChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PandaBgCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = PandaCyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Native Crash Doctor (Logcat / Tombstone)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Text(
                    text = "Paste your SIGSEGV, SIGABRT, tombstone, or compiler build failure log. Panda AI will inspect the stack registers (PC, LR, SP) and pinpoint the crash line.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }
        }

        OutlinedTextField(
            value = crashInput,
            onValueChange = onCrashInputChange,
            placeholder = {
                Text(
                    text = "Paste logcat output here...\ne.g. *** *** *** *** ***\nFatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0\nbacktrace:\n  #00 pc 000000000032a1e0 /data/app/.../libGTASA.so",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CodeEditorBg,
                unfocusedContainerColor = CodeEditorBg,
                focusedBorderColor = PandaCyanAccent,
                unfocusedBorderColor = OutlineBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        onCrashInputChange(clip.getItemAt(0).coerceToText(context).toString())
                        Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PandaBgCardElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "📋 Paste Log", color = PandaCyanAccent)
            }

            Button(
                onClick = onAnalyze,
                colors = ButtonDefaults.buttonColors(containerColor = PandaPurplePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.5f)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Analyze with Panda AI", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// Pre-packaged AML & Android NDK Snippets
private val amlPluginSnippets = listOf(
    CodeSnippet(
        title = "AML Plugin Main Template",
        description = "Standard entry point for Android Mod Loader mods in GTA SA",
        language = "cpp",
        code = """
#include <mod/amlmod.h>
#include <mod/logger.h>
#include <mod/config.h>

MYMOD(net.author.myamlmod, "My AML Mod", "1.0", "AuthorName")

extern "C" void OnModLoad() {
    logger->Info("My AML Mod has loaded successfully!");
    
    // Acquire game base address
    void* hGame = aml->GetLib("libGTASA.so");
    uintptr_t pGame = aml->GetLibAddr("libGTASA.so");
    logger->Info("libGTASA.so loaded at %p", (void*)pGame);
}

extern "C" void OnModUnload() {
    logger->Info("My AML Mod has unloaded.");
}
        """.trimIndent(),
        aiPrompt = "Customize this AML plugin skeleton to add a feature in GTA SA Android. Explain each macro and hook mechanism."
    ),
    CodeSnippet(
        title = "AML Memory Patch & NOP",
        description = "Write raw bytes or NOP instructions directly to game memory",
        language = "cpp",
        code = """
// Place NOP (No Operation) to disable an instruction
// arm64: 4 bytes NOP is 0x1F2003D5
aml->PlaceNOP(pGame + 0x3FA200, 1); // 1 instruction

// Write custom bytes (e.g. enable infinite health or ammo patch)
unsigned char newBytes[] = { 0x00, 0x00, 0x80, 0xD2 }; // MOV W0, #0
aml->Write(pGame + 0x4819C0, (uintptr_t)newBytes, sizeof(newBytes));
        """.trimIndent(),
        aiPrompt = "Explain how to write custom memory patches with AML and how to find target function offsets using IDA Pro or Ghidra."
    )
)

private val hookSnippets = listOf(
    CodeSnippet(
        title = "AML Inline Hook (HOOK)",
        description = "Intercept a function, read parameters, and call original function",
        language = "cpp",
        code = """
// 1. Declare original function pointer
void (*orig_CWorld_Add)(void* pEntity);

// 2. Define custom replacement hook
void my_CWorld_Add(void* pEntity) {
    // Custom logic before original call
    logger->Info("CWorld::Add called for entity: %p", pEntity);
    
    // Call original function
    orig_CWorld_Add(pEntity);
}

// 3. Register the hook in OnModLoad()
uintptr_t addr = aml->GetSym(hGame, "_ZN6CWorld3AddEP7CEntity");
if(addr) {
    HOOK(addr, my_CWorld_Add, orig_CWorld_Add);
}
        """.trimIndent(),
        aiPrompt = "Explain how to hook C++ mangled symbols in Android NDK using AML HOOK macro and how to pass arguments safely."
    ),
    CodeSnippet(
        title = "and64hook Arm64 Function Hook",
        description = "Lightweight inline hook for 64-bit Android games",
        language = "cpp",
        code = """
#include <and64hook.h>

void* (*orig_PlayerUpdate)(void* self, float delta);

void* my_PlayerUpdate(void* self, float delta) {
    // Custom modifications to player state
    return orig_PlayerUpdate(self, delta);
}

// Inside OnModLoad:
A64HookFunction((void*)(pGame + 0x38AC90), (void*)my_PlayerUpdate, (void**)&orig_PlayerUpdate);
        """.trimIndent(),
        aiPrompt = "Explain A64HookFunction usage in Android Mod Loader and how to prevent SIGSEGV when calling original functions."
    )
)

private val imguiSnippets = listOf(
    CodeSnippet(
        title = "ImGui Android Touch & Render Loop",
        description = "Inject ImGui menu into GTA San Andreas RenderWare loop",
        language = "cpp",
        code = """
#include <imgui.h>
#include <backends/imgui_impl_android.h>
#include <backends/imgui_impl_opengl3.h>

void RenderMyImGuiMenu() {
    ImGui_ImplOpenGL3_NewFrame();
    ImGui_ImplAndroid_NewFrame();
    ImGui::NewFrame();

    ImGui::Begin("Panda AI AML Cheat Menu", nullptr, ImGuiWindowFlags_AlwaysAutoResize);
    ImGui::Text("Panda AI Native Mod active!");
    if(ImGui::Button("Spawn Jetpack")) {
        // Trigger game cheat
    }
    ImGui::End();

    ImGui::Render();
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
}
        """.trimIndent(),
        aiPrompt = "Provide complete setup for compiling Dear ImGui with AML Compiler for GTA San Andreas Android including touch dispatch."
    )
)

private val buildConfigSnippets = listOf(
    CodeSnippet(
        title = "Android.mk for AML Compiler",
        description = "Standard build file for compiling native .so mod with AML Compiler or AIDE",
        language = "make",
        code = """
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := MyAMLMod
LOCAL_SRC_FILES := main.cpp
LOCAL_CPPFLAGS := -std=c++17 -fvisibility=hidden -Wall
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_LDLIBS := -llog -ldl

include $(BUILD_SHARED_LIBRARY)
        """.trimIndent(),
        aiPrompt = "Explain this Android.mk build configuration for AML Compiler, including arm64-v8a target flags and linking liblog."
    )
)
