package note.markdown

import deckmarkdown.Note
import deckmarkdown.note.DefaultParser
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTests {

    private val parser = DefaultParser

    @Test
    fun `No notes produce empty`() {
        val text = parser.writeNotes(listOf())
        val expected = ""
        assertEquals(expected, text)
    }

    @Test
    fun `Cloze is stored correctly`() {
        val notes = listOf(
            Note.Cloze(1, "This is text {{c1::1}}"),
            Note.Cloze(2, "And this {{c1::text}} number is {{c2::2}}")
        )
        val text = parser.writeNotes(notes)
        val expected = """
@1
This is text {{c1::1}}

@2
And this {{c1::text}} number is {{c2::2}}
        """.trimIndent()
        assertEquals(expected, text)
    }

    @Test
    fun `Basic and BasisAndReversed are stored correctly`() {
        val notes = listOf(
            Note.Basic(1, "AAA", "BBB"),
            Note.BasicAndReverse(2, "EEE", "FFF"),
            Note.BasicAndReverse(3, "GGG", "HHH HHH HHH ;,!@#$%^&"),
            Note.Basic(4, "CCC", "DDD")
        )
        val text = parser.writeNotes(notes)
        val expected = """
@1
q: AAA
a: BBB

@2
qa: EEE
aq: FFF

@3
qa: GGG
aq: HHH HHH HHH ;,!@#${'$'}%^&

@4
q: CCC
a: DDD
        """.trimIndent()
        assertEquals(expected, text)
    }

    @Test
    fun `All are stored correctly`() {
        val notes = listOf(
            Note.Basic(1, "AAA", "BBB"),
            Note.Cloze(2, "And this {{c1::text}} number is {{c2::2}}"),
            Note.BasicAndReverse(3, "GGG", "HHH HHH")
        )
        val text = parser.writeNotes(notes)
        val expected = """
@1
q: AAA
a: BBB

@2
And this {{c1::text}} number is {{c2::2}}

@3
qa: GGG
aq: HHH HHH
        """.trimIndent()
        assertEquals(expected, text)
    }
}
