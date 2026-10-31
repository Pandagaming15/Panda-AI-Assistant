package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiProviderType
import com.example.data.pref.AppPreferences
import com.example.data.repository.ChatRepository
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.PandaBgCard
import com.example.ui.theme.PandaBgCardElevated
import com.example.ui.theme.PandaBgDeep
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.PandaGreenAccent
import com.example.ui.theme.PandaPurpleLight
import com.example.ui.theme.PandaPurplePrimary
import com.example.ui.theme.PandaRedError
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    repository: ChatRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var providerType by remember { mutableStateOf(preferences.providerType) }
    var geminiApiKey by remember { mutableStateOf(preferences.geminiApiKey) }
    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    var selectedGeminiModel by remember { mutableStateOf(preferences.selectedModel) }

    var openAiEndpoint by remember { mutableStateOf(preferences.openAiEndpoint) }
    var openAiApiKey by remember { mutableStateOf(preferences.openAiApiKey) }
    var openAiModel by remember { mutableStateOf(preferences.openAiModel) }

    var temperature by remember { mutableFloatStateOf(preferences.temperature) }
    var systemPrompt by remember { mutableStateOf(preferences.systemPrompt) }
    var autoMinimize by remember { mutableStateOf(preferences.autoMinimizeOnOutsideTap) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PandaBgDeep)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = PandaCyanAccent,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "AI Configuration & Preferences",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Manage your API keys, model parameters, and assistant behavior",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )
            }
        }

        // Section: AI Provider Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PandaBgCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI Provider",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProviderOptionCard(
                        title = "Google Gemini",
                        subtitle = "Default & Recommended",
                        isSelected = providerType == AiProviderType.GEMINI,
                        onClick = {
                            providerType = AiProviderType.GEMINI
                            preferences.providerType = AiProviderType.GEMINI
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ProviderOptionCard(
                        title = "OpenAI-Compatible",
                        subtitle = "Custom API / Ollama",
                        isSelected = providerType == AiProviderType.OPENAI_COMPATIBLE,
                        onClick = {
                            providerType = AiProviderType.OPENAI_COMPATIBLE
                            preferences.providerType = AiProviderType.OPENAI_COMPATIBLE
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section: Gemini Settings
        if (providerType == AiProviderType.GEMINI) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PandaBgCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Google Gemini Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PandaPurpleLight
                        )
                    )

                    // API Key Field
                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = {
                            geminiApiKey = it
                            preferences.geminiApiKey = it
                        },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isGeminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (geminiApiKey.isNotBlank()) "✅ Key configured" else "⚠️ API key is required",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (geminiApiKey.isNotBlank()) PandaGreenAccent else Color(0xFFFBBF24),
                                fontSize = 12.sp
                            )
                        )

                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                context.startActivity(intent)
                            }
                        ) {
                            Text(text = "Get Key from AI Studio", color = PandaCyanAccent, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = PandaCyanAccent)
                        }
                    }

                    // Model Choices
                    Text(
                        text = "Select Gemini Model",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )

                    val geminiModels = listOf(
                        "gemini-3.8-flash" to "Latest & recommended for coding (Default)",
                        "gemini-3.0-flash" to "High-speed reasoning model",
                        "gemini-3.5-pro" to "Advanced deep logic and analysis"
                    )

                    for ((modelName, modelDesc) in geminiModels) {
                        ModelOptionRow(
                            name = modelName,
                            description = modelDesc,
                            isSelected = selectedGeminiModel == modelName,
                            onClick = {
                                selectedGeminiModel = modelName
                                preferences.selectedModel = modelName
                            }
                        )
                    }
                }
            }
        }

        // Section: OpenAI-Compatible Settings
        if (providerType == AiProviderType.OPENAI_COMPATIBLE) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PandaBgCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "OpenAI-Compatible Endpoint",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PandaCyanAccent
                        )
                    )

                    OutlinedTextField(
                        value = openAiEndpoint,
                        onValueChange = {
                            openAiEndpoint = it
                            preferences.openAiEndpoint = it
                        },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = openAiApiKey,
                        onValueChange = {
                            openAiApiKey = it
                            preferences.openAiApiKey = it
                        },
                        label = { Text("API Key (or Bearer Token)") },
                        placeholder = { Text("sk-...") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = openAiModel,
                        onValueChange = {
                            openAiModel = it
                            preferences.openAiModel = it
                        },
                        label = { Text("Model Identifier") },
                        placeholder = { Text("gpt-4o-mini or deepseek-chat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Section: Generation Parameters & System Prompt
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PandaBgCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Creativity & Temperature",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = String.format("%.2f", temperature),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = PandaCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "Lower values (0.2 - 0.4) generate more deterministic, bug-free C++ code.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )

                Slider(
                    value = temperature,
                    onValueChange = {
                        temperature = it
                        preferences.temperature = it
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = PandaCyanAccent,
                        activeTrackColor = PandaPurplePrimary,
                        inactiveTrackColor = PandaBgCardElevated
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // System Instruction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Prompt (NDK & AML)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    TextButton(
                        onClick = {
                            systemPrompt = AppPreferences.DEFAULT_SYSTEM_PROMPT
                            preferences.systemPrompt = AppPreferences.DEFAULT_SYSTEM_PROMPT
                            Toast.makeText(context, "System prompt reset to default", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(text = "Reset Default", color = PandaCyanAccent, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = {
                        systemPrompt = it
                        preferences.systemPrompt = it
                    },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
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
            }
        }

        // Section: Floating Window Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PandaBgCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Floating Window Behavior",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-minimize on outside tap",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Tapping outside the chat window minimizes it back to the bubble",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Switch(
                        checked = autoMinimize,
                        onCheckedChange = {
                            autoMinimize = it
                            preferences.autoMinimizeOnOutsideTap = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PandaPurplePrimary
                        )
                    )
                }

                Button(
                    onClick = {
                        preferences.lastBubbleX = 50
                        preferences.lastBubbleY = 300
                        Toast.makeText(context, "Floating bubble coordinates reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PandaBgCardElevated),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Reset Bubble Screen Position", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Section: Storage & History
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PandaBgCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Chat History & Data",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Text(
                    text = "Conversations are stored locally on your device in SQLite/Room.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )

                Button(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PandaRedError.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Clear All Conversations", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(text = "Clear All Chat History?") },
            text = { Text(text = "This will permanently delete all stored conversations and messages.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.clearAllConversations()
                            showClearConfirmDialog = false
                            Toast.makeText(context, "All conversations cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(text = "Delete All", color = PandaRedError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = "Cancel", color = TextSecondary)
                }
            },
            containerColor = PandaBgCard,
            titleContentColor = Color.White,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun ProviderOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PandaPurplePrimary.copy(alpha = 0.2f) else PandaBgCardElevated)
            .border(
                1.5.dp,
                if (isSelected) PandaCyanAccent else OutlineBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else TextSecondary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isSelected) PandaCyanAccent else Color(0xFF6B7280),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun ModelOptionRow(
    name: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PandaPurplePrimary.copy(alpha = 0.15f) else PandaBgDeep)
            .border(
                1.dp,
                if (isSelected) PandaCyanAccent else OutlineBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PandaCyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
