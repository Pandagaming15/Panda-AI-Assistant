package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.pref.AppPreferences
import com.example.data.repository.ChatRepository
import com.example.service.FloatingAssistantService
import com.example.ui.screens.AmlToolkitScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PandaBgCard
import com.example.ui.theme.PandaBgDeep
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.PandaPurplePrimary
import com.example.ui.theme.PandaTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Storage
import com.example.ui.screens.MemoryScreen

sealed class Screen(val index: Int, val title: String, val icon: ImageVector) {
    object Dashboard : Screen(0, "Assistant", Icons.Default.Dashboard)
    object Chat : Screen(1, "Chat", Icons.AutoMirrored.Filled.Chat)
    object AmlToolkit : Screen(2, "AML Toolkit", Icons.Default.Build)
    object Memory : Screen(3, "Memory", Icons.Default.Storage)
    object Settings : Screen(4, "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AppPreferences
    private lateinit var repository: ChatRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = AppPreferences(this)
        repository = ChatRepository.getInstance(this)

        setContent {
            PandaTheme {
                MainContent(
                    preferences = preferences,
                    repository = repository,
                    onRequestNotificationPermission = {
                        requestNotificationPermissionIfNeeded()
                    }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
}

@Composable
fun MainContent(
    preferences: AppPreferences,
    repository: ChatRepository,
    onRequestNotificationPermission: () -> Unit
) {
    var selectedScreen by remember { mutableIntStateOf(0) }
    var isServiceRunning by remember { mutableStateOf(FloatingAssistantService.isRunning) }

    // Periodically poll service status to keep dashboard button updated
    LaunchedEffect(Unit) {
        while (true) {
            isServiceRunning = FloatingAssistantService.isRunning
            delay(1000)
        }
    }

    val screens = listOf(
        Screen.Dashboard,
        Screen.Chat,
        Screen.AmlToolkit,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PandaBgDeep,
        bottomBar = {
            NavigationBar(
                containerColor = PandaBgCard,
                tonalElevation = 8.dp
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedScreen == screen.index,
                        onClick = { selectedScreen = screen.index },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PandaCyanAccent,
                            indicatorColor = PandaPurplePrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_${screen.title.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreen) {
                0 -> DashboardScreen(
                    preferences = preferences,
                    isServiceRunning = isServiceRunning,
                    onNavigateToChat = { selectedScreen = 1 },
                    onNavigateToAml = { selectedScreen = 2 },
                    onNavigateToMemory = { selectedScreen = 3 },
                    onNavigateToSettings = { selectedScreen = 4 },
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
                1 -> ChatScreen(
                    repository = repository,
                    onNavigateToSettings = { selectedScreen = 4 }
                )
                2 -> AmlToolkitScreen(
                    repository = repository,
                    onNavigateToChat = { selectedScreen = 1 }
                )
                3 -> MemoryScreen(
                    memoryRepository = repository.memoryRepository
                )
                4 -> SettingsScreen(
                    preferences = preferences,
                    repository = repository
                )
            }
        }
    }
}
