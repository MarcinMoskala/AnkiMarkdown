package write

import deckmarkdown.Note
import deckmarkdown.note.BasicParser
import deckmarkdown.note.DeckParser
import kotlin.test.Test
import kotlin.test.assertEquals

class WriteBasicTests {

    private val parser = DeckParser(processors = listOf(BasicParser))

    @Test
    fun `Simple and multiline Basic is parsed correctly`() {
        val notes = listOf<Note>(
            Note.Basic(front = "AAA", back = "BBB"),
            Note.Basic(front = "QLine 1\nQLine 2", back = "Line 1\nLine 2\nLine 3")
        )
        val expected = """
q: AAA
a: BBB

q: QLine 1
QLine 2
a: Line 1
Line 2
Line 3
        """.trimIndent()
        assertEquals(expected, parser.writeNotes(notes))
    }
}