package deckmarkdown.note

import deckmarkdown.Note
import deckmarkdown.Note.General
import deckmarkdown.api.ApiNote
import deckmarkdown.recognizeKeyValueLines

/*
 * Consumes all API notes (see recognizeApiNote below), so should be after all other
 * notes but before TextParser.
 */
object GeneralParser : FullNoteProcessor<General> {
    val MODEL_NAME_FIELD = "modelName"

    override fun handlesNote(note: Note): Boolean = note is General

    override fun recognize(text: String): Boolean =
        text.recognizeKeyValueLines()?.let { it[MODEL_NAME_FIELD] } != null

    override fun parse(id: Long?, noteText: String): General =
        noteText.recognizeKeyValueLines()!!
            .let { fields ->
                General(
                    id = id,
                    modelName = fields[MODEL_NAME_FIELD]!!.trim(),
                    fields = fields - MODEL_NAME_FIELD
                )
            }

    override fun render(note: General): String =
        "modelName: ${note.modelName}\n" +
        note.fields.toList().joinToString(separator = "\n") { (field, value) -> "$field: $value" }

    /*
     * Every unrecognized API note should be treated as general.
     */
    override fun recognizeApiNote(apiNote: ApiNote): Boolean = true

    override fun cardToAnkiNote(note: General, deckName: String, comment: String): ApiNote = ApiNote(
        noteId = note.id ?: ApiNote.NO_ID,
        deckName = deckName,
        modelName = note.modelName,
        fields = note.fields,
    )

    override fun ankiNoteToCard(apiNote: ApiNote): General = General(
        id = apiNote.noteId,
        modelName = apiNote.modelName,
        fields = apiNote.fields,
    )

    override fun toHtml(note: General): String = TODO()

    override fun toMarkdown(note: General): String = TODO()
}