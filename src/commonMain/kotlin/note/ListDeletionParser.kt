package deckmarkdown.note

import deckmarkdown.Note
import deckmarkdown.Note.ListDeletion
import deckmarkdown.Note.ListDeletion.ListType
import deckmarkdown.api.ApiNote
import deckmarkdown.recognizeKeyValueLines

object ListDeletionParser : FullNoteProcessor<ListDeletion> {
    private val LIST_QUESTION_START_REGEX = "^([LlSs]):([^\\n]+)\\n([^*]*)\\*".toRegex()
    private val LIST_ITEM_REGEX = "\\*\\s*([^\\n:]+)((: (([^\\n]*)(\\n([^*]*))))?)".toRegex()

    private val API_NOTE_TO_TYPE = mapOf(
        "ListDeletion" to ListType.List,
        "SetDeletion" to ListType.Set
    )

    override fun handlesNote(note: Note): Boolean = note is ListDeletion

    override fun recognize(text: String): Boolean = LIST_QUESTION_START_REGEX in text

    override fun parse(id: Long?, noteText: String): ListDeletion {
        val map = noteText.recognizeKeyValueLines() ?: error("There must be a key, at least S: or L:")
        val (prefix, titleAndItemsText) = map.toList().first()
        val title = titleAndItemsText.substringBefore("\n").trim()
        val points = titleAndItemsText.substringAfter("\n")

        val listType = when (prefix.lowercase()) {
            "s" -> ListType.Set
            else -> ListType.List
        }

        val items = LIST_ITEM_REGEX.findAll(points + "\n")
            .map {
                val value = it.groupValues[1].trim().trimEnd()
                val comment = it.groupValues[4].trim().trimEnd()
                ListDeletion.Item(value, comment)
            }
            .toList()

        return ListDeletion(
            id = id,
            type = listType,
            title = title,
            items = items,
            extra = map["Extra"] ?: map["extra"] ?: ""
        )
    }

    override fun render(note: ListDeletion): String = "${if (note.type == ListType.List) "L" else "S"}: {question}\n"
        .replace("{question}", note.title)
        .plus(note.items.joinToString(separator = "\n") {
            if (it.comment.isBlank()) "* ${it.value}" else "* ${it.value}: ${it.comment}"
        })
        .plus(if (note.extra.isNotBlank()) "\nExtra: ${note.extra}" else "")

    override fun recognizeApiNote(apiNote: ApiNote): Boolean = apiNote.modelName in API_NOTE_TO_TYPE.keys

    override fun cardToAnkiNote(note: ListDeletion, deckName: String, comment: String): ApiNote = ApiNote(
        noteId = note.id ?: ApiNote.NO_ID,
        deckName = deckName,
        modelName = API_NOTE_TO_TYPE.reverseLookup(note.type),
        fields = ApiNote.fieldsOf(
            "Title" to note.title,
            "Extra" to note.extra
        ) + note.items
            .withIndex()
            .flatMap { (index, item) ->
                val positionStr = "${index + 1}"
                listOf(
                    positionStr to item.value,
                    "$positionStr comment" to item.comment
                )
            }
            .toMap()
    )

    override fun ankiNoteToCard(apiNote: ApiNote): ListDeletion = ListDeletion(
        id = apiNote.noteId,
        type = API_NOTE_TO_TYPE[apiNote.modelName] ?: error("Unsupported model name " + apiNote.modelName),
        title = apiNote.field("Title"),
        items = (1..20).mapNotNull { i ->
            val value = apiNote.fieldOrNull("$i").takeUnless<String?> { it.isNullOrBlank() } ?: return@mapNotNull null
            val comment = apiNote.fieldOrNull("${i} comment").orEmpty()
            ListDeletion.Item(
                value,
                comment
            )
        },
        extra = apiNote.field("Extra")
    )

    override fun toHtml(note: ListDeletion): String = "TODO"

    override fun toMarkdown(note: ListDeletion): String = "TODO"

}

private fun <K, V> Map<K, V>.reverseLookup(value: V) =
    toList().first { it.second == value }.first

