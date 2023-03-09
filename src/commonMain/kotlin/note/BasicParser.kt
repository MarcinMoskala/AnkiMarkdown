package deckmarkdown.note

import deckmarkdown.Note
import deckmarkdown.Note.Basic
import deckmarkdown.Note.BasicAndReverse
import deckmarkdown.api.ApiNote
import note.MarkdownParser

object BasicParser : FullNoteProcessor<Basic> {
    private val PATTERN = "[Qq]:([\\s\\S]+)\\n[Aa]:([\\s\\S]+)".toRegex()
    private val PATTERN_WITH_EXTRA = "[Qq]:([\\s\\S]+)\\n[Aa]:([\\s\\S]+)\\n[eE]:([\\s\\S]+)".toRegex()
    private const val API_NOTE_NAME = "Basic"
    val FRONT_FIELD = "Front"
    val BACK_FIELD = "Back"
    val EXTRA_FIELD = "Extra"

    override fun handlesNote(note: Note): Boolean = note is Basic

    override fun recognize(text: String): Boolean = PATTERN_WITH_EXTRA.matches(text) || PATTERN.matches(text)

    override fun parse(id: Long?, noteText: String): Basic {
        if (PATTERN_WITH_EXTRA.matches(noteText)) {
            val (question, answer, extra) = parseQAE(noteText, PATTERN_WITH_EXTRA)
            return Basic(
                id = id,
                front = question,
                back = answer,
                extra = extra
            )
        }
        val (question, answer) = parseQA(noteText, PATTERN)
        return Basic(
            id = id,
            front = question,
            back = answer
        )
    }

    override fun render(note: Basic): String = "q: {front}\na: {back}"
        .replace("{front}", note.front)
        .replace("{back}", note.back)
        .let { if (note.extra.isNullOrBlank()) it else "$it\ne: ${note.extra}" }

    override fun recognizeApiNote(apiNote: ApiNote): Boolean =
        apiNote.modelName == API_NOTE_NAME ||
        apiNote.modelName.matches(Regex("$API_NOTE_NAME-[\\d]*"))

    override fun cardToAnkiNote(note: Basic, deckName: String, comment: String ): ApiNote = ApiNote(
        noteId = note.id ?: ApiNote.NO_ID,
        deckName = deckName,
        modelName = API_NOTE_NAME,
        fields = ApiNote.fieldsOf(
            FRONT_FIELD to note.front,
            BACK_FIELD to note.back,
            EXTRA_FIELD to note.extra.orEmpty()
        )
    )

    override fun ankiNoteToCard(apiNote: ApiNote): Basic = Basic(
        apiNote.noteId,
        apiNote.field(FRONT_FIELD),
        apiNote.field(BACK_FIELD),
        apiNote.fieldOrNull(EXTRA_FIELD).takeUnless { it.isNullOrBlank() },
    )

    override fun toHtml(note: Basic): String = "<i>Q:</i> {front}<br><i>A:</i> {back}"
        .replace("{front}", note.front)
        .replace("{back}", note.back)

    override fun toMarkdown(note: Basic): String = "*Question: {front}*\n\nAnswer: {back}"
        .replace("{front}", note.front)
        .replace("{back}", note.back)
}

object BasicAndReversedParser : FullNoteProcessor<BasicAndReverse> {
    private val PATTERN = "[Qq][Aa]:([\\s\\S]+)\\n[Aa][Qq]:([\\s\\S]+)".toRegex()
    private val PATTERN_WITH_EXTRA = "[Qq][Aa]:([\\s\\S]+)\\n[Aa][Qq]:([\\s\\S]+)\\n[eE]:([\\s\\S]+)".toRegex()

    const val API_NOTE_NAME = "Basic (and reversed card)"

    override fun handlesNote(note: Note): Boolean = note is BasicAndReverse

    override fun recognize(text: String): Boolean = PATTERN.matches(text) || PATTERN_WITH_EXTRA.matches(text)

    override fun parse(id: Long?, noteText: String): BasicAndReverse {
        if (PATTERN_WITH_EXTRA.matches(noteText)) {
            val (question, answer, extra) = parseQAE(noteText, PATTERN_WITH_EXTRA)
            return BasicAndReverse(
                id = id,
                front = question,
                back = answer,
                extra = extra
            )
        }
        val (question, answer) = parseQA(noteText, PATTERN)
        return BasicAndReverse(
            id = id,
            front = question,
            back = answer
        )
    }

    override fun render(note: BasicAndReverse): String = "qa: {front}\naq: {back}"
        .replace("{front}", note.front)
        .replace("{back}", note.back)
        .let { if (note.extra.isNullOrBlank()) it else "$it\ne: ${note.extra}" }

    override fun recognizeApiNote(apiNote: ApiNote): Boolean =
        apiNote.modelName == API_NOTE_NAME ||
        apiNote.modelName.matches(Regex("Basic \\(and reversed card\\)-\\d*"))

    override fun cardToAnkiNote(note: BasicAndReverse, deckName: String, comment: String): ApiNote = ApiNote(
        noteId = note.id ?: ApiNote.NO_ID,
        deckName = deckName,
        modelName = API_NOTE_NAME,
        fields = ApiNote.fieldsOf(
            "Front" to note.front,
            "Back" to note.back,
            "Extra" to note.extra.orEmpty()
        )
    )

    override fun ankiNoteToCard(apiNote: ApiNote): BasicAndReverse = BasicAndReverse(
        apiNote.noteId,
        apiNote.field("Front"),
        apiNote.field("Back"),
        apiNote.fieldOrNull("Extra").takeUnless { it.isNullOrBlank() }
    )

    override fun toHtml(note: BasicAndReverse): String = "<i>Q/A:</i> {front}<br><i>A/Q:</i> {back}"
        .replace("{front}", note.front)
        .replace("{back}", note.back)

    override fun toMarkdown(note: BasicAndReverse): String = "*Question: {front}*\n\nAnswer: {back}"
        .replace("{front}", note.front)
        .replace("{back}", note.back)
}

private fun parseQA(text: String, regex: Regex): Pair<String, String> =
    regex
        .matchEntire(text)
        ?.groupValues
        ?.drop(1)
        ?.map { it.trim() }
        ?.let { (f, s) -> f to s }
        ?: throw IllegalArgumentException("Incorrect format of the paragraph: \n$text")

private fun parseQAE(text: String, regex: Regex): Triple<String, String, String> =
    regex
        .matchEntire(text)
        ?.groupValues
        ?.drop(1)
        ?.map { it.trim() }
        ?.let { (f, s, t) -> Triple(f, s, t) }
        ?: throw IllegalArgumentException("Incorrect format of the paragraph: \n$text")