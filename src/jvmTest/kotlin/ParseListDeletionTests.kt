package note.parse

import deckmarkdown.Note
import deckmarkdown.Note.ListDeletion.Item
import deckmarkdown.note.DeckParser
import deckmarkdown.note.ListDeletionParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseListDeletionTests {

    private val parser = DeckParser(processors = listOf(ListDeletionParser))

    @Test
    fun `Simple List is parsed correctly`() {
        val text = """
L: First 3 letters
* a
* b
* c
        """.trimIndent()
        val expected = listOf<Note>(
            Note.ListDeletion(
                title = "First 3 letters",
                items = listOf(Item("a"), Item("b"), Item("c"))
            ),
        )
        assertEquals(expected, parser.parseNotes(text))
    }

    @Test
    fun `A lines below item is understood as a comment`() {
        val text = """
L: First 3 letters
* a: aaaaa
next
and more
* b: bbbbb
* c: ccccc
        """.trimIndent()
        val expected = listOf<Note>(
            Note.ListDeletion(
                title = "First 3 letters",
                items = listOf(Item("a", "aaaaa\nnext\nand more"), Item("b", "bbbbb"), Item("c", "ccccc"))
            )
        )
        assertEquals(expected, parser.parseNotes(text))
    }

    @Test
    fun `List can be both uppercase and lowercase`() {
        val text = """
L: First 3 letters
* a
* b
* c

l: First 3 letters
* a
* b
* c
        """.trimIndent()
        val (upper, lower) = parser.parseNotes(text)
        assertEquals(upper, lower)
    }

    @Test
    fun `White spaces before content are trimmed`() {
        val text = """
L:     First 3 letters
*     a
*              b
*c
        """.trimIndent()
        val expected = listOf<Note>(
            Note.ListDeletion(
                title = "First 3 letters",
                items = listOf(Item("a"), Item("b"), Item("c"))
            )
        )
        assertEquals(expected, parser.parseNotes(text))
    }
}