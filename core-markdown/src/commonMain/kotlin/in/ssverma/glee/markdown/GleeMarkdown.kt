package `in`.ssverma.glee.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Immutable
sealed interface MarkdownBlock {
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String?) : MarkdownBlock
    data class Heading(val text: String, val level: Int) : MarkdownBlock
    data class ListItem(val text: AnnotatedString, val level: Int) : MarkdownBlock
}

@Composable
fun GleeMarkdown(
    content: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { parseMarkdown(content) }
    
    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> HeadingBlock(block)
                is MarkdownBlock.Paragraph -> ParagraphBlock(block)
                is MarkdownBlock.CodeBlock -> CodeBlock(block)
                is MarkdownBlock.ListItem -> ListItemBlock(block)
            }
            if (index < blocks.lastIndex) {
                val spacerHeight = if (block is MarkdownBlock.ListItem && blocks.getOrNull(index + 1) is MarkdownBlock.ListItem) {
                    4.dp
                } else {
                    12.dp
                }
                Spacer(Modifier.height(spacerHeight))
            }
        }
    }
}

@Composable
private fun HeadingBlock(block: MarkdownBlock.Heading) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.titleLarge
    }
    Text(
        text = block.text,
        style = style,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ParagraphBlock(block: MarkdownBlock.Paragraph) {
    Text(
        text = block.text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ListItemBlock(block: MarkdownBlock.ListItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = (block.level * 16).dp)) {
        Text(text = "•", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        Text(
            text = block.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CodeBlock(block: MarkdownBlock.CodeBlock) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        if (!block.language.isNullOrBlank()) {
            Text(
                text = block.language.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Text(
            text = block.code,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A simple markdown parser that handles basic blocks.
 */
private fun parseMarkdown(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = content.lines()
    var i = 0
    
    while (i < lines.size) {
        val line = lines[i]
        
        when {
            line.startsWith("```") -> {
                val language = line.removePrefix("```").trim()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    code.append(lines[i]).append("\n")
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(code.toString().trimEnd(), language.ifEmpty { null }))
                i++
            }
            line.trimStart().startsWith("#") -> {
                val trimmed = line.trimStart()
                val level = trimmed.takeWhile { it == '#' }.length
                val text = trimmed.removePrefix("#".repeat(level)).trim()
                blocks.add(MarkdownBlock.Heading(text, level))
                i++
            }
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                val trimmed = line.trimStart()
                val level = (line.length - trimmed.length) / 2
                val text = trimmed.substring(2).trim()
                blocks.add(MarkdownBlock.ListItem(parseInlineMarkdown(text), level))
                i++
            }
            line.isBlank() -> {
                i++
            }
            else -> {
                val paragraph = StringBuilder()
                while (i < lines.size && 
                    lines[i].isNotBlank() && 
                    !lines[i].trimStart().startsWith("#") && 
                    !lines[i].startsWith("```") &&
                    !lines[i].trimStart().startsWith("- ") &&
                    !lines[i].trimStart().startsWith("* ")
                ) {
                    paragraph.append(lines[i]).append("\n")
                    i++
                }
                val text = paragraph.toString().trim()
                if (text.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(parseInlineMarkdown(text)))
                }
            }
        }
    }
    
    return blocks
}

/**
 * Parses inline markdown like **bold**, *italic*, and `code`.
 */
private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append("*")
                        i += 1
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace, 
                            background = Color.Gray.copy(alpha = 0.15f),
                            color = Color(0xFFE91E63) // Pinkish color for inline code
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append("`")
                        i += 1
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
