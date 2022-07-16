package deckmarkdown

import deckmarkdown.api.AnkiApi
import deckmarkdown.note.DefaultParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlin.js.Promise

@JsExport
class AnkiConnectorJs {
    private val connector = AnkiConnector(AnkiApi(), DefaultParser)
    private val scope = CoroutineScope(SupervisorJob())

    fun pushDeck(deckName: String, markdown: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pushDeck(deckName, markdown)
    }

    fun pullDeckToExisting(deckName: String, currentMarkdown: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pullDeckToExisting(deckName, currentMarkdown)
    }

    fun pullDeck(deckName: String, comment: String? = null): Promise<AnkiConnectorResult> = scope.promise {
        connector.pullDeck(deckName, comment)
    }
}