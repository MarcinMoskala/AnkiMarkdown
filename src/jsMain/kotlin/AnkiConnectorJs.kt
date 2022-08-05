@file:OptIn(ExperimentalJsExport::class)

import deckmarkdown.AnkiConnector
import deckmarkdown.AnkiConnectorResult
import deckmarkdown.FileResult
import deckmarkdown.api.AnkiApi
import deckmarkdown.note.DefaultParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.promise
import kotlin.js.Promise

@JsExport
@JsName("AnkiConnector")
class AnkiConnectorJs {
    private val connector = AnkiConnector(AnkiApi(), DefaultParser)
    private val scope = CoroutineScope(SupervisorJob())

    fun checkConnection(): Promise<Boolean> = scope.promise {
        connector.checkConnection()
    }

    fun getDeckNames(): Promise<Array<String>> = scope.promise {
        connector.getDeckNames().toTypedArray()
    }

    fun generateArticle(fileName: String, fileContent: String): FileResult? =
        connector.generateArticle(fileName, fileContent)

    fun exportAnkiPackage(fileName: String, fileContent: String): Promise<Boolean> = scope.promise {
        connector.exportAnkiPackage(fileName, fileContent)
    }

    fun pushFile(fileName: String, fileContent: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pushFile(fileName, fileContent)
    }

    fun pullDeckToExistingFile(fileName: String, fileContent: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pullDeckToExistingFile(fileName, fileContent)
    }

    fun pushDeck(deckName: String, markdown: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pushDeck(deckName, markdown)
    }

    fun pullDeckToExisting(deckName: String, currentMarkdown: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pullDeckToExisting(deckName, currentMarkdown)
    }

    fun pullDeck(deckName: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pullDeck(deckName)
    }
}