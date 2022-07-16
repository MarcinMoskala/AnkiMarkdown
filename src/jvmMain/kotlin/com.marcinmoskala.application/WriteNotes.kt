package com.marcinmoskala.application

import kotlinx.coroutines.coroutineScope
import deckmarkdown.note.DefaultParser

suspend fun main() = coroutineScope<Unit> {
    val notes = AnkiConnectorJvm()
        .readNotesFromDeck(deckName = "Wiedza::Techniczne::Modele_Predykcyjne")

    println("Pure:")
    notes.let(DefaultParser::writeNotes)
        .let(::println)

    println("Html:")
    notes.let(DefaultParser::htmlWriteNotes)
        .let(::println)

    println("Markdown:")
    notes.let(DefaultParser::markdownWriteNotes)
        .let(::println)
}