package deckmarkdown.note

import deckmarkdown.Note
import deckmarkdown.api.ApiNote

val DefaultParser = DeckParser(
    processors = listOf(
        BasicParser,
        BasicAndReversedParser,
        ReminderParser,
        ListDeletionParser,
        ClozeParser,
        GeneralParser,
        TextParser
    )
)

class DeckParser(private val processors: List<NoteProcessor<*>>) {

    fun parseNotes(markdown: String): List<Note> {
        val slitted = markdown.split("\n\n")
        return slitted.asSequence()
            .map { it.trimStart().trimEnd() }
            .map { paragraph ->
                if (paragraph.startsWith("@")) {
                    require(paragraph.contains("\n")) { "Nothing after id in the paragraph $paragraph" }
                    val (idLine, notesText) = paragraph.split("\n", limit = 2)
                    NotesTextWithId(idLine.substringAfter("@").toLongOrNull(), notesText)
                } else {
                    NotesTextWithId(null, paragraph)
                }
            }
            .mapNotNull { (id, noteText) ->
                val serializer = processors.filterIsInstance<CardParser<*>>()
                    .firstOrNull { it.recognize(noteText) }
                serializer?.parse(id, noteText)
            }
            .toList()
    }

    fun apiNoteToNote(apiNote: ApiNote): Note {
        val serializer = processors.filterIsInstance<AnkiParser<*>>()
            .first { it.recognizeApiNote(apiNote) }
        return serializer.ankiNoteToCard(apiNote)
    }

    fun noteToApiNote(note: Note, deckName: String, comment: String): ApiNote {
        val serializer = processors.filterIsInstance<AnkiParser<*>>()
            .first { it.handlesNote(note) }
        return serializer._cardToAnkiNote(note, deckName, comment)
    }

    private data class NotesTextWithId(val id: Long?, val notesText: String)

    fun writeNotes(notes: List<Note>): String = notes.joinToString(separator = "\n\n") { note ->
        val head = if (note.id == null) "" else "@${note.id}\n"
        val serializer = processors.filterIsInstance<CardParser<*>>()
            .first { it.handlesNote(note) }
        val noteText = serializer._render(note)
        return@joinToString "$head$noteText"
    }

    fun htmlWriteNotes(notes: List<Note>): String = notes.joinToString(separator = "\n") { note ->
        val serializer = processors.filterIsInstance<HtmlSerializer<*>>()
            .first { it.handlesNote(note) }
        val elemAsHtml = serializer._toHtml(note)
        return@joinToString "<div>$elemAsHtml</div>"
    }

    fun markdownWriteNotes(notes: List<Note>): String = notes.joinToString(separator = "\n\n") { note ->
        val serializer = processors.filterIsInstance<MarkdownSerializer<*>>()
            .first { it.handlesNote(note) }
        return@joinToString serializer._toMarkdown(note)
    }
}

interface FullNoteProcessor<T : Note> : CardParser<T>, AnkiParser<T>, HtmlSerializer<T>, MarkdownSerializer<T>

interface NoteProcessor<T : Note> {
    fun handlesNote(note: Note): Boolean
}

interface CardParser<T : Note> : NoteProcessor<T> {
    fun recognize(text: String): Boolean
    fun parse(id: Long?, noteText: String): T
    fun render(note: T): String
    @Suppress("UNCHECKED_CAST")
    fun _render(note: Any?): String = render(note as T)
}

interface AnkiParser<T : Note> : NoteProcessor<T> {
    fun recognizeApiNote(apiNote: ApiNote): Boolean
    fun cardToAnkiNote(note: T, deckName: String, comment: String): ApiNote
    fun ankiNoteToCard(apiNote: ApiNote): T
    @Suppress("UNCHECKED_CAST")
    fun _cardToAnkiNote(note: Any?, deckName: String, comment: String): ApiNote = cardToAnkiNote(note as T, deckName, comment)
}

interface HtmlSerializer<T : Note> : NoteProcessor<T> {
    fun toHtml(note: T): String
    @Suppress("UNCHECKED_CAST")
    fun _toHtml(note: Any?) = toHtml(note as T)
}

interface MarkdownSerializer<T : Note> : NoteProcessor<T> {
    fun toMarkdown(note: T): String
    @Suppress("UNCHECKED_CAST")
    fun _toMarkdown(note: Any?) = toMarkdown(note as T)
}