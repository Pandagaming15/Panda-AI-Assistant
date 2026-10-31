package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.pref.AppPreferences
import com.example.service.FloatingAssistantService
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.PandaAmberWarning
import com.example.ui.theme.PandaBgCard
import com.example.ui.theme.PandaBgCardElevated
import com.example.ui.theme.PandaBgDeep
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.PandaCyanGlow
import com.example.ui.theme.PandaGreenAccent
import com.example.ui.theme.PandaPurplePrimary
import com.example.ui.theme.PandaRedError
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    preferences: AppPreferences,
    isServiceRunning: Boolean,
    onNavigateToChat: () -> Unit,
    onNavigateToAml: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val hasOverlay = Settings.canDrawOverlays(context)
    val hasApiKey = preferences.hasValidApiKey()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PandaBgDeep)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = PandaCyanAccent),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PandaBgCard)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E163B),
                                Color(0xFF0F172A),
                                Color(0xFF141224)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PandaCyanAccent.copy(alpha = 0.5f), PandaPurplePrimary.copy(alpha = 0.5f))),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.panda_icon),
                        contentDescription = "Panda AI Mascot",
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(2.dp, PandaCyanGlow, CircleShape)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Panda AI",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 22.sp
                                )
                            )
                            StatusPill(isActive = isServiceRunning)
                        }

                        Text(
                            text = "Floating C++ & AML Assistant",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = PandaCyanAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = "Model: ${preferences.selectedModel}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Primary Floating Assistant Action Button
        Button(
            onClick = {
                if (!hasOverlay) {
                    requestOverlayPermission(context)
                    return@Button
                }
                if (!hasApiKey) {
                    Toast.makeText(context, "Please configure your Gemini API Key in Settings first", Toast.LENGTH_LONG).show()
                    onNavigateToSettings()
                    return@Button
                }
                if (isServiceRunning) {
                    FloatingAssistantService.stopService(context)
                    Toast.makeText(context, "Floating Assistant Stopped", Toast.LENGTH_SHORT).show()
                } else {
                    onRequestNotificationPermission()
                    FloatingAssistantService.startService(context)
                    Toast.makeText(context, "Floating Assistant Started! Drag the bubble or open AML Compiler", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("toggle_floating_service_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServiceRunning) PandaRedError else PandaPurplePrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isServiceRunning) "Stop Floating Assistant" else "Start Floating Assistant",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        // Permission & Status Checklist
        Text(
            text = "System Permissions & Readiness",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        StatusCard(
            title = "Floating Window (Overlay) Permission",
            subtitle = if (hasOverlay) "Granted: Assistant can float over AML Compiler" else "Required: Allows Panda AI to float over other apps",
            isConfigured = hasOverlay,
            actionLabel = if (hasOverlay) "Granted" else "Grant Permission",
            onAction = { requestOverlayPermission(context) }
        )

        StatusCard(
            title = "AI Provider Configuration",
            subtitle = if (hasApiKey) "Active (${preferences.providerType.displayName})" else "Required: Set your Gemini or OpenAI API Key",
            isConfigured = hasApiKey,
            actionLabel = if (hasApiKey) "Configured" else "Enter API Key",
            onAction = onNavigateToSettings
        )

        // Workflow / Quickstart Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PandaBgCardElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = PandaCyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How to use with AML Compiler & AIDE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                val steps = listOf(
                    "1. Tap 'Start Floating Assistant' above.",
                    "2. Switch to AML Compiler, AIDE, or any file manager.",
                    "3. Notice the small Panda AI bubble docked on your screen edge.",
                    "4. Tap the bubble to open the full chat without leaving AML.",
                    "5. Ask for C++ hooks, ImGui menus, or tap 'Paste' to input compiler errors.",
                    "6. Tap 'Copy Code' inside the AI response and paste into your .cpp file!"
                )

                for (step in steps) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // Quick Navigation Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavTile(
                title = "In-App Chat",
                subtitle = "Full screen mode",
                icon = Icons.Default.Code,
                color = PandaPurplePrimary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToChat
            )

            NavTile(
                title = "AML Toolkit",
                subtitle = "NDK Templates",
                icon = Icons.Default.Security,
                color = PandaCyanAccent,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToAml
            )

            NavTile(
                title = "Settings",
                subtitle = "API & Model",
                icon = Icons.Default.Settings,
                color = PandaGreenAccent,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun StatusPill(isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) PandaGreenAccent.copy(alpha = 0.2f) else PandaAmberWarning.copy(alpha = 0.2f))
            .border(1.dp, if (isActive) PandaGreenAccent else PandaAmberWarning, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isActive) "ACTIVE" else "IDLE",
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isActive) PandaGreenAccent else PandaAmberWarning,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun StatusCard(
    title: String,
    subtitle: String,
    isConfigured: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PandaBgCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isConfigured) PandaGreenAccent else PandaAmberWarning,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (!isConfigured) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PandaPurplePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = actionLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun NavTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PandaBgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
        }
    }
}

private fun requestOverlayPermission(context: Context) {
    if (!Settings.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
        Toast.makeText(context, "Enable 'Display over other apps' for Panda AI", Toast.LENGTH_LONG).show()
    }
}
