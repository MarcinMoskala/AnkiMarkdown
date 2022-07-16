package write

import deckmarkdown.Note
import deckmarkdown.note.BasicAndReversedParser
import deckmarkdown.note.DeckParser
import kotlin.test.Test
import kotlin.test.assertEquals

class WriteBasicAndReversedTests {

    private val parser =
        DeckParser(processors = listOf(BasicAndReversedParser))

    @Test
    fun `Simple and multiline Basic is parsed correctly`() {
        val notes = listOf<Note>(
            Note.BasicAndReverse(front = "AAA", back = "BBB"),
            Note.BasicAndReverse(front = "QLine 1\nQLine 2", back = "Line 1\nLine 2\nLine 3")
        )
        val expected = """
qa: AAA
aq: BBB

qa: QLine 1
QLine 2
aq: Line 1
Line 2
Line 3
        """.trimIndent()
        assertEquals(expected, parser.writeNotes(notes))
    }
}