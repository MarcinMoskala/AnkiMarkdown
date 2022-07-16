package deckmarkdown.note

import deckmarkdown.Note
import deckmarkdown.Note.*
import deckmarkdown.api.ApiNote

object ClozeParser : FullNoteProcessor<Cloze> {
    private val CLOZE_REGEX = "\\{\\{([^:]+::(.+?))}}".toRegex()
    private val STANDALONE_BRACKET_REGEX = "\\{\\{([^}{]+)}}".toRegex()
    private const val API_NOTE_NAME = "Cloze"
    const val TEXT_FIELD = "Text"

    override fun handlesNote(note: Note): Boolean = note is Cloze

    override fun recognize(text: String): Boolean = CLOZE_REGEX in text || STANDALONE_BRACKET_REGEX in text

    override fun parse(id: Long?, noteText: String): Cloze = Cloze(id, processToCloze(noteText))

    override fun render(note: Cloze): String = note.text

    override fun recognizeApiNote(apiNote: ApiNote): Boolean = apiNote.modelName == API_NOTE_NAME

    override fun cardToAnkiNote(note: Cloze, deckName: String, comment: String ): ApiNote = ApiNote(
        noteId = note.id ?: ApiNote.NO_ID,
        deckName = deckName,
        modelName = API_NOTE_NAME,
        fields = mapOf(TEXT_FIELD to note.text.newLinesToBrs(), "Extra" to comment.newLinesToBrs())
    )

    override fun ankiNoteToCard(apiNote: ApiNote): Cloze = Cloze(
        apiNote.noteId,
        apiNote.readTextField(TEXT_FIELD)
    )

    override fun toHtml(note: Cloze): String = note.text
        .replace(CLOZE_REGEX) { "<b>${it.groupValues[2]}</b>" }

    override fun toMarkdown(note: Cloze): String = note.text
        .replace(CLOZE_REGEX) { "*${it.groupValues[2]}*" }

    private fun processToCloze(text: String): String {
        if (CLOZE_REGEX in text) return text
        var num = 0
        return text.replace(STANDALONE_BRACKET_REGEX) { matchResult: MatchResult ->
            val content = matchResult.groupValues[1]
            num++
            "{{c$num::$content}}"
        }
    }
}

