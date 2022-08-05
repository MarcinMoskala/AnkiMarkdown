package deckmarkdown.note

import deckmarkdown.Note
import deckmarkdown.Note.ListDeletion
import deckmarkdown.Note.ListDeletion.ListType
import deckmarkdown.api.ApiNote

object ListDeletionParser : FullNoteProcessor<ListDeletion> {
    private val LIST_QUESTION_REGEX = "^([LlSs]):([^\\n]+)\\n([^*]*)\\*".toRegex()
    private val LIST_ITEM_REGEX = "\\*\\s*([^\\n]*)(\\n([^*]*))?".toRegex()

    private val API_NOTE_TO_TYPE = mapOf(
        "ListDeletion" to ListType.List,
        "SetDeletion" to ListType.Set
    )

    override fun handlesNote(note: Note): Boolean = note is ListDeletion

    override fun recognize(text: String): Boolean = LIST_QUESTION_REGEX in text

    override fun parse(id: Long?, noteText: String): ListDeletion {
        val parsedStart = checkNotNull(LIST_QUESTION_REGEX.find(noteText))
        val prefix = parsedStart.groupValues[1]
        val question = parsedStart.groupValues[2].trim().trimEnd()
        val generalComment = parsedStart.groupValues[3].trim().trimEnd()

        val listType = when (prefix.toLowerCase()) {
            "s" -> ListType.Set
            else -> ListType.List
        }

        val items = LIST_ITEM_REGEX.findAll(noteText)
            .map {
                val value = it.groupValues[1].trim().trimEnd()
                val comment = it.groupValues[3].trim().trimEnd()
                ListDeletion.Item(value, comment)
            }
            .toList()

        return ListDeletion(id, listType, question, items, generalComment)
    }

    override fun render(note: ListDeletion): String = "${if (note.type == ListType.List) "L" else "S"}: {question}\n"
        .replace("{question}", note.title)
        .plus(if (note.generalComment.isNotBlank()) "${note.generalComment}\n" else "")
        .plus(note.items.joinToString(separator = "\n") {
            if (it.comment.isBlank()) "* ${it.value}" else "* ${it.value}\n${it.comment}"
        })

    override fun recognizeApiNote(apiNote: ApiNote): Boolean = apiNote.modelName in API_NOTE_TO_TYPE.keys

    override fun cardToAnkiNote(note: ListDeletion, deckName: String, comment: String): ApiNote = ApiNote(
        noteId = note.id ?: ApiNote.NO_ID,
        deckName = deckName,
        modelName = API_NOTE_TO_TYPE.reverseLookup(note.type),
        fields = mapOf("Title" to note.title, "General Comment" to note.generalComment, "Extra" to comment) +
                note.items
                    .withIndex()
                    .flatMap { (index, item) ->
                        val positionStr = "${index + 1}"
                        listOfNotNull(positionStr to item.value, "$positionStr comment" to item.comment)
                    }
                    .toMap()
    )

    override fun ankiNoteToCard(apiNote: ApiNote): ListDeletion = ListDeletion(
        apiNote.noteId,
        API_NOTE_TO_TYPE[apiNote.modelName] ?: error("Unsupported model name " + apiNote.modelName),
        apiNote.fields.getValue("Title").removeMultipleBreaks(),
        apiNote.makeItemsList(),
        apiNote.fields.getValue("General Comment")
    )

    override fun toHtml(note: ListDeletion): String = "TODO"

    override fun toMarkdown(note: ListDeletion): String = "TODO"
}

private fun <K, V> Map<K, V>.reverseLookup(value: V) =
    toList().first { it.second == value }.first

private fun ApiNote.makeItemsList(): List<ListDeletion.Item> = (1..20).mapNotNull { i ->
    val value = fields["$i"].takeUnless { it.isNullOrBlank() } ?: return@mapNotNull null
    val comment = fields["${i} comment"].orEmpty()
    ListDeletion.Item(value, comment)
}
