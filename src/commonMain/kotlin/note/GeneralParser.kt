package deckmarkdown.note

import deckmarkdown.api.ApiNote
import deckmarkdown.Note
import deckmarkdown.Note.General
import note.MarkdownParser

// TODO
/*
 * Consumes all API notes (see recognizeApiNote), so should be after all other
 * notes but before TextParser.
 */
object GeneralParser : FullNoteProcessor<General> {
    private val mdParser = MarkdownParser

    override fun handlesNote(note: Note): Boolean = note is General

    override fun recognize(text: String): Boolean = TODO()

    override fun parse(id: Long?, noteText: String): General = TODO()

    override fun render(note: General): String = TODO()

    /*
     * Every unrecognized API note should be treated as general.
     */
    override fun recognizeApiNote(apiNote: ApiNote): Boolean = true

    override fun cardToAnkiNote(note: General, deckName: String, comment: String): ApiNote = ApiNote(
        noteId = note.id ?: ApiNote.NO_ID,
        deckName = deckName,
        modelName = note.modelName,
        fields = note.fields
    )

    override fun ankiNoteToCard(apiNote: ApiNote): General = TODO()

    override fun toHtml(note: General): String = TODO()

    override fun toMarkdown(note: General): String = TODO()
}