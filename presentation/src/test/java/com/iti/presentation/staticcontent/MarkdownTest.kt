package com.iti.presentation.staticcontent

import com.iti.presentation.staticcontent.components.MarkdownBlock
import com.iti.presentation.staticcontent.components.parseMarkdown
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {

    @Test
    fun `heading levels are distinguished`() {
        val blocks = parseMarkdown("# Title\n\n## Section")

        assertEquals(
            listOf(
                MarkdownBlock.Heading("Title", level = 1),
                MarkdownBlock.Heading("Section", level = 2),
            ),
            blocks,
        )
    }

    @Test
    fun `hard-wrapped lines are folded into a single paragraph`() {
        val blocks = parseMarkdown("first line\nsecond line\n\nnext paragraph")

        assertEquals(
            listOf(
                MarkdownBlock.Paragraph("first line second line"),
                MarkdownBlock.Paragraph("next paragraph"),
            ),
            blocks,
        )
    }

    @Test
    fun `bullets break the surrounding paragraph`() {
        val blocks = parseMarkdown("intro\n- one\n- two\noutro")

        assertEquals(
            listOf(
                MarkdownBlock.Paragraph("intro"),
                MarkdownBlock.Bullet("one"),
                MarkdownBlock.Bullet("two"),
                MarkdownBlock.Paragraph("outro"),
            ),
            blocks,
        )
    }

    @Test
    fun `unrecognised markup degrades to text instead of vanishing`() {
        val blocks = parseMarkdown("**bold** and [link](x)")

        assertEquals(listOf(MarkdownBlock.Paragraph("**bold** and [link](x)")), blocks)
    }

    @Test
    fun `an empty document produces no blocks`() {
        assertEquals(emptyList<MarkdownBlock>(), parseMarkdown("\n\n   \n"))
    }

    @Test
    fun `arabic content parses the same as latin`() {
        val blocks = parseMarkdown("# سياسة الخصوصية\n\n- بيانات الحساب\n\nنص تمهيدي")

        assertEquals(
            listOf(
                MarkdownBlock.Heading("سياسة الخصوصية", level = 1),
                MarkdownBlock.Bullet("بيانات الحساب"),
                MarkdownBlock.Paragraph("نص تمهيدي"),
            ),
            blocks,
        )
    }
}
