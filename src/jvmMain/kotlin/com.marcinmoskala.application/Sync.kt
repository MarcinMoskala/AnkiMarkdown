package com.marcinmoskala.application

import kotlinx.coroutines.coroutineScope
import java.io.File

suspend fun main() = coroutineScope<Unit> {
    val ankiMarkup = AnkiConnector()
//    ankiMarkup.syncFolder("notes")
//    ankiMarkup.writeNotesToFile("Aktywne::Baza_wiedzy::The-Four-Hour-Work-Week", File("notes/Aktywne::Baza_wiedzy::The-Four-Hour-Work-Week.md"))
//    ankiMarkup.writeNotesToFile("Aktywne::Latin_phrases", File("notes/Aktywne::Latin_phrases.md"))
    ankiMarkup.pushFile(File("notes/Aktywne::Latin_phrases.md"))
//    ankiMarkup.syncFile(File("notes/Aktywne::Stoic::Ego_to_twój_wróg.md"))
//    ankiMarkup.syncFile(File("notes/Aktywne::Stoic::Stoic_tldr.md"))
    print("Done")
}