package com.example.ui.floating

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.PandaGreenAccent
import com.example.ui.theme.PandaPurplePrimary
import com.example.ui.theme.PandaRedError
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun FloatingChatContent(
    repository: ChatRepository,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onToggleExpand: () -> Unit,
    isExpanded: Boolean,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = PandaCyanAccent)
            .clip(RoundedCornerShape(18.dp))
            .background(PandaBgDeep)
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(PandaCyanAccent.copy(alpha = 0.8f), PandaPurplePrimary.copy(alpha = 0.4f))
                ),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            FloatingChatHeader(
                repository = repository,
                isExpanded = isExpanded,
                onToggleExpand = onToggleExpand,
                onMinimize = onMinimize,
                onClose = onClose,
                onClearChat = {
                    coroutineScope.launch {
                        activeConvId?.let { repository.deleteConversation(it) }
                        repository.getOrCreateActiveConversation()
                        Toast.makeText(context, "Conversation cleared", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Quick Actions Bar
            QuickActionsBar(
                onActionSelected = { action ->
                    inputText = action.promptPrefix
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Conversation Messages
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                if (messages.isEmpty()) {
                    FloatingEmptyState(
                        onSelectSample = { prompt ->
                            inputText = prompt
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            FloatingMessageItem(
                                message = message,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("response", message.content))
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

            // Input Composer
            FloatingInputComposer(
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
}

@Composable
private fun FloatingChatHeader(
    repository: ChatRepository,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onClearChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131122))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.panda_icon),
                contentDescription = "Panda AI",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )

            Column {
                Text(
                    text = "Panda AI",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = repository.preferences.selectedModel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PandaCyanAccent,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Clear Chat
            IconButton(
                onClick = onClearChat,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expand / Compact toggle
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Size",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Minimize to bubble
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Minimize,
                    contentDescription = "Minimize to Bubble",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Close floating assistant
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Assistant",
                    tint = PandaRedError,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingMessageItem(
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
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .background(if (isUser) Color(0xFF6D28D9) else PandaBgCard)
                .border(
                    1.dp,
                    if (isUser) PandaPurplePrimary else OutlineBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(10.dp)
        ) {
            Column {
                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    )
                } else {
                    if (message.content.isBlank() && message.isStreaming) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PandaCyanAccent
                            )
                            Text(
                                text = "Panda AI is thinking...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
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
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // AI Message Bottom Toolbar (Copy & Regenerate)
                if (!isUser && message.content.isNotBlank() && !message.isStreaming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy message",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingEmptyState(
    onSelectSample: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.panda_icon),
            contentDescription = "Panda AI Mascot",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Panda AI Floating Assistant",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = "Ready for AML Compiler & Android NDK Coding",
            style = MaterialTheme.typography.bodySmall.copy(
                color = PandaCyanAccent,
                fontSize = 12.sp
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Tap a template below or paste code/errors directly:",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val samples = listOf(
            "How do I hook a C++ function in GTA SA with AML?",
            "Fix: undefined reference to '__android_log_print'",
            "Generate ImGui menu hook for Android Mod Loader"
        )
        for (sample in samples) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PandaBgCardElevated)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "• $sample",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFD1D5DB),
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FloatingInputComposer(
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
            .background(Color(0xFF131122))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Floating Paste Button
        IconButton(
            onClick = onPasteClipboard,
            modifier = Modifier
                .testTag("floating_paste_button")
                .size(38.dp)
                .clip(CircleShape)
                .background(PandaBgCardElevated)
                .border(1.dp, Color(0xFF3730A3), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = "Paste from Clipboard",
                tint = PandaCyanAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        // Text Field
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    text = "Ask Panda AI or paste error...",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280), fontSize = 12.sp)
                )
            },
            maxLines = 4,
            modifier = Modifier
                .weight(1f)
                .testTag("floating_chat_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PandaBgDeep,
                unfocusedContainerColor = PandaBgDeep,
                focusedBorderColor = PandaCyanAccent,
                unfocusedBorderColor = OutlineBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Send or Stop Button
        IconButton(
            onClick = { if (isGenerating) onStop() else onSend() },
            modifier = Modifier
                .testTag("floating_send_button")
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isGenerating) PandaRedError else PandaPurplePrimary)
        ) {
            Icon(
                imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isGenerating) "Stop Generation" else "Send Message",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
