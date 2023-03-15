package ankimarkdown

import ankimarkdown.fakes.FakeAnkiApi
import deckmarkdown.AnkiConnector
import deckmarkdown.Note
import deckmarkdown.api.ApiNote
import deckmarkdown.note.BasicParser
import deckmarkdown.note.DefaultParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PullingTest: E2ETest() {

    @Test
    fun general() = testPull(
        startingMarkdown = """
            Some text 1
            
            Some text 2
        """.trimIndent(),
        notesOnDeck = listOf(
            ApiNote(
                noteId = 123,
                deckName = deckName,
                modelName = "Special model name (for test)",
                fields = mapOf(
                    "field_a" to "This is text a",
                    "field_b" to "This is text b",
                )
            )
        ),
        expectedMarkdown = """
            Some text 1
            
            Some text 2
            
            @123
            modelName: Special model name (for test)
            field_a: This is text a
            field_b: This is text b
        """.trimIndent(),
    )

    private fun testPull(
        startingMarkdown: String = "",
        notesOnDeck: List<ApiNote>,
        expectedMarkdown: String? = null,
    ) = runTest {
        fakeApi.hasNotes(notesOnDeck)
        val res = connector.pullDeck(deckName, startingMarkdown)
        if (expectedMarkdown != null) {
            assertEquals(expectedMarkdown, res.markdown)
        }
        fakeApi.clean()
    }
}