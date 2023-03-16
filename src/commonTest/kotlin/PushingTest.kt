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
class PushingTest: E2ETest() {

    @Test
    fun rawText() = testPush(
        markdown = """
            This is a raw text
        """.trimIndent(),
        expectedNotes = listOf(
            Note.Text("This is a raw text")
        ),
        expectedApiNotes = listOf(),
        expectedMarkdown = """
            This is a raw text
        """.trimIndent()
    )

    @Test
    fun basic() = testPush(
        markdown = """
            q: Some question?
            a: Some answer
        """.trimIndent(),
        expectedNotes = listOf(
            Note.Basic(
                front = "Some question?",
                back = "Some answer"
            )
        ),
        expectedApiNotes = listOf(
            basicApi(
                front = "Some question?",
                back = "Some answer"
            )
        ),
        expectedMarkdown = """
            @0
            q: Some question?
            a: Some answer
        """.trimIndent()
    )

    @Test
    fun basicWithId() = testPush(
        markdown = """
            @987656789
            q: Some question?
            a: Some answer
        """.trimIndent(),
        expectedNotes = listOf(
            Note.Basic(
                id = 987656789,
                front = "Some question?",
                back = "Some answer"
            )
        ),
        expectedApiNotes = listOf(
            basicApi(
                noteId = 987656789,
                front = "Some question?",
                back = "Some answer"
            )
        ),
        expectedMarkdown = """
            @987656789
            q: Some question?
            a: Some answer
        """.trimIndent()
    )

    @Test
    fun basicMultiline() = testPush(
        markdown = """
            q: This
            is
            some
            question
            a: This
            is
            an
            answer
        """.trimIndent(),
        expectedNotes = listOf(
            Note.Basic(
                front = "This\nis\nsome\nquestion",
                back = "This\nis\nan\nanswer"
            )
        ),
        expectedApiNotes = listOf(
            basicApi(
                front = "This<br>\nis<br>\nsome<br>\nquestion",
                back = "This<br>\nis<br>\nan<br>\nanswer"
            )
        ),
        expectedMarkdown = """
            @0
            q: This
            is
            some
            question
            a: This
            is
            an
            answer
        """.trimIndent()
    )

    @Test
    fun basicWithExtra() = testPush(
        markdown = """
            q: This is some question
            a: This is an answer
            e: This is an extra
            multiline
        """.trimIndent(),
        expectedNotes = listOf(
            Note.Basic(
                front = "This is some question",
                back = "This is an answer",
                extra = "This is an extra\nmultiline"
            )
        ),
        expectedApiNotes = listOf(
            basicApi(
                front = "This is some question",
                back = "This is an answer",
                extra = "This is an extra<br>\nmultiline"
            )
        ),
        expectedMarkdown = """
            @0
            q: This is some question
            a: This is an answer
            e: This is an extra
            multiline
        """.trimIndent()
    )

    @Test
    fun basicAndReversedWithId() = testPush(
        markdown = """
            @12345
            qa: Some question?
            aq: Some answer
        """.trimIndent(),
        expectedNotes = listOf(
            Note.BasicAndReverse(
                id = 12345,
                front = "Some question?",
                back = "Some answer"
            )
        ),
        expectedApiNotes = listOf(
            basicAndReversedApi(
                noteId = 12345,
                front = "Some question?",
                back = "Some answer"
            )
        ),
        expectedMarkdown = """
            @12345
            qa: Some question?
            aq: Some answer
        """.trimIndent()
    )

    @Test
    fun basicAndReversed() = testPush(
        markdown = """
            qa: Some question?
            aq: Some answer
        """.trimIndent(),
        expectedNotes = listOf(
            Note.BasicAndReverse(
                front = "Some question?",
                back = "Some answer"
            )
        ),
        expectedApiNotes = listOf(
            basicAndReversedApi(
                front = "Some question?",
                back = "Some answer"
            )
        ),
        expectedMarkdown = """
            @0
            qa: Some question?
            aq: Some answer
        """.trimIndent()
    )

    @Test
    fun basicAndReversedMultiline() = testPush(
        markdown = """
            qa: This
            is
            some
            question
            aq: This
            is
            an
            answer
        """.trimIndent(),
        expectedNotes = listOf(
            Note.BasicAndReverse(
                front = "This\nis\nsome\nquestion",
                back = "This\nis\nan\nanswer"
            )
        ),
        expectedApiNotes = listOf(
            basicAndReversedApi(
                front = "This<br>\nis<br>\nsome<br>\nquestion",
                back = "This<br>\nis<br>\nan<br>\nanswer"
            )
        ),
        expectedMarkdown = """
            @0
            qa: This
            is
            some
            question
            aq: This
            is
            an
            answer
        """.trimIndent()
    )

    @Test
    fun basicAndReversedWithExtra() = testPush(
        markdown = """
            qa: This is some question
            aq: This is 
            an answer
            e: This is an extra
            multiline
        """.trimIndent(),
        expectedNotes = listOf(
            Note.BasicAndReverse(
                front = "This is some question",
                back = "This is \nan answer",
                extra = "This is an extra\nmultiline"
            )
        ),
        expectedApiNotes = listOf(
            basicAndReversedApi(
                front = "This is some question",
                back = "This is <br>\nan answer",
                extra = "This is an extra<br>\nmultiline"
            )
        ),
        expectedMarkdown = """
            @0
            qa: This is some question
            aq: This is 
            an answer
            e: This is an extra
            multiline
        """.trimIndent()
    )

    @Test
    fun list() = testPush(
        markdown = """
            L: What is the alphabet?
            * A
            * B
            * C
        """.trimIndent(),
        expectedNotes = listOf(
            Note.ListDeletion(
                type = Note.ListDeletion.ListType.List,
                title = "What is the alphabet?",
                items = listOf(
                    Note.ListDeletion.Item("A"),
                    Note.ListDeletion.Item("B"),
                    Note.ListDeletion.Item("C"),
                )
            )
        ),
        expectedApiNotes = listOf(
            listApi(
                title = "What is the alphabet?",
                items = mapOf(
                    "A" to "",
                    "B" to "",
                    "C" to "",
                )
            )
        ),
        expectedMarkdown = """
            @0
            L: What is the alphabet?
            * A
            * B
            * C
        """.trimIndent()
    )

    @Test
    fun setMultilineCommentsExtra() = testPush(
        markdown = """
            S: What are HTTP methods?
            * GET: The GET method requests a representation of the specified resource. 
            Requests using GET should only retrieve data.
            * HEAD: The HEAD method asks for a response identical to a GET request, 
            but without the response body.
            * POST: The POST method submits an entity to the specified resource, 
            often causing a change in state or side effects on the server.
            * DELETE
            * and others
            Extra: Those are not all the methods, there are also...
        """.trimIndent(),
//        expectedNotes = listOf(
//            Note.ListDeletion(
//                type = Note.ListDeletion.ListType.Set,
//                title = "What is the alphabet?",
//                items = listOf(
//                    Note.ListDeletion.Item("A"),
//                    Note.ListDeletion.Item("B"),
//                    Note.ListDeletion.Item("C"),
//                )
//            )
//        ),
        expectedApiNotes = listOf(
            setApi(
                title = "What are HTTP methods?",
                items = mapOf(
                    "GET" to "The GET method requests a representation of the specified resource. \nRequests using GET should only retrieve data.",
                    "HEAD" to "The HEAD method asks for a response identical to a GET request, \nbut without the response body.",
                    "POST" to "The POST method submits an entity to the specified resource, \noften causing a change in state or side effects on the server.",
                    "DELETE" to "",
                    "and others" to "",
                ),
                extra = "Those are not all the methods, there are also..."
            )
        ),
        expectedMarkdown = """
            @0
            S: What are HTTP methods?
            * GET - The GET method requests a representation of the specified resource. 
            Requests using GET should only retrieve data.
            * HEAD - The HEAD method asks for a response identical to a GET request, 
            but without the response body.
            * POST - The POST method submits an entity to the specified resource, 
            often causing a change in state or side effects on the server.
            * DELETE
            * and others
            Extra: Those are not all the methods, there are also...
        """.trimIndent()
    )

    @Test
    fun general() = testPush(
        markdown = """
            modelName: Special model name (for test)
            field_a: This is text a
            field_b: This is text b
        """.trimIndent(),
        expectedNotes = listOf(
            Note.General(
                modelName = "Special model name (for test)",
                fields = mapOf(
                    "field_a" to "This is text a",
                    "field_b" to "This is text b",
                )
            )
        ),
        expectedApiNotes = listOf(
            ApiNote(
                deckName = deckName,
                modelName = "Special model name (for test)",
                fields = mapOf(
                    "field_a" to "This is text a",
                    "field_b" to "This is text b",
                )
            )
        ),
        expectedMarkdown = """
            @0
            modelName: Special model name (for test)
            field_a: This is text a
            field_b: This is text b
        """.trimIndent()
    )

    @Test
    fun generalMultiline() = testPush(
        markdown = """
            modelName: Special 
            model name (for test)
            field_a: This is 
            text a
            field_b: This is 
            text b
        """.trimIndent(),
        expectedNotes = listOf(
            Note.General(
                modelName = "Special \nmodel name (for test)",
                fields = mapOf(
                    "field_a" to "This is \ntext a",
                    "field_b" to "This is \ntext b",
                )
            )
        ),
        expectedApiNotes = listOf(
            ApiNote(
                deckName = deckName,
                modelName = "Special \nmodel name (for test)",
                fields = mapOf(
                    "field_a" to "This is \ntext a",
                    "field_b" to "This is \ntext b",
                )
            )
        ),
        expectedMarkdown = """
            @0
            modelName: Special 
            model name (for test)
            field_a: This is 
            text a
            field_b: This is 
            text b
        """.trimIndent()
    )

    @Test
    fun generalText() = testPush(
        markdown = """
            modelName: ABC
            f1: DEF
            f2: GHI
            
            JKL MNO
            PRS TUW
            
            modelName: Special model name (for test)
            field_a: This is text a
            field_b: This is text b
        """.trimIndent(),
        expectedNotes = listOf(
            Note.General(
                modelName = "ABC",
                fields = mapOf(
                    "f1" to "DEF",
                    "f2" to "GHI",
                )
            ),
            Note.Text("JKL MNO\nPRS TUW"),
            Note.General(
                modelName = "Special model name (for test)",
                fields = mapOf(
                    "field_a" to "This is text a",
                    "field_b" to "This is text b",
                )
            )
        ),
        expectedApiNotes = listOf(
            ApiNote(
                deckName = deckName,
                modelName = "ABC",
                fields = mapOf(
                    "f1" to "DEF",
                    "f2" to "GHI",
                )
            ),
            ApiNote(
                deckName = deckName,
                modelName = "Special model name (for test)",
                fields = mapOf(
                    "field_a" to "This is text a",
                    "field_b" to "This is text b",
                )
            ),
        ),
        expectedMarkdown = """
            @0
            modelName: ABC
            f1: DEF
            f2: GHI
            
            JKL MNO
            PRS TUW
            
            @1
            modelName: Special model name (for test)
            field_a: This is text a
            field_b: This is text b
        """.trimIndent()
    )

    private fun testPush(
        markdown: String,
        expectedNotes: List<Note>? = null,
        expectedApiNotes: List<ApiNote>? = null,
        expectedMarkdown: String? = null,
    ) = runTest {
        if (expectedNotes != null) {
            assertEquals(expectedNotes, DefaultParser.parseNotes(markdown))
        }

        val res = connector.pushDeck(deckName, markdown)
        if (expectedApiNotes != null) {
            assertEquals(expectedApiNotes, fakeApi.notes)
        }
        if (expectedMarkdown != null) {
            assertEquals(expectedMarkdown, res.markdown)
        }
        fakeApi.clean()
    }
}