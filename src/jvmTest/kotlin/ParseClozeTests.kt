package note.parse

import deckmarkdown.Note
import deckmarkdown.note.ClozeParser
import deckmarkdown.note.DeckParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseClozeTests {

    private val parser = DeckParser(processors = listOf(ClozeParser))

    @Test
    fun `Simple and multiline Cloze is parsed correctly`() {
        val text = """
Lorem {{c1::ipsum}} dolor sit amet

Lorem {{c1::ipsum}}
dolor sit amet
        """.trimIndent()
        val expected = listOf<Note>(
            Note.Cloze(text = "Lorem {{c1::ipsum}} dolor sit amet"),
            Note.Cloze(text = "Lorem {{c1::ipsum}}\ndolor sit amet")
        )
        assertEquals(expected, parser.parseNotes(text))
    }

    @Test
    fun `Both standard and shortened format for cloze produce the same result`() {
        val text = """
Lorem {{ipsum}} dolor sit {{amet}}

Lorem {{c1::ipsum}} dolor sit {{c2::amet}}
        """.trimIndent()
        val (first, second) = parser.parseNotes(text)
        assertEquals(second, first)
    }
}