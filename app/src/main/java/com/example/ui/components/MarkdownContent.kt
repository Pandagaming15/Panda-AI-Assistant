package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.PandaPurpleLight
import com.example.ui.theme.TextPrimary

sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Code(val language: String, val code: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
}

@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { parseMarkdown(content) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 16.sp
                        else -> 14.sp
                    }
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = PandaPurpleLight
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.Code -> {
                    CodeBlockView(
                        code = block.code,
                        language = block.language,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(PandaCyanAccent)
                        )
                        Text(
                            text = formatInlineMarkdown(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                lineHeight = 20.sp,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            lineHeight = 20.sp,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

private fun parseMarkdown(raw: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = raw.lines()

    var inCodeBlock = false
    var codeLang = ""
    val codeBuilder = StringBuilder()
    val paraBuilder = StringBuilder()

    fun flushPara() {
        val text = paraBuilder.toString().trim()
        if (text.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(text))
            paraBuilder.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // End code block
                blocks.add(MarkdownBlock.Code(codeLang, codeBuilder.toString().trimEnd()))
                codeBuilder.clear()
                codeLang = ""
                inCodeBlock = false
            } else {
                flushPara()
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBuilder.append(line).append("\n")
            continue
        }

        // Heading
        if (trimmed.startsWith("#")) {
            flushPara()
            val level = trimmed.takeWhile { it == '#' }.length
            val title = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Heading(level, title))
            continue
        }

        // Bullet list
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            flushPara()
            val text = trimmed.drop(2).trim()
            blocks.add(MarkdownBlock.BulletItem(text))
            continue
        }

        if (trimmed.isEmpty()) {
            flushPara()
        } else {
            if (paraBuilder.isNotEmpty()) paraBuilder.append(" ")
            paraBuilder.append(trimmed)
        }
    }

    if (inCodeBlock) {
        blocks.add(MarkdownBlock.Code(codeLang, codeBuilder.toString().trimEnd()))
    } else {
        flushPara()
    }

    return blocks
}

private fun formatInlineMarkdown(text: String) = buildAnnotatedString {
    var i = 0
    val len = text.length

    while (i < len) {
        when {
            // Bold **text**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val boldText = text.substring(i + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                    append(boldText)
                    pop()
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }

            // Inline code `code`
            text.startsWith("`", i) && !text.startsWith("```", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    val inlineCode = text.substring(i + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = PandaCyanAccent,
                            background = Color(0xFF1E1B33),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(" $inlineCode ")
                    pop()
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            else -> {
                append(text[i])
                i++
            }
        }
    }
}
