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
class PushingTest {
    private val fakeApi = FakeAnkiApi()
    private val connector = AnkiConnector(api = fakeApi)
    private val deckName = "SOME_DECK_NAME"

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

    private fun basicApi(
        front: String,
        back: String,
        extra: String? = null,
        noteId: Long = 0
    ) = ApiNote(
        noteId, deckName, "Basic", mapOf(
            BasicParser.FRONT_FIELD to front,
            BasicParser.BACK_FIELD to back,
            BasicParser.EXTRA_FIELD to extra.orEmpty(),
        )
    )

    private fun basicAndReversedApi(
        front: String,
        back: String,
        extra: String? = null,
        noteId: Long = 0
    ) = ApiNote(
        noteId, deckName, "Basic (and reversed card)", mapOf(
            BasicParser.FRONT_FIELD to front,
            BasicParser.BACK_FIELD to back,
            BasicParser.EXTRA_FIELD to extra.orEmpty(),
        )
    )

    private fun listApi(
        title: String,
        items: Map<String, String>,
        generalComment: String = "",
        extra: String = "",
        noteId: Long = 0
    ) = ApiNote(
        noteId, deckName, "ListDeletion", mapOf(
            "Title" to title,
            "General Comment" to generalComment,
            "Extra" to extra,
            *items.toList().flatMapIndexed { index: Int, (text, comment) ->
                val num = index + 1
                listOf(
                    "$num" to text,
                    "$num comment" to comment
                )
            }.toTypedArray()
        )
    )
}