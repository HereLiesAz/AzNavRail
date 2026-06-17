package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzDivider

/**
 * A dependency-free Markdown renderer tuned to the AzNavRail aesthetic.
 *
 * It is intentionally lightweight (no third-party markdown library): it covers the common subset
 * found in repo docs — ATX headings, paragraphs, bold/italic, inline code, fenced code blocks,
 * unordered/ordered lists, block-quotes, horizontal rules, and links — and themes every element
 * from [MaterialTheme] with [activeColor] used for links and accents, matching the rest of the rail.
 */
@Composable
internal fun AzMarkdown(
    markdown: String,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val codeBg = if (activeColor != Color.Unspecified) {
        activeColor.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }
    val accent = if (activeColor != Color.Unspecified) activeColor else MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        val lines = markdown.replace("\r\n", "\n").split("\n")
        var i = 0
        val paragraph = StringBuilder()

        @Composable
        fun flushParagraph() {
            if (paragraph.isNotBlank()) {
                Text(
                    text = parseInline(paragraph.toString().trim(), accent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
            }
            paragraph.setLength(0)
        }

        while (i < lines.size) {
            val raw = lines[i]
            val line = raw.trimEnd()
            when {
                // Fenced code block
                line.trimStart().startsWith("```") -> {
                    flushParagraph()
                    val code = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        code.appendLine(lines[i]); i++
                    }
                    CodeBlock(code.toString().trimEnd(), codeBg)
                    Spacer(Modifier.height(8.dp))
                }
                // Horizontal rule
                line.trim().let { it == "---" || it == "***" || it == "___" } -> {
                    flushParagraph()
                    AzDivider()
                    Spacer(Modifier.height(8.dp))
                }
                // ATX heading
                Regex("^#{1,6} ").containsMatchIn(line) -> {
                    flushParagraph()
                    val level = line.takeWhile { it == '#' }.length
                    val text = line.drop(level).trim()
                    Text(
                        text = parseInline(text, accent),
                        style = headingStyleFor(level),
                        color = if (level <= 2) accent else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                // Block quote
                line.trimStart().startsWith(">") -> {
                    flushParagraph()
                    val quote = line.trimStart().removePrefix(">").trim()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = parseInline(quote, accent),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                // List item (unordered or ordered)
                Regex("^\\s*([-*+] |\\d+\\. )").containsMatchIn(raw) -> {
                    flushParagraph()
                    val indent = raw.takeWhile { it == ' ' }.length
                    val ordered = Regex("^\\s*\\d+\\. ").find(raw)
                    val bullet = ordered?.value?.trim() ?: "•"
                    val content = raw.trimStart().replaceFirst(Regex("^([-*+] |\\d+\\. )"), "")
                    Row(modifier = Modifier.padding(start = (8 + indent * 4).dp)) {
                        Text("$bullet ", style = MaterialTheme.typography.bodyLarge, color = accent)
                        Text(
                            text = parseInline(content, accent),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
                line.isBlank() -> flushParagraph()
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line.trim())
                }
            }
            i++
        }
        flushParagraph()
    }
}

@Composable
private fun CodeBlock(code: String, background: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
            .padding(PaddingValues(12.dp))
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun headingStyleFor(level: Int) = when (level) {
    1 -> MaterialTheme.typography.headlineMedium
    2 -> MaterialTheme.typography.headlineSmall
    3 -> MaterialTheme.typography.titleLarge
    4 -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.titleSmall
}

private val INLINE = Regex(
    "`([^`]+)`" +                       // 1: code
        "|\\*\\*([^*]+)\\*\\*" +        // 2: bold
        "|__([^_]+)__" +                // 3: bold
        "|\\*([^*]+)\\*" +              // 4: italic
        "|_([^_]+)_" +                  // 5: italic
        "|\\[([^\\]]+)\\]\\(([^)]+)\\)" // 6: link text, 7: url
)

/** Parses inline markdown (code, bold, italic, links) into a themed [AnnotatedString]. */
internal fun parseInline(text: String, accent: Color): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in INLINE.findAll(text)) {
        if (m.range.first > last) append(text.substring(last, m.range.first))
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = accent)) { append(g[1]) }
            g[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[2]) }
            g[3].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[3]) }
            g[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[4]) }
            g[5].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[5]) }
            g[6].isNotEmpty() -> withLink(
                LinkAnnotation.Url(
                    g[7],
                    TextLinkStyles(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)),
                )
            ) { append(g[6]) }
        }
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}

/** [buildAnnotatedString] helper mirroring the stdlib's `withStyle` to keep call sites terse. */
private inline fun AnnotatedString.Builder.withStyle(style: SpanStyle, block: AnnotatedString.Builder.() -> Unit) {
    val idx = pushStyle(style)
    try { block() } finally { pop(idx) }
}
