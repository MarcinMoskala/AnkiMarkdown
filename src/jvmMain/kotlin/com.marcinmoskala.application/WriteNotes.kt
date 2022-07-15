package com.marcinmoskala.application

import kotlinx.coroutines.coroutineScope
import note.DefaultParser

suspend fun main() = coroutineScope<Unit> {
    val notes = AnkiConnector()
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