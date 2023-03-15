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
abstract class E2ETest {
    protected val fakeApi = FakeAnkiApi()
    protected val connector = AnkiConnector(api = fakeApi)
    protected val deckName = "SOME_DECK_NAME"

    protected fun basicApi(
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

    protected fun basicAndReversedApi(
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

    protected fun listApi(
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