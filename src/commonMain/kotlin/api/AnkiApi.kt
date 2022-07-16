package deckmarkdown.api

import deckmarkdown.note.brsToNewLines
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import deckmarkdown.note.removeMultipleBreaks

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
//    suspend fun storeMediaFile(file: File)
}

data class ResultWrapper<T>(val result: T? = null, val error: String? = null)

sealed class ApiNoteOrText
data class Text(val text: String) : ApiNoteOrText()

@Serializable
data class ApiNote(
    val noteId: Long = NO_ID, // For creation, API do not care
    val deckName: String,
    val modelName: String,
    val fields: Map<String, String>,
) : ApiNoteOrText() {
    val hasId get() = noteId != NO_ID

    fun readTextField(field: String): String = fields.getValue(field)
        .removeMultipleBreaks()
        .brsToNewLines()

    companion object {
        const val NO_ID = -1L
    }
}

data class ApiNoteModel(
    val modelName: String,
    val inOrderFields: List<String>,
    val cardTemplates: List<CardTemplate>
)

data class CardTemplate(
    val Front: String,
    val Back: String
)

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

data class OrderedField(
    val value: String,
    val order: Int
)

class AnkiApi : RepositoryApi {
    private val url = "http://localhost:8765"
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun connected(): Boolean = try {
        client.post(url).status.value in 100..300
    } catch (exception: Throwable) {
        exception.printStackTrace()
        false
    }

    override suspend fun getNotesInDeck(deckName: String): List<ApiNote> {
        val res = client.post(url) {
            setBody("""{"action": "findNotes", "version": 6, "params": {"query": "deck:$deckName"}}""")
        }.body<ResultWrapper<List<String>>>()
        val notesIds = res.result ?: throw Error(res.error)
        val idsAsString = notesIds.joinToString(prefix = "[", postfix = "]", separator = ", ")

        val res2 = client.post(url) {
            setBody("""{"action": "notesInfo", "version": 6, "params": {"notes": $idsAsString}}""")
        }.body<ResultWrapper<List<NoteReceiveDataApi>>>()
        return res2.result
            ?.map { it.toNoteData(deckName) }
            ?: throw Error("${res2.error} for $deckName")
    }

    override suspend fun addNote(apiNote: ApiNote): ApiNote {
        val noteStr = Json.encodeToString(apiNote)
        val res = client.post(url) {
            setBody("""{"action": "addNote", "version": 6, "params": {"note": $noteStr}}""")
        }.body<ResultWrapper<Long>>()
        val id = res.result ?: throw Error("${res.error} for $apiNote")
        return apiNote.copy(noteId = id)
    }

    override suspend fun updateNoteFields(apiNote: ApiNote): ApiNote {
        val fieldsStr = Json.encodeToString(apiNote.fields)
        val res = client.post(url) {
            setBody("""{"action": "updateNoteFields", "version": 6, "params": { "note": { "id": ${apiNote.noteId}, "fields": $fieldsStr}}}""")
        }.body<ResultWrapper<Any?>>()
        if (res?.error != null) throw Error("${res.error} for $apiNote")
        return apiNote
    }

    override suspend fun createDeck(name: String) {
        val res = client.post(url) {
            setBody("""{"action": "createDeck", "version": 6, "params": {"deck": "$name"}}""")
        }.body<ResultWrapper<Long>>()
        if (res.error != null) throw Error("${res.error} for $name")
    }

    override suspend fun removeDeck(name: String) {
        val res = client.post(url) {
            setBody("""{"action": "deleteDecks", "version": 6, "params": {"decks": ["$name"], "cardsToo": true}}""")
        }.body<ResultWrapper<Long>>()
        if (res.error != null) throw Error("${res.error} for $name")
    }

    override suspend fun deleteNotes(ids: Set<Long>) {
        val idsAsString = ids.joinToString(prefix = "[", postfix = "]", separator = ", ")
        val res = client.post(url) {
            val bodyText = """{"action": "deleteNotes", "version": 6, "params": {"notes": $idsAsString}}"""
            setBody(bodyText)
        }.body<ResultWrapper<Any?>>()
        if (res?.error != null) throw Error("${res.error} for $ids")
    }

    override suspend fun getDecks(): List<String> {
        val res = client.post(url) {
            setBody("""{"action": "deckNames", "version": 6}""")
        }.body<ResultWrapper<List<String>>>()
        return res.result ?: throw Error(res.error)
    }

    override suspend fun getModelsNames(): List<String> {
        val res = client.post(url) {
            setBody("""{"action": "modelNames", "version": 6}""")
        }.body<ResultWrapper<List<String>>>()
        return res.result ?: throw Error(res.error)
    }

    override suspend fun addModel(model: ApiNoteModel) {
        val modelStr = Json.encodeToString(model)
        val res = client.post(url) {
            setBody("""{"action": "createModel", "version": 6, "params": $modelStr}""")
        }.body<ResultWrapper<Any?>>()
        if (res?.error != null) throw Error("${res.error} for $model (str $modelStr)")
    }
// TODO
//    override suspend fun storeMediaFile(file: File) {
//        val filename = file.name
//        val text = client.post<String>(url) {
//            val bodyText =
//                """{"action": "storeMediaFile", "version": 6, "params": { "filename": "$filename", "data": "${file.readInBase64()}" }}"""
//            body = bodyText
//        }
//        val res = text.readObjectOrNull<ResultWrapper<Any?>>()
//        if (res?.error != null) throw Error("${res.error}")
//    }
}

//fun File.readInBase64(): String {
//    val bytes = this.readBytes()
//    val base64 = Base64.getEncoder().encodeToString(bytes)
//    return base64
//}
