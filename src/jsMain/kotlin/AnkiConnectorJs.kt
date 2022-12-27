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

    fun generateArticle(fileContent: String): FileResult? =
        connector.generateArticle(fileContent)

    fun exportAnkiPackage(fileContent: String): Promise<Boolean> = scope.promise {
        connector.exportAnkiPackage(fileContent)
    }

    fun pushDeck(deckName: String, markdown: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pushDeck(deckName, markdown)
    }

    fun pushFile(fileContent: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pushFile(fileContent)
    }

    fun createFile(deckName: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.createFile(deckName)
    }

    fun pullFile(fileContent: String): Promise<AnkiConnectorResult> = scope.promise {
        connector.pullFile(fileContent)
    }

    fun pullDeck(deckName: String, currentMarkdown: String? = null): Promise<AnkiConnectorResult> = scope.promise {
        connector.pullDeck(deckName, currentMarkdown)
    }
}