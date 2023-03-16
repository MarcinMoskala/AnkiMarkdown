package deckmarkdown.api

import deckmarkdown.hashCodeOf
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import note.MarkdownParser

interface RepositoryApi {
    suspend fun connected(): Boolean
    suspend fun addNote(apiNote: ApiNote): ApiNote
    suspend fun updateNoteFields(apiNote: ApiNote): ApiNote
    suspend fun getNotesInDeck(deckName: String): List<ApiNote>
    suspend fun createDeck(name: String)
    suspend fun removeDeck(name: String)
    suspend fun deleteNotes(ids: Set<Long>)
    suspend fun getDecks(): List<String>
    suspend fun getModelsNames(): List<String>
    suspend fun addModel(model: ApiNoteModel)
    suspend fun exportPackage(deckName: String, packageDestination: String, includeScheduled: Boolean)
    suspend fun storeMediaFile(fileName: String, fileContentBase64: String): Boolean
    suspend fun retrieveMediaFile(fileName: String): String
}

@Serializable
data class ResultWithListOfString(
    val result: List<String>?,
    val error: String? = null
)

@Serializable
data class ResultWithString(
    val result: String?,
    val error: String? = null
)

@Serializable
data class ResultWithListOfLongs(
    val result: List<Long>?,
    val error: String? = null
)

@Serializable
data class ResultWithId(
    val result: Long?,
    val error: String? = null
)

@Serializable
data class ResultWithNoteReceiveDataApiList(
    val result: List<NoteReceiveDataApi>?,
    val error: String? = null
)

@Serializable
data class ResultWrapper<T>(val result: T? = null, val error: String? = null)

@Serializable
class FileNameAndData(val filename: String, val data: String)

sealed class ApiNoteOrText
data class Text(val text: String) : ApiNoteOrText()

@Serializable
data class ApiNote(
    val noteId: Long = NO_ID, // For creation only, API do not care
    val deckName: String,
    val modelName: String,
    val fields: Map<String, String>,
) : ApiNoteOrText() {
    val hasId get() = noteId != NO_ID

    fun field(name: String): String = fieldOrNull(name)
        ?: error("Missing required field $name")

    fun fieldOrNull(name: String): String? = fields[name]
        ?.let(MarkdownParser::ankiToMarkdown)

    override fun equals(other: Any?): Boolean =
        other is ApiNote &&
        deckName == other.deckName &&
        modelName == other.modelName &&
        fields == other.fields

    override fun hashCode(): Int = hashCodeOf(deckName, modelName, fields)

    companion object {
        const val NO_ID = -1L

        fun basic(id: Long, deckName: String, front: String, back: String) =
            ApiNote(noteId = id, deckName = deckName, modelName = "Basic", fields = mapOf("Front" to front, "Back" to back))

        fun cloze(id: Long, deckName: String, text: String) =
            ApiNote(noteId = id, deckName = deckName, modelName = "Cloze", fields = mapOf("Text" to text))

        fun fieldsOf(vararg nameToValue: Pair<String, String>): Map<String, String> =
            nameToValue.toMap().mapValues { (_, value) -> value.let(MarkdownParser::markdownToAnki) }
    }
}

@Serializable
data class ApiNoteModel(
    val modelName: String,
    val inOrderFields: List<String>,
    val cardTemplates: List<CardTemplate>
)

@Serializable
data class CardTemplate(
    val Front: String,
    val Back: String
)

@Serializable
data class NoteReceiveDataApi(
    val noteId: Long,
    val modelName: String,
    val fields: Map<String, OrderedField>,
    val tags: List<String> = emptyList()
) {
    fun toNoteData(deckName: String): ApiNote = ApiNote(
        noteId = noteId,
        deckName = deckName,
        modelName = modelName,
        fields = fields.mapValues { it.value.value },
    )
}

@Serializable
data class OrderedField(
    val value: String,
    val order: Int
)

class AnkiApi : RepositoryApi {
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val url = "http://localhost:8765"
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun connected(): Boolean = try {
        client.post(url).status.value in 100..300
    } catch (exception: Throwable) {
        false
    }

    override suspend fun getNotesInDeck(deckName: String): List<ApiNote> {
        val res = request<ResultWithListOfLongs>("findNotes", """{"query": "deck:$deckName"}""")
        val notesIds = res.result ?: throw Error(res.error)
        val idsAsString = notesIds.joinToString(prefix = "[", postfix = "]", separator = ", ")

        val res2 = request<ResultWithNoteReceiveDataApiList>("notesInfo", """{"notes": $idsAsString}""")
        return res2.result
            ?.map { it.toNoteData(deckName) }
            ?: throw Error("${res2.error} for $deckName")
    }

    override suspend fun addNote(apiNote: ApiNote): ApiNote {
        val noteStr = json.encodeToString(apiNote)
        val res = request<ResultWithId>("addNote", """{"note": $noteStr}""")
        val id = res.result ?: throw Error("Error while adding note $noteStr")
        return apiNote.copy(noteId = id)
    }

    override suspend fun updateNoteFields(apiNote: ApiNote): ApiNote {
        val fieldsStr = json.encodeToString(apiNote.fields)
        call("updateNoteFields", """{ "note": { "id": ${apiNote.noteId}, "fields": $fieldsStr}}""")
        return apiNote
    }

    override suspend fun createDeck(name: String) {
        call("createDeck", """{"deck": "$name"}""")
    }

    override suspend fun removeDeck(name: String) {
        call("deleteNotes", """{"decks": ["$name"], "cardsToo": true}""")
    }

    override suspend fun deleteNotes(ids: Set<Long>) {
        val idsAsString = ids.joinToString(prefix = "[", postfix = "]", separator = ", ")
        call("deleteNotes", """{"notes": $idsAsString}""")
    }

    override suspend fun getDecks(): List<String> {
        val res = request<ResultWithListOfString>("deckNames")
        return res.result ?: throw Error(res.error)
    }

    override suspend fun getModelsNames(): List<String> {
        val res = request<ResultWithListOfString>("modelNames")
        return res.result ?: throw Error(res.error)
    }

    override suspend fun addModel(model: ApiNoteModel) {
        call("createModel", json.encodeToString(model))
    }

    override suspend fun exportPackage(deckName: String, packageDestination: String, includeScheduled: Boolean) {
        call("exportPackage", """{"deck": "$deckName", "path": "$packageDestination", "includeSched": $includeScheduled}""")
    }

    override suspend fun storeMediaFile(fileName: String, fileContentBase64: String): Boolean {
        val data = FileNameAndData(fileName, fileContentBase64)
        return request<ResultWithString>("storeMediaFile", json.encodeToString(data))
            .result != "false"
    }

    override suspend fun retrieveMediaFile(fileName: String): String =
        request<ResultWithString>("retrieveMediaFile", """{ "filename": "$fileName" }""")
            .result
            .orEmpty()

    suspend fun getAllMediaFileNames(): List<String> =
        request<ResultWithListOfString>("getMediaFilesNames", """{ "pattern": "*" }""")
            .result
            .orEmpty()

    private suspend fun call(action: String, params: String) {
        val resText = client.post(url) {
            setBody("""{"action": "$action", "version": 6, "params": $params}""")
        }.body<String>()
        val res = json.parseToJsonElement(resText)
        if (res.jsonObject["error"] != JsonNull) throw Error("${res.jsonObject["error"]} for $action and params $params")
    }

    private suspend inline fun <reified T> request(action: String, params: String? = null): T {
        val resText = client.post(url) {
            val paramsBody = if (params != null) """, "params": $params""" else ""
            setBody("""{"action": "$action", "version": 6 $paramsBody}""")
        }.body<String>()
        return json.decodeFromString(resText)
    }
}

//fun File.readInBase64(): String {
//    val bytes = this.readBytes()
//    val base64 = Base64.getEncoder().encodeToString(bytes)
//    return base64
//}
