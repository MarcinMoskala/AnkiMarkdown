package note.markdown

import deckmarkdown.Note
import deckmarkdown.note.DefaultParser
import note.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownParserTests {

    private val parser = MarkdownParser

    @Test
    fun `Bold is parsed from regex`() {
        val text = parser.markdownToAnki("this is **some text** hello")
        assertEquals("this is <b>some text</b> hello", text)
    }

    @Test
    fun `Bold is parsed from html`() {
        val text = parser.ankiToMarkdown("this is <b>some text</b> hello")
        assertEquals("this is **some text** hello", text)
    }

    @Test
    fun `Italianic is parsed from regex`() {
        val text = parser.markdownToAnki("this is *some text* hello")
        assertEquals("this is <i>some text</i> hello", text)
    }

    @Test
    fun `Italianic is parsed from html`() {
        val text = parser.ankiToMarkdown("this is <i>some text</i> hello")
        assertEquals("this is *some text* hello", text)
    }

    @Test
    fun `Image is parsed from regex`() {
        val text = parser.markdownToAnki("this is ![[an image_.img]] hello")
        assertEquals("this is <img src=\"an image_.img\" /> hello", text)
    }

    @Test
    fun `Image is parsed from html`() {
        val text = parser.ankiToMarkdown("this is <img src=\"an image_.img\" /> hello")
        assertEquals("this is ![[an image_.img]] hello", text)
    }
}
