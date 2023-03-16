package toanki

import deckmarkdown.Note
import deckmarkdown.Note.ListDeletion.Item
import deckmarkdown.note.DeckParser
import deckmarkdown.note.ListDeletionParser
import kotlin.test.Test
import deckmarkdown.api.ApiNote
import kotlin.test.assertEquals

class AnkiNoteFromListTest {

    val parser = DeckParser(listOf(ListDeletionParser))

    @Test
    fun `Example ListDeletion is correctly transformed`() {
        val id = 123L
        val deckName = "DeckName"
        val comment = "Comment"
        val note = Note.ListDeletion(
            id = id,
            title = "AAA",
            extra = "General Comment",
            items = listOf(Item("a"), Item("b", "comment"))
        )
        val actual = parser.noteToApiNote(note, deckName, comment)
        val expected = ApiNote(
            noteId = id,
            deckName = deckName,
            modelName = "ListDeletion",
            fields = mapOf(
                "Title" to "AAA",
                "Extra" to "General Comment",
                "1" to "a",
                "1 comment" to "",
                "2" to "b",
                "2 comment" to "comment"
            )
        )
        assertEquals(expected, actual)
    }
}