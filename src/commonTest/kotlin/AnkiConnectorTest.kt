package ankimarkdown

import ankimarkdown.fakes.FakeAnkiApi
import deckmarkdown.AnkiConnector
import deckmarkdown.api.ApiNote
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnkiConnectorTest {

    private val fakeApi = FakeAnkiApi()
    private val connect = AnkiConnector(api = fakeApi)

    @AfterTest
    fun cleanup() {
        fakeApi.clean()
    }

    @Test
    fun simpleTest() = runTest {
        fakeApi.hasNotes()

        // when
        val content = """
            q: This is a question
            a: This is an answer!

            This is a {{cloze}}
        """.trimIndent()
        val result = connect.pushDeck("A_Deck", content)

        // then
        assertEquals(2, result.addedCount)
        assertEquals(0, result.removedCount)
        assertEquals(0, result.updatedCount)
        assertEquals(0, result.unchangedCount)
        val expectedMarkdown = """
            
            ***
            
            @0
            q: This is a question
            a: This is an answer!
            
            @1
            This is a {{c1::cloze}}""".trimIndent()
        assertEquals(expectedMarkdown, result.updatedMarkdown)
        assertEquals(
            listOf(
                ApiNote(noteId = 0, deckName = "A_Deck", modelName = "Basic", fields = mapOf("Front" to "This is a question", "Back" to "This is an answer!", "Extra" to "")),
                ApiNote(noteId = 1, deckName = "A_Deck", modelName = "Cloze", fields = mapOf("Text" to "This is a {{c1::cloze}}", "Extra" to ""))
            ), fakeApi.notes
        )
    }
}