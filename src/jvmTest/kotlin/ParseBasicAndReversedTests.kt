package note.parse

import deckmarkdown.Note
import deckmarkdown.note.BasicAndReversedParser
import deckmarkdown.note.DeckParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseBasicAndReversedTests {

    private val parser =
        DeckParser(processors = listOf(BasicAndReversedParser))

    @Test
    fun `Simple and multiline BasicAndReversed is parsed correctly`() {
        val text = """
qa: AAA
aq: BBB

qa: QLine 1
QLine 2
aq: Line 1
Line 2
Line 3
        """.trimIndent()
        val expected = listOf<Note>(
            Note.BasicAndReverse(front = "AAA", back = "BBB"),
            Note.BasicAndReverse(front = "QLine 1\nQLine 2", back = "Line 1\nLine 2\nLine 3")
        )
        assertEquals(expected, parser.parseNotes(text))
    }

    @Test
    fun `Uppercase qa and aq text produce BasicAndReversed as well`() {
        val text = """
QA: AAA
AQ: BBB

qA: AAA
Aq: BBB

Qa: AAA
aQ: BBB
        """.trimIndent()
        val expected = listOf<Note>(
            Note.BasicAndReverse(front = "AAA", back = "BBB"),
            Note.BasicAndReverse(front = "AAA", back = "BBB"),
            Note.BasicAndReverse(front = "AAA", back = "BBB")
        )
        assertEquals(expected, parser.parseNotes(text))
    }

    @Test
    fun `White spaces before content are trimmed`() {
        val text = """
qa:AAA
aq:BBB

qa:
AAA
aq:
BBB

qa:     AAA
aq:          BBB

qa:
AAA
aq:     
     BBB
        """.trimIndent()
        val expected = listOf<Note>(
            Note.BasicAndReverse(front = "AAA", back = "BBB"),
            Note.BasicAndReverse(front = "AAA", back = "BBB"),
            Note.BasicAndReverse(front = "AAA", back = "BBB"),
            Note.BasicAndReverse(front = "AAA", back = "BBB")
        )
        assertEquals(expected, parser.parseNotes(text))
    }
}