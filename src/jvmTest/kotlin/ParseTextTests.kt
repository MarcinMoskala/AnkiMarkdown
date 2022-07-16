package note.parse

import deckmarkdown.Note
import deckmarkdown.note.DeckParser
import deckmarkdown.note.TextParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseTextTests {

    val parser = DeckParser(listOf(TextParser))

    @Test
    fun `Pure text with no markup is a text`() {
        val text = """
AAA BBB

Line 1
Line 2
Line 3

Lorem ipsum
        """.trimIndent()
        val expected = listOf<Note>(
            Note.Text(text = "AAA BBB"),
            Note.Text(text = "Line 1\nLine 2\nLine 3"),
            Note.Text(text = "Lorem ipsum")
        )
        assertEquals(expected, parser.parseNotes(text))
    }
}