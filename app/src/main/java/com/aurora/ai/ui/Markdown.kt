package com.aurora.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface Block {
    data class Para(val t: String) : Block
    data class Heading(val level: Int, val t: String) : Block
    data class Bullet(val marker: String, val t: String) : Block
    data class Code(val lang: String, val code: String) : Block
}

private val HeadingRegex = Regex("^#{1,3} .*")
private val NumberedRegex = Regex("^\\d+[.)] .*")

/** Parser Markdown ringan: paragraf, heading, daftar, dan blok kode. */
fun parseBlocks(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val lines = src.lines()
    val para = StringBuilder()

    fun flush() {
        if (para.isNotEmpty()) {
            out.add(Block.Para(para.toString().trim()))
            para.clear()
        }
    }

    var i = 0
    while (i < lines.size) {
        val tl = lines[i].trim()
        when {
            tl.startsWith("```") -> {
                flush()
                val lang = tl.removePrefix("```").trim()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code.appendLine(lines[i])
                    i++
                }
                out.add(Block.Code(lang, code.toString().trimEnd()))
            }
            HeadingRegex.matches(tl) -> {
                flush()
                val level = tl.takeWhile { it == '#' }.length
                out.add(Block.Heading(level, tl.dropWhile { it == '#' }.trim()))
            }
            tl.startsWith("- ") || tl.startsWith("* ") || tl.startsWith("• ") -> {
                flush()
                out.add(Block.Bullet("•", tl.drop(2).trim()))
            }
            NumberedRegex.matches(tl) -> {
                flush()
                out.add(Block.Bullet(tl.substringBefore(' '), tl.substringAfter(' ').trim()))
            }
            tl.isEmpty() -> flush()
            else -> {
                if (para.isNotEmpty()) para.append('\n')
                para.append(tl)
            }
        }
        i++
    }
    flush()
    return out
}

/** Inline: **tebal** dan `kode`. */
fun inlineMd(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = GoldSoft)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
            } else {
                append("**")
                i += 2
            }
        } else if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > 0) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = Gold,
                        background = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
            } else {
                append('`')
                i++
            }
        } else {
            append(text[i])
            i++
        }
    }
}

@Composable
fun MarkdownText(text: String, color: Color, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseBlocks(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { b ->
            when (b) {
                is Block.Para -> Text(
                    inlineMd(b.t),
                    style = MaterialTheme.typography.bodyLarge,
                    color = color
                )
                is Block.Heading -> Text(
                    inlineMd(b.t),
                    color = GoldSoft,
                    fontFamily = SerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = when (b.level) {
                        1 -> 22.sp
                        2 -> 19.sp
                        else -> 17.sp
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
                is Block.Bullet -> Row(Modifier.fillMaxWidth()) {
                    Text(
                        b.marker,
                        color = Gold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        inlineMd(b.t),
                        style = MaterialTheme.typography.bodyLarge,
                        color = color,
                        modifier = Modifier.weight(1f)
                    )
                }
                is Block.Code -> CodeBlock(b.lang, b.code)
            }
        }
    }
}

@Composable
private fun CodeBlock(lang: String, code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CodeBg)
            .border(BorderStroke(1.dp, Hairline), shape)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                lang.ifBlank { "kode" },
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (copied) "Tersalin ✓" else "Salin",
                style = MaterialTheme.typography.labelSmall,
                color = Gold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(
            code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Color(0xFFD7D3C8),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}
