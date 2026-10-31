package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.zip.ZipInputStream

class MemoryRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context)
) {
    companion object {
        private const val TAG = "MemoryRepository"

        @Volatile
        private var INSTANCE: MemoryRepository? = null

        fun getInstance(context: Context): MemoryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MemoryRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        // File extensions supported for indexing into AI memory database
        private val SUPPORTED_EXTENSIONS = setOf(
            "h", "hpp", "hxx", "c", "cpp", "cxx", "cc", "java", "kt", "mk",
            "cmake", "txt", "md", "json", "xml", "ini", "def"
        )
    }

    private val memoryDao = database.memoryDocumentDao()

    val allDocuments: Flow<List<MemoryDocumentEntity>> = memoryDao.getAllDocuments()
    val documentCount: Flow<Int> = memoryDao.getDocumentCount()

    suspend fun getDocumentCountSync(): Int = withContext(Dispatchers.IO) {
        memoryDao.getDocumentCountSync()
    }

    suspend fun importFromUri(uri: Uri, displayName: String): Int = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        val lowerName = displayName.lowercase()

        if (lowerName.endsWith(".zip")) {
            cr.openInputStream(uri)?.use { inputStream ->
                return@withContext importFromZip(inputStream, category = "Imported ZIP Archive")
            } ?: 0
        } else {
            val content = cr.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext 0

            val ext = displayName.substringAfterLast('.', "")
            val category = detectCategory(displayName)
            val doc = MemoryDocumentEntity(
                id = UUID.randomUUID().toString(),
                filename = displayName,
                filePath = displayName,
                category = category,
                content = content,
                summary = extractSymbolsSummary(content),
                sizeBytes = content.toByteArray().size.toLong(),
                importedAt = System.currentTimeMillis(),
                isEnabled = true
            )
            memoryDao.insertDocument(doc)
            1
        }
    }

    suspend fun importFromText(
        filename: String,
        content: String,
        category: String = "Custom File"
    ): Unit = withContext(Dispatchers.IO) {
        val doc = MemoryDocumentEntity(
            id = UUID.randomUUID().toString(),
            filename = filename,
            filePath = filename,
            category = category,
            content = content,
            summary = extractSymbolsSummary(content),
            sizeBytes = content.toByteArray().size.toLong(),
            importedAt = System.currentTimeMillis(),
            isEnabled = true
        )
        memoryDao.insertDocument(doc)
    }

    suspend fun importBundledAmlPsdk(): Int = withContext(Dispatchers.IO) {
        try {
            context.assets.open("aml-psdk.zip").use { inputStream ->
                importFromZip(inputStream, category = "AML Plugin-SDK (GTA SA)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing bundled aml-psdk: ${e.message}", e)
            0
        }
    }

    suspend fun importFromZip(
        inputStream: InputStream,
        category: String = "Imported Archive"
    ): Int = withContext(Dispatchers.IO) {
        val documents = mutableListOf<MemoryDocumentEntity>()
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry

        while (entry != null) {
            val entryName = entry.name
            val filename = entryName.substringAfterLast('/')
            val ext = filename.substringAfterLast('.', "").lowercase()

            if (!entry.isDirectory && SUPPORTED_EXTENSIONS.contains(ext) && !filename.startsWith(".")) {
                // Read text entry
                val reader = BufferedReader(InputStreamReader(zipIn, Charsets.UTF_8))
                val sb = StringBuilder()
                var line: String?
                // Limit maximum characters per single file to prevent OOM on enormous generated files
                var charCount = 0
                val maxChars = 80_000

                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: break
                    sb.append(l).append("\n")
                    charCount += l.length + 1
                    if (charCount > maxChars) {
                        sb.append("\n// [Content truncated for memory indexing...]\n")
                        break
                    }
                }

                val content = sb.toString()
                if (content.isNotBlank()) {
                    val itemCategory = when {
                        entryName.contains("renderware", ignoreCase = true) -> "RenderWare SDK"
                        entryName.contains("game_sa", ignoreCase = true) -> "GTA SA SDK"
                        entryName.contains("examples", ignoreCase = true) -> "AML Examples"
                        ext in listOf("h", "hpp") -> "C++ Header"
                        ext in listOf("cpp", "c") -> "C++ Source"
                        ext in listOf("mk", "cmake") -> "Build Config"
                        else -> category
                    }

                    val doc = MemoryDocumentEntity(
                        id = UUID.randomUUID().toString(),
                        filename = filename,
                        filePath = entryName,
                        category = itemCategory,
                        content = content,
                        summary = extractSymbolsSummary(content),
                        sizeBytes = content.toByteArray().size.toLong(),
                        importedAt = System.currentTimeMillis(),
                        isEnabled = true
                    )
                    documents.add(doc)
                }
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        if (documents.isNotEmpty()) {
            memoryDao.insertDocuments(documents)
        }
        documents.size
    }

    suspend fun setDocumentEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        memoryDao.setDocumentEnabled(id, enabled)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        memoryDao.deleteDocument(id)
    }

    suspend fun clearAllDocuments() = withContext(Dispatchers.IO) {
        memoryDao.clearAllDocuments()
    }

    /**
     * Retrieval-Augmented Context search:
     * Extracts tokens from user prompt, finds matching files from memory,
     * and compiles an injection block for the AI model so it adheres directly to the user's codebase.
     */
    suspend fun findRelevantKnowledge(query: String, maxDocs: Int = 3): String = withContext(Dispatchers.IO) {
        val enabledDocs = memoryDao.getEnabledDocuments()
        if (enabledDocs.isEmpty()) return@withContext ""

        // Extract identifier tokens (e.g. CWorld, Events, drawingEvent, aml, Hook, CPed, CVehicle)
        val tokens = Regex("[A-Za-z0-9_]{3,}")
            .findAll(query)
            .map { it.value }
            .filter { !STOP_WORDS.contains(it.lowercase()) }
            .toList()

        if (tokens.isEmpty()) return@withContext ""

        // Score each document based on filename matches and symbol/content occurrences
        val scoredDocs = enabledDocs.map { doc ->
            var score = 0
            val lowerFilename = doc.filename.lowercase()
            val lowerSummary = doc.summary.lowercase()

            for (token in tokens) {
                val lowerToken = token.lowercase()
                if (lowerFilename.contains(lowerToken)) {
                    score += 15
                }
                if (doc.filename.equals(token, ignoreCase = true) || doc.filename.startsWith(token, ignoreCase = true)) {
                    score += 25
                }
                if (lowerSummary.contains(lowerToken)) {
                    score += 8
                }
                if (doc.content.contains(token)) {
                    score += 2
                }
            }
            doc to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxDocs)

        if (scoredDocs.isEmpty()) return@withContext ""

        val sb = StringBuilder()
        sb.append("\n\n=== [PROJECT MEMORY & IMPORTED SDK KNOWLEDGE BASE] ===\n")
        sb.append("The following definitions and headers are imported into memory from the user's project/SDK. Always adhere strictly to these structures, types, function signatures, and hook conventions:\n\n")

        for ((doc, _) in scoredDocs) {
            sb.append("--- FILE: ${doc.filePath} (${doc.category}) ---\n")
            // Include summary and truncated header content
            val snippet = if (doc.content.length > 2500) {
                doc.content.take(2500) + "\n// ... [remaining file truncated] ..."
            } else {
                doc.content
            }
            sb.append(snippet).append("\n\n")
        }
        sb.append("=== [END OF IMPORTED MEMORY KNOWLEDGE BASE] ===\n\n")
        sb.toString()
    }

    private fun detectCategory(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "h", "hpp" -> "C++ Header"
            "cpp", "c", "cc" -> "C++ Source"
            "java" -> "Java File"
            "mk", "cmake" -> "Build Config"
            else -> "Source Document"
        }
    }

    private fun extractSymbolsSummary(code: String): String {
        val lines = code.lines().take(400)
        val symbols = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("class ") || trimmed.startsWith("struct ") ||
                trimmed.startsWith("enum ") || trimmed.startsWith("#define ") ||
                trimmed.startsWith("extern ") || trimmed.contains("HOOK") ||
                trimmed.startsWith("MYMOD(") || trimmed.contains("Event")
            ) {
                symbols.add(trimmed.take(120))
                if (symbols.size > 25) break
            }
        }
        return symbols.joinToString("\n")
    }

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "that", "this", "from", "how", "what", "where",
        "make", "code", "want", "help", "please", "can", "you", "error", "write", "generate"
    )
}
