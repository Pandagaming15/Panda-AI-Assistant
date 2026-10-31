package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.data.repository.ChatRepository
import com.example.ui.components.MarkdownContent
import com.example.ui.components.QuickActionsBar
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.PandaBgCard
import com.example.ui.theme.PandaBgCardElevated
import com.example.ui.theme.PandaBgDeep
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.PandaPurplePrimary
import com.example.ui.theme.PandaRedError
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    repository: ChatRepository,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isGenerating by repository.isGenerating.collectAsState()
    val activeConvId by repository.activeConversationId.collectAsState()

    var messages by remember { mutableStateOf<List<ChatMessageEntity>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(activeConvId) {
        val convId = activeConvId ?: repository.getOrCreateActiveConversation()
        repository.getMessagesForConversation(convId).collect {
            messages = it
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PandaBgDeep)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.panda_icon),
                        contentDescription = "Panda AI",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    Column {
                        Text(
                            text = "Panda AI Coding Chat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = repository.preferences.selectedModel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PandaCyanAccent,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            },
            actions = {
                // New Chat
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.createNewConversation()
                            Toast.makeText(context, "New chat started", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = PandaCyanAccent
                    )
                }

                // Clear Chat
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            activeConvId?.let { repository.deleteConversation(it) }
                            repository.getOrCreateActiveConversation()
                            Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = TextSecondary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PandaBgCard
            )
        )

        // API Key warning banner if not configured
        if (!repository.preferences.hasValidApiKey()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF451A03))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚠️ Gemini API key not configured yet.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFDE68A)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Set Key",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PandaCyanAccent,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Quick Actions Bar
        QuickActionsBar(
            onActionSelected = { action ->
                inputText = action.promptPrefix
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Messages Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            if (messages.isEmpty()) {
                InAppEmptyChat(
                    onSelectPrompt = { prompt ->
                        inputText = prompt
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        InAppMessageBubble(
                            message = msg,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("response", msg.content))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onRegenerate = {
                                coroutineScope.launch {
                                    repository.regenerateLastResponse(coroutineScope)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Composer
        InAppInputComposer(
            text = inputText,
            onTextChange = { inputText = it },
            isGenerating = isGenerating,
            onSend = {
                val prompt = inputText.trim()
                if (prompt.isNotBlank()) {
                    inputText = ""
                    repository.sendMessage(prompt, coroutineScope)
                }
            },
            onStop = {
                repository.stopGeneration()
            },
            onPasteClipboard = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val pasteText = clip.getItemAt(0).coerceToText(context).toString()
                    if (pasteText.isNotBlank()) {
                        inputText = if (inputText.isBlank()) pasteText else "$inputText\n$pasteText"
                        Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun InAppMessageBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) Color(0xFF5B21B6) else PandaBgCard)
                .border(
                    1.dp,
                    if (isUser) PandaPurplePrimary else OutlineBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                } else {
                    if (message.content.isBlank() && message.isStreaming) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = PandaCyanAccent
                            )
                            Text(
                                text = "Panda AI is thinking...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary
                                )
                            )
                        }
                    } else {
                        MarkdownContent(content = message.content)
                        if (message.isStreaming) {
                            Text(
                                text = " ▌",
                                color = PandaCyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                if (!isUser && message.content.isNotBlank() && !message.isStreaming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy message",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppEmptyChat(
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.panda_icon),
            contentDescription = "Panda AI Mascot",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Welcome to Panda AI Assistant",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "Your AI companion for C++, AML Mod Loader, and Android NDK",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = PandaCyanAccent
            )
        )
        Spacer(modifier = Modifier.height(20.dp))

        val promptSuggestions = listOf(
            "Write a C++ AML plugin that increases player run speed in GTA SA",
            "How to hook a function at memory offset 0x4B3A80 in libGTASA.so?",
            "Explain Android.mk flags for arm64-v8a native compilation"
        )

        for (prompt in promptSuggestions) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PandaBgCardElevated)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE5E7EB),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun InAppInputComposer(
    text: String,
    onTextChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPasteClipboard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PandaBgCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onPasteClipboard,
            modifier = Modifier
                .testTag("in_app_paste_button")
                .size(44.dp)
                .clip(CircleShape)
                .background(PandaBgCardElevated)
                .border(1.dp, Color(0xFF3730A3), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = "Paste from Clipboard",
                tint = PandaCyanAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    text = "Ask C++/AML question or paste code/error...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                )
            },
            maxLines = 5,
            modifier = Modifier
                .weight(1f)
                .testTag("in_app_chat_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PandaBgDeep,
                unfocusedContainerColor = PandaBgDeep,
                focusedBorderColor = PandaCyanAccent,
                unfocusedBorderColor = OutlineBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        )

        IconButton(
            onClick = { if (isGenerating) onStop() else onSend() },
            modifier = Modifier
                .testTag("in_app_send_button")
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isGenerating) PandaRedError else PandaPurplePrimary)
        ) {
            Icon(
                imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isGenerating) "Stop Generation" else "Send Message",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
