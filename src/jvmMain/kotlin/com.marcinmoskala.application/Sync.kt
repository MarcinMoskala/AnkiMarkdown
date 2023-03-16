package com.marcinmoskala.application

import deckmarkdown.AnkiConnector
import deckmarkdown.api.AnkiApi
import kotlinx.coroutines.coroutineScope

suspend fun main() = coroutineScope<Unit> {
    val ankiConnector = AnkiConnector()
//    ankiMarkup.syncFolder("notes")
//    ankiMarkup.writeNotesToFile("Aktywne::Baza_wiedzy::The-Four-Hour-Work-Week", File("notes/Aktywne::Baza_wiedzy::The-Four-Hour-Work-Week.md"))
//    ankiMarkup.writeNotesToFile("Aktywne::Latin_phrases", File("notes/Aktywne::Latin_phrases.md"))
//    ankiMarkup.pushFile(File("notes/Aktywne::Latin_phrases.md"))
//    ankiMarkup.syncFile(File("notes/Aktywne::Stoic::Ego_to_twój_wróg.md"))
//    ankiMarkup.syncFile(File("notes/Aktywne::Stoic::Stoic_tldr.md"))
    print(AnkiApi().retrieveMediaFile("210px-Alexei_Jawlensky_-_Young_Girl_with_a_Flowered_Hat,_1910_-_Google_Art_Project.jpg"))
}