package com.marcinmoskala.application

import deckmarkdown.AnkiConnector
import deckmarkdown.AnkiConnectorResult
import deckmarkdown.Note
import deckmarkdown.note.DeckParser
import deckmarkdown.note.DefaultParser
import deckmarkdown.api.AnkiApi
import deckmarkdown.api.ApiNote
import deckmarkdown.api.RepositoryApi
import java.io.File

class AnkiConnectorJvm(
    private val connector: AnkiConnector = AnkiConnector(),
) {
    suspend fun pushFile(file: File): AnkiConnectorResult {
        val pushResult = connector.pushFile(file.readText())
        file.writeText(pushResult.markdown)
        return pushResult
    }
}
