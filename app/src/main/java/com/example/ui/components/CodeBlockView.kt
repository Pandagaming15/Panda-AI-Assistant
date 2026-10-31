package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeEditorBg
import com.example.ui.theme.CodeEditorBorder
import com.example.ui.theme.CodeFunction
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeNumber
import com.example.ui.theme.CodeString
import com.example.ui.theme.CodeType
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.PandaGreenAccent
import kotlinx.coroutines.delay

@Composable
fun CodeBlockView(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val displayLang = when (language.lowercase().trim()) {
        "cpp", "c++", "c" -> "C++ (AML / NDK)"
        "java" -> "Java / JNI"
        "kotlin", "kt" -> "Kotlin"
        "make", "makefile", "mk" -> "Android.mk"
        "cmake" -> "CMakeLists.txt"
        "sh", "bash" -> "Shell / Terminal"
        "json" -> "JSON"
        "xml" -> "XML"
        else -> if (language.isNotBlank()) language.uppercase() else "CODE"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CodeEditorBg)
            .border(1.dp, CodeEditorBorder, RoundedCornerShape(8.dp))
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161426))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PandaCyanAccent)
                )
                Text(
                    text = "  $displayLang",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PandaCyanAccent,
                        fontSize = 11.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = isCopied,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Copied!",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PandaGreenAccent,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("code", code)
                        clipboard.setPrimaryClip(clip)
                        isCopied = true
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = if (isCopied) PandaGreenAccent else Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Code Content with syntax highlighting
        val scrollState = rememberScrollState()
        val highlighted = remember(code, language) {
            highlightCode(code, language)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(12.dp)
        ) {
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Lightweight, high-performance syntax highlighter for C++, Java, Makefiles, and shell code.
 */
private fun highlightCode(code: String, language: String) = buildAnnotatedString {
    val lines = code.lines()
    val keywords = setOf(
        "int", "void", "char", "float", "double", "bool", "auto", "const", "static", "struct", "class",
        "public", "private", "protected", "virtual", "override", "return", "if", "else", "for", "while",
        "switch", "case", "break", "continue", "default", "sizeof", "new", "delete", "nullptr", "NULL",
        "template", "typename", "inline", "extern", "typedef", "uintptr_t", "intptr_t", "size_t",
        "include", "define", "ifdef", "ifndef", "endif", "import", "package", "val", "var", "fun",
        "JNIEXPORT", "JNICALL", "jstring", "jint", "jobject", "jclass", "JNIEnv", "JavaVM"
    )

    for ((lineIdx, line) in lines.withIndex()) {
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("//") || trimmed.startsWith("#") && !trimmed.startsWith("#include") && !trimmed.startsWith("#define") -> {
                pushStyle(SpanStyle(color = CodeComment))
                append(line)
                pop()
            }
            trimmed.startsWith("/*") || trimmed.startsWith("*") -> {
                pushStyle(SpanStyle(color = CodeComment))
                append(line)
                pop()
            }
            else -> {
                // Word by word highlighting
                val regex = Regex("([a-zA-Z_][a-zA-Z0-9_]*|\"[^\"]*\"|'[^']*'|0x[0-9a-fA-F]+|[0-9]+|[^a-zA-Z0-9_\"'\\s]+|\\s+)")
                val matches = regex.findAll(line)
                for (match in matches) {
                    val word = match.value
                    when {
                        word in keywords -> {
                            pushStyle(SpanStyle(color = CodeKeyword, fontWeight = FontWeight.Bold))
                            append(word)
                            pop()
                        }
                        word.startsWith("\"") || word.startsWith("'") -> {
                            pushStyle(SpanStyle(color = CodeString))
                            append(word)
                            pop()
                        }
                        word.startsWith("0x") || word.toIntOrNull() != null -> {
                            pushStyle(SpanStyle(color = CodeNumber))
                            append(word)
                            pop()
                        }
                        word.firstOrNull()?.isUpperCase() == true && word.length > 1 -> {
                            pushStyle(SpanStyle(color = CodeType))
                            append(word)
                            pop()
                        }
                        word.endsWith("(") -> {
                            pushStyle(SpanStyle(color = CodeFunction))
                            append(word)
                            pop()
                        }
                        else -> {
                            pushStyle(SpanStyle(color = Color(0xFFE2E8F0)))
                            append(word)
                            pop()
                        }
                    }
                }
            }
        }
        if (lineIdx < lines.lastIndex) {
            append("\n")
        }
    }
}
