package api

import deckmarkdown.Note
import assertThrows
import kotlinx.coroutines.runBlocking
import deckmarkdown.note.DefaultParser
import kotlin.test.Test
import deckmarkdown.api.AnkiApi
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import kotlin.random.Random
import kotlin.test.assertEquals

class AnkiApiTests {

    val api = AnkiApi()
    val parser = DefaultParser

    @BeforeEach
    fun beforeMethod() = runBlocking {
        assumeTrue(api.connected())
    }

    @Test
    fun `After deck added it exists, after removed, not anymore`() = runBlocking {
        val deckName = "MyName"
        val decksBefore = api.getDecks()
        assert(deckName !in decksBefore) { "Deck with the name $deckName should not be present before this test" }

        try {
            api.createDeck(deckName)
            assertDeckAdded(deckName)
        } finally {
            api.removeDeck(deckName)
        }
        assertDeckRemoved(deckName)
    }

    @Test
    fun `After notes added they exists, after removed, not anymore`() = runBlocking {
        val deckName = "MyName"
        val notes = listOf(Note.BasicAndReverse(1, "AAA", "BBB"))
            .map { parser.noteToApiNote(it, deckName, "") }
        try {
            api.createDeck(deckName)
            assertDeckAdded(deckName)

            val addedNotes = notes.map { api.addNote(it) }
            val notesInDeck = api.getNotesInDeck(deckName)
            assertEquals(addedNotes, notesInDeck)
        } finally {
            api.removeDeck(deckName)
        }
        assertDeckRemoved(deckName)
    }

    private suspend fun assertDeckAdded(deckName: String) {
        val decksAfter = api.getDecks()
        assert(deckName in decksAfter) { "Deck with the name $deckName should be present after addition. Decks are #" }
    }

    private suspend fun assertDeckRemoved(deckName: String) {
        val decksAfterEverything = api.getDecks()
        assert(deckName !in decksAfterEverything) { "Deck with the name $deckName should not be present after this test" }
    }

    @Test // Same for incorrect names
    fun `Adding to a deck that does not exist causes error throw`() = runBlocking {
        val deckName = "MyNameKOKOKOKOKO" + Random.nextInt()
        assertThrows<Error> {
            val note = Note.Cloze(1, "AAA")
            val apiNote = parser.noteToApiNote(note, deckName, "")
            api.addNote(apiNote)
        }
    }

//    @Test
//    fun `Elements stays the same after read and write`() = runBlocking {
//
//        val listDeletionNote = Note.ListDeletion(
//            title = "AAA",
//            items = listOf(Item("a"), Item("b", "comment"))
//        )
//
//        val deckName = "MyName"
//        val notes = listOf(listDeletionNote)
//        val apiNotes = notes.map { it.toApiNote(deckName, "") }
//        try {
//            api.createDeck(deckName)
//            assertDeckAdded(deckName)
//
//            val addedNoted = apiNotes.map { api.addNote(it) }
//            val notesInDeck = api.getNotesInDeck(deckName)
//            val actual = notesInDeck.map { it.toNote() }
//            assertEquals(notes, actual) // Problem with id
//        } finally {
//            api.removeDeck(deckName)
//        }
//        assertDeckRemoved(deckName)
//    }
}