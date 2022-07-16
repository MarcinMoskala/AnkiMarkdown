package note.html

import deckmarkdown.Note
import deckmarkdown.note.DefaultParser
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlWriteTests {

    val parser = DefaultParser

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
        val text = parser.htmlWriteNotes(notes)
        val expected = """
<div>This is text <b>1</b></div>
<div>And this <b>text</b> number is <b>2</b></div>
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
        val text = parser.htmlWriteNotes(notes)
        val expected = """
<div><i>Q:</i> AAA<br><i>A:</i> BBB</div>
<div><i>Q/A:</i> EEE<br><i>A/Q:</i> FFF</div>
<div><i>Q/A:</i> GGG<br><i>A/Q:</i> HHH HHH HHH ;,!@#${'$'}%^&</div>
<div><i>Q:</i> CCC<br><i>A:</i> DDD</div>
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
        val text = parser.htmlWriteNotes(notes)
        val expected = """
<div><i>Q:</i> AAA<br><i>A:</i> BBB</div>
<div>And this <b>text</b> number is <b>2</b></div>
<div><i>Q/A:</i> GGG<br><i>A/Q:</i> HHH HHH</div>
        """.trimIndent()
        assertEquals(expected, text)
    }
}
