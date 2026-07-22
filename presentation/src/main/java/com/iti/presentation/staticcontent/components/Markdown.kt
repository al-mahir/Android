package com.iti.presentation.staticcontent.components


internal sealed interface MarkdownBlock {
    data class Heading(val text: String, val level: Int) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}


internal fun parseMarkdown(source: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraph.toString())
            paragraph.clear()
        }
    }

    source.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        when {
            line.isEmpty() -> flushParagraph()

            line.startsWith("## ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(line.removePrefix("## ").trim(), level = 2)
            }

            line.startsWith("# ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(line.removePrefix("# ").trim(), level = 1)
            }

            line.startsWith("- ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Bullet(line.removePrefix("- ").trim())
            }

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(line)
            }
        }
    }

    flushParagraph()
    return blocks
}
