package ankimarkdown

import ankimarkdown.fakes.FakeAnkiApi
import deckmarkdown.AnkiConnector
import deckmarkdown.AnkiConnectorResult
import deckmarkdown.Note
import deckmarkdown.api.ApiNote
import deckmarkdown.note.BasicParser
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
    fun shouldPushDeck() = runTest {
        // given
        val content = """
            q: This is a question
            a: This is an answer!

            This is a {{cloze}}
        """.trimIndent()

        // when
        val result = connect.pushDeck("A_Deck", content)

        // then
        assertEquals(2, result.ankiModificationsCounts?.addedCount)
        assertEquals(0, result.ankiModificationsCounts?.removedCount)
        assertEquals(0, result.ankiModificationsCounts?.updatedCount)
        assertEquals(0, result.ankiModificationsCounts?.unchangedCount)
        val expectedMarkdown = """            
            @0
            q: This is a question
            a: This is an answer!
            
            @1
            This is a {{c1::cloze}}""".trimIndent()
        assertEquals(expectedMarkdown, result.markdown)
        assertEquals(
            listOf(
                ApiNote(noteId = 0, deckName = "A_Deck", modelName = "Basic", fields = mapOf("Front" to "This is a question", "Back" to "This is an answer!", "Extra" to "")),
                ApiNote(noteId = 1, deckName = "A_Deck", modelName = "Cloze", fields = mapOf("Text" to "This is a {{c1::cloze}}", "Extra" to ""))
            ), fakeApi.notes
        )
    }

    @Test
    fun shouldPullDeckToExisting() = runTest {
        // given
        fakeApi.hasNotes(
            ApiNote.basic(432, "A_Deck", "AAA", "BBB"),
            ApiNote.cloze(654, "A_Deck", "This is second {{c1::cloze}}"),
        )
        val content = """
            Some text
            
            q: This is a question
            a: This is an answer!
            
            @987
            This is first {{c1::cloze}}
            
            Some text 2
            
            @654
            This is a {{c1::cloze}}
        """.trimIndent()

        // when
        val result = connect.pullDeck("A_Deck", content)

        // then
        assertEquals(null, result.ankiModificationsCounts)
        val expectedMarkdown = """            
            Some text
            
            q: This is a question
            a: This is an answer!
            
            Some text 2
            
            @654
            This is second {{c1::cloze}}
            
            @432
            q: AAA
            a: BBB
        """.trimIndent()
        assertEquals(expectedMarkdown, result.markdown)
    }

    @Test
    fun shouldPullDeck() = runTest {
        // given
        val notes = listOf(
            ApiNote.basic(id = 1, deckName = "A_Deck", front = "A", back = "B"),
            ApiNote.basic(id = 2, deckName = "A_Deck", front = "C", back = "D"),
            ApiNote.basic(id = 3, deckName = "B_Deck", front = "E", back = "E"),
        )
        fakeApi.hasNotes(*notes.toTypedArray())

        // when
        val result = connect.pullDeck("A_Deck")

        // then
        assertEquals(null, result.ankiModificationsCounts)
        val expectedMarkdown = """            
            @1
            q: A
            a: B
            
            @2
            q: C
            a: D
        """.trimIndent()
        assertEquals(expectedMarkdown, result.markdown)
        assertEquals(notes, fakeApi.notes)
    }

    @Test
    fun shouldPushFile() = runTest {
        // given
        val content = """
            ---
            deckName: "New name ---"
            ---
            
            q: This is a question
            a: This is an answer!

            This is a {{cloze}}
        """.trimIndent()

        // when
        val result = connect.pushFile(content)

        // then
        assertEquals(AnkiConnectorResult.ModificationsCounts(addedCount = 2), result.ankiModificationsCounts)
        val expectedMarkdown = """
            ---
            deckName: "New name ---"
            ---
            
            @0
            q: This is a question
            a: This is an answer!
            
            @1
            This is a {{c1::cloze}}""".trimIndent()
        assertEquals(expectedMarkdown, result.markdown)
        assertEquals(
            listOf(
                ApiNote(noteId = 0, deckName = "New name ---", modelName = "Basic", fields = mapOf("Front" to "This is a question", "Back" to "This is an answer!", "Extra" to "")),
                ApiNote(noteId = 1, deckName = "New name ---", modelName = "Cloze", fields = mapOf("Text" to "This is a {{c1::cloze}}", "Extra" to ""))
            ), fakeApi.notes
        )
    }

    @Test
    fun shouldCreateFile() = runTest {
        // given
        val notes = listOf(
            ApiNote.basic(id = 1, deckName = "A_Deck", front = "A", back = "B"),
            ApiNote.basic(id = 2, deckName = "A_Deck", front = "C", back = "D"),
            ApiNote.basic(id = 3, deckName = "B_Deck", front = "E", back = "E"),
        )
        fakeApi.hasNotes(*notes.toTypedArray())

        // when
        val result = connect.createFile("A_Deck")

        // then
        assertEquals(null, result.ankiModificationsCounts)
        val expectedMarkdown = """   
            ---
            deckName: "A_Deck"
            ---

            @1
            q: A
            a: B
            
            @2
            q: C
            a: D""".trimIndent()
        assertEquals(expectedMarkdown, result.markdown)
        assertEquals(notes, fakeApi.notes)
    }

    @Test
    fun pullPushAndPullAreConsistent() = runTest {
        // given
        val notes = listOf(
            ApiNote.basic(id = 1, deckName = "A_Deck", front = "A", back = "B"),
            ApiNote.basic(id = 2, deckName = "A_Deck", front = "C", back = "D"),
            ApiNote.basic(id = 3, deckName = "B_Deck", front = "E", back = "E"),
        )
        fakeApi.hasNotes(*notes.toTypedArray())

        // when
        val result1 = connect.pullDeck("A_Deck")
        val result2 = connect.pushDeck("A_Deck", result1.markdown)
        val result3 = connect.pullDeck("A_Deck", result2.markdown)

        // then
        assertEquals(result1.markdown, result2.markdown)
        assertEquals(result2.markdown, result3.markdown)
    }

    @Test
    fun pushAndPullDoNotLooseData() = runTest {
        // given
        val content = """
            ---
            deckName: "A_Deck"
            ---
            
            @123
            q: This is a question
            a: This is an answer!

            @456
            This is a {{c1::cloze}}
        """.trimIndent()

        // when
        val result1 = connect.pushFile(content)
        val result2 = connect.createFile("A_Deck")

        // then
        assertEquals(result1.markdown, result2.markdown)
    }

    @Test
    fun pushAndPullToExistingDoNotLooseData() = runTest {
        // given
        val content = """
            ---
            deckName: A_Deck
            ---
            
            @123
            q: This is a question
            a: This is an answer!

            @456
            This is a {{c1::cloze}}
        """.trimIndent()

        // when
        val result1 = connect.pushFile(content)
        val result2 = connect.pullDeck("A_Deck", content)

        // then
        assertEquals(result1.markdown, result2.markdown)
    }

    @Test
    fun shouldTransformMarkdownWhenPushingFile() = runTest {
        val noteContent = """
            ---
            deckName: A_Deck
            ---
            
            @1676064119969
            q: What is the difference between **concurrent** and **paralell** process?
            a: Concurrent **might** mean that there is only one thread that executes multiple processes interchangably. Paralell **must** be using multiple threads and CPU cores, to effectively execute two tasks at the same time. 
            ![[Pasted image 20230119100105.png]]
            ![[Pasted image 20230119100201.png]]

            @1676064119993
            q: What do we call processes that are each executed internchangably by a single thread?
            a: **Concurrent**.

            @1676064120018
            q: What do we call processes that are each executed by different thread, on different CPU cores?
            a: **Pralalell** (more precisely) or **concurrent**.
        """.trimIndent()

        // when
        connect.pushFile(noteContent)

        // then
        val expected = listOf(
            ApiNote(noteId=1676064119969, deckName="A_Deck", modelName="Basic", fields= mapOf(
                "Front" to "What is the difference between <b>concurrent<i>* and *</i>paralell</b> process?",
                "Back" to "Concurrent <b>might<i>* mean that there is only one thread that executes multiple processes interchangably. Paralell *</i>must</b> be using multiple threads and CPU cores, to effectively execute two tasks at the same time. <br>\n<img src=\"Pasted image 20230119100105.png\" /><br>\n<img src=\"Pasted image 20230119100201.png\" />",
                "Extra" to ""
            )),
            ApiNote(noteId=1676064119993, deckName="A_Deck", modelName="Basic", fields=mapOf(
                "Front" to "What do we call processes that are each executed internchangably by a single thread?",
                "Back" to "<b>Concurrent</b>.",
                "Extra" to ""
            )),
            ApiNote(noteId=1676064120018, deckName="A_Deck", modelName="Basic", fields=mapOf(
                "Front" to "What do we call processes that are each executed by different thread, on different CPU cores?",
                "Back" to "<b>Pralalell<i>* (more precisely) or *</i>concurrent</b>.",
                "Extra" to ""
            ))
        )
        assertEquals(expected, fakeApi.notes)
    }
}