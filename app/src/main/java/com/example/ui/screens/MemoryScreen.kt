package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MemoryDocumentEntity
import com.example.data.repository.MemoryRepository
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
import com.example.ui.theme.PandaRedError
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(
    memoryRepository: MemoryRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val documents by memoryRepository.allDocuments.collectAsState(initial = emptyList())
    val docCount by memoryRepository.documentCount.collectAsState(initial = 0)

    var searchQuery by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var viewingDocument by remember { mutableStateOf<MemoryDocumentEntity?>(null) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // File Picker for local files or archives (.zip, .h, .cpp, .mk, .txt)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isImporting = true
                try {
                    val displayName = getFileNameFromUri(context, uri) ?: "imported_file"
                    val count = memoryRepository.importFromUri(uri, displayName)
                    Toast.makeText(context, "Successfully imported $count document(s) into memory!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isImporting = false
                }
            }
        }
    }

    val filteredDocs = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) documents
        else documents.filter {
            it.filename.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.summary.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalSizeBytes = remember(documents) { documents.sumOf { it.sizeBytes } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PandaBgDeep)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PandaBgCard)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E1738), Color(0xFF101B2B), Color(0xFF141224))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PandaCyanAccent.copy(alpha = 0.5f), PandaPurplePrimary.copy(alpha = 0.5f))),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PandaPurplePrimary.copy(alpha = 0.2f))
                                    .border(1.dp, PandaCyanAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = PandaCyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Project Memory Database",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "RAG indexing for C++ SDKs, headers, and source files",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                )
                            }
                        }

                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PandaCyanAccent,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }

                    // Stats Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F0E1A))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Indexed Files: $docCount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = PandaCyanAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Size: ${formatBytes(totalSizeBytes)}",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                        )
                        Text(
                            text = if (docCount > 0) "● AI Memory Active" else "○ Empty",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (docCount > 0) PandaGreenAccent else Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Import Bundled aml-psdk Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isImporting = true
                        val count = memoryRepository.importBundledAmlPsdk()
                        isImporting = false
                        Toast.makeText(context, "Loaded $count aml-psdk headers into memory!", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_aml_psdk_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PandaPurplePrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isImporting
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Import aml-psdk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Pick Local File / ZIP
            Button(
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/x-zip-compressed",
                            "text/*",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_file_picker_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PandaBgCardElevated),
                shape = RoundedCornerShape(12.dp),
                enabled = !isImporting
            ) {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = PandaCyanAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Import File/ZIP", fontSize = 12.sp, color = PandaCyanAccent, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Search & Manual Snippet Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search imported headers, classes, or symbols...", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PandaBgCard,
                    unfocusedContainerColor = PandaBgCard,
                    focusedBorderColor = PandaCyanAccent,
                    unfocusedBorderColor = OutlineBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            IconButton(
                onClick = { showManualAddDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PandaBgCard)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Paste Custom Snippet", tint = PandaCyanAccent)
            }

            if (docCount > 0) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PandaBgCard)
                        .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear All", tint = PandaRedError)
                }
            }
        }

        // Document List
        Box(modifier = Modifier.weight(1f)) {
            if (filteredDocs.isEmpty()) {
                EmptyMemoryPlaceholder(
                    totalCount = docCount,
                    onImportAml = {
                        coroutineScope.launch {
                            isImporting = true
                            val count = memoryRepository.importBundledAmlPsdk()
                            isImporting = false
                            Toast.makeText(context, "Loaded $count aml-psdk headers into memory!", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        MemoryDocumentRow(
                            document = doc,
                            onToggleEnabled = { enabled ->
                                coroutineScope.launch {
                                    memoryRepository.setDocumentEnabled(doc.id, enabled)
                                }
                            },
                            onView = { viewingDocument = doc },
                            onDelete = {
                                coroutineScope.launch {
                                    memoryRepository.deleteDocument(doc.id)
                                    Toast.makeText(context, "Deleted ${doc.filename}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Inspect Document Content
    viewingDocument?.let { doc ->
        AlertDialog(
            onDismissRequest = { viewingDocument = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.filename,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${doc.filePath} • ${doc.category}",
                            style = MaterialTheme.typography.labelSmall.copy(color = PandaCyanAccent, fontSize = 11.sp)
                        )
                    }
                    IconButton(onClick = { viewingDocument = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    val lang = when {
                        doc.filename.endsWith(".h") || doc.filename.endsWith(".cpp") -> "cpp"
                        doc.filename.endsWith(".java") -> "java"
                        doc.filename.endsWith(".mk") -> "make"
                        else -> "code"
                    }
                    CodeBlockView(
                        code = doc.content,
                        language = lang,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingDocument = null }) {
                    Text("Close", color = PandaCyanAccent)
                }
            },
            containerColor = PandaBgCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog: Manually Paste Custom Code Snippet
    if (showManualAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = {
                Text(
                    text = "Add Custom Code to Memory",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Filename or Class Name (e.g. MyHooks.h)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Code / Header Content") },
                        placeholder = { Text("struct CustomData { ... };\nvoid MyHook();") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                            coroutineScope.launch {
                                memoryRepository.importFromText(newTitle.trim(), newContent.trim(), "Custom Snippet")
                                showManualAddDialog = false
                                Toast.makeText(context, "Added $newTitle to memory database!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PandaPurplePrimary)
                ) {
                    Text("Save to Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = PandaBgCard
        )
    }

    // Dialog: Confirm Clear All
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Entire Memory Database?") },
            text = { Text("This will remove all $docCount indexed project files and headers from memory.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            memoryRepository.clearAllDocuments()
                            showClearDialog = false
                            Toast.makeText(context, "Memory database cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Delete All", color = PandaRedError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = PandaBgCard
        )
    }
}

@Composable
private fun MemoryDocumentRow(
    document: MemoryDocumentEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onView),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PandaBgCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = document.filename,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (document.isEnabled) Color.White else TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E1B33))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = document.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PandaCyanAccent,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${document.filePath} • ${formatBytes(document.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = document.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PandaPurplePrimary,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF282444)
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMemoryPlaceholder(
    totalCount: Int,
    onImportAml: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = null,
            tint = PandaCyanAccent,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (totalCount == 0) "No Documents in Memory" else "No Matching Documents",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        Text(
            text = if (totalCount == 0)
                "Import your C++ plugin-SDK headers or custom files so Panda AI learns your specific types and functions."
            else
                "Try searching for another header name or symbol.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        if (totalCount == 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onImportAml,
                colors = ButtonDefaults.buttonColors(containerColor = PandaPurplePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import aml-psdk Knowledge Base")
            }
        }
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}
