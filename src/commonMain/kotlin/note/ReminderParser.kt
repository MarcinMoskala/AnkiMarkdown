package deckmarkdown.note

import deckmarkdown.Note
import deckmarkdown.Note.*
import deckmarkdown.api.ApiNote
import note.MarkdownParser

object ReminderParser : FullNoteProcessor<Reminder> {
    private val PATTERN = "[Rr](eminder)?:([\\s\\S]+)".toRegex()
    private const val API_NOTE_NAME = "Reminder"
    private val mdParser = MarkdownParser

    override fun handlesNote(note: Note): Boolean = note is Reminder

    override fun recognize(text: String): Boolean = PATTERN.matches(text)

    override fun parse(id: Long?, noteText: String): Reminder =
        Reminder(id, noteText.substringAfter(":").trim())

    override fun render(note: Reminder): String = "Reminder: " + note.text

    override fun recognizeApiNote(apiNote: ApiNote): Boolean = apiNote.modelName == API_NOTE_NAME

    override fun cardToAnkiNote(note: Reminder, deckName: String, comment: String): ApiNote = ApiNote(
        noteId = note.id ?: ApiNote.NO_ID,
        deckName = deckName,
        modelName = "Reminder",
        fields = mapOf(
            "Front" to note.text.let(mdParser::markdownToAnki),
            "Extra" to comment.let(mdParser::markdownToAnki)
        )
    )

    override fun ankiNoteToCard(apiNote: ApiNote): Reminder = Reminder(
        apiNote.noteId,
        apiNote.fields.getValue("Front").let(mdParser::ankiToMarkdown)
    )

    override fun toHtml(note: Reminder): String = note.text

    override fun toMarkdown(note: Reminder): String = note.text
}