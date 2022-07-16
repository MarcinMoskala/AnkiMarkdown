package note.parse

import deckmarkdown.Note
import assertThrows
import deckmarkdown.note.DefaultParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseTests {

    val parser = DefaultParser

    @Test
    fun `Text string with no special elements produces Text note`() {
        val texts = listOf("K", "Lorem ipsum", "A\nB")
        for (text in texts) {
            val notes = parser.parseNotes(text)
            assertEquals(listOf(Note.Text(text)), notes)
        }
    }

    @Test
    fun `IncorrectFormatException is thrown when only id and not note`() {
        assertThrows<IllegalArgumentException> { parser.parseNotes("@1") }
        assertThrows<IllegalArgumentException> { parser.parseNotes("@1\n") }
    }

    @Test
    fun `Text string with incorrect special syntax elements produces no notes`() {
        val texts = listOf("Lorem {ipsum", "Lorem }ipsum", "Lorem }{ipsum")
        for (wholeText in texts) {
            val text = wholeText.substringAfter("\n")
            val notes = parser.parseNotes(wholeText)
            assertEquals(listOf(Note.Text(text)), notes)
        }
    }

    @Test
    fun `Text string with cloze produces a cloze note`() {
        val notes = parser.parseNotes("Lorem {{c1::ipsum}} est")
        assertEquals(listOf(Note.Cloze(text = "Lorem {{c1::ipsum}} est")), notes)
    }

    @Test
    fun `We can have multiple clozes and they are separated by an empty line`() {
        val text = """
            This is text {{c1::1}}

            And this {{c1::text}} number is {{c2::2}}
        """.trimIndent()
        val notes = parser.parseNotes(text)
        assertEquals(
            listOf(
                Note.Cloze(text = "This is text {{c1::1}}"),
                Note.Cloze(text = "And this {{c1::text}} number is {{c2::2}}")
            ), notes
        )
    }

    @Test
    fun `Questions and answers works fine`() {
        val text = """
            q: My question
            a: My answer
        """.trimIndent()
        val notes = parser.parseNotes(text)
        assertEquals(listOf(Note.Basic(front = "My question", back = "My answer")), notes)
    }

    @Test
    fun `Multiline questions and answers works fine`() {
        val text = """
            q: My question
            Line 2
            a: My answer
            line 2
        """.trimIndent()
        val notes = parser.parseNotes(text)
        assertEquals(listOf(Note.Basic(front = "My question\nLine 2", back = "My answer\nline 2")), notes)
    }

    @Test
    fun `Questions and answers with reverse works fine`() {
        val text = """
            qa: My question
            aq: My answer
        """.trimIndent()
        val notes = parser.parseNotes(text)
        assertEquals(listOf(Note.BasicAndReverse(front = "My question", back = "My answer")), notes)
    }

    @Test
    fun `Both cloze and qa answers work`() {
        val text = """
            This is text {{c1::1}}

            qa: My question
            aq: My answer

            q: Question 2
            a: Answer 2

            And this {{c1::text}} number is {{c2::2}}
        """.trimIndent()
        val notes = parser.parseNotes(text)
        val expected = listOf(
            Note.Cloze(text = "This is text {{c1::1}}"),
            Note.BasicAndReverse(front = "My question", back = "My answer"),
            Note.Basic(front = "Question 2", back = "Answer 2"),
            Note.Cloze(text = "And this {{c1::text}} number is {{c2::2}}")
        )
        assertEquals(expected, notes)
    }

    @Test
    fun `Mixed ids and no ids work correctly`() {
        val text = """
            @1
            This is text {{c1::1}}

            qa: My question
            aq: My answer

            @2
            q: Question 2
            a: Answer 2

            And this {{c1::text}} number is {{c2::2}}
        """.trimIndent()
        val notes = parser.parseNotes(text)
        val expected = listOf(
            Note.Cloze(id = 1, text = "This is text {{c1::1}}"),
            Note.BasicAndReverse(front = "My question", back = "My answer"),
            Note.Basic(id = 2, front = "Question 2", back = "Answer 2"),
            Note.Cloze(text = "And this {{c1::text}} number is {{c2::2}}")
        )
        assertEquals(expected, notes)
    }
}