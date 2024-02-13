package com.marcinmoskala.application

import deckmarkdown.FileMustHaveHeader
import deckmarkdown.HeaderMustSpecifyName
import kotlinx.coroutines.coroutineScope
import java.io.File

suspend fun main() = coroutineScope<Unit> {
    val ankiConnector = AnkiConnectorJvm()
    ankiConnector.syncAllInDict(File("/Users/mm/Documents/Notes/Almanach"))
    ankiConnector.syncAllInDict(File("/Users/mm/Documents/Notes/Esencja"))
    println("Done")
}

suspend fun AnkiConnectorJvm.syncAllInDict(file: File) {
    File("/Users/mm/Documents/Notes/Almanach").listFiles()!!.forEach { file ->
        if (file.isFile && file.extension == "md") {
            runCatching {
                val pushResult = pushFile(file)
                println("For the file ${file.name} made modifications: ${pushResult.ankiModificationsCounts}")
            }.onFailure {
                if (it is FileMustHaveHeader || it is HeaderMustSpecifyName) {
                    println("${file.name} is not an anki deck")
                } else {
                    println("Error: $it")
                    it.printStackTrace()
                }
            }
        }
    }
}
