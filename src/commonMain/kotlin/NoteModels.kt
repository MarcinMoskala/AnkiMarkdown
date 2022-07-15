//import kotlinx.coroutines.coroutineScope
//import parse.AnkiApi
//import parse.ApiNote
//import parse.ApiNoteModel
//import parse.CardTemplate
//
//suspend fun main() = coroutineScope<Unit> {
//    val api = AnkiApi()
//    val modelsNames = api.getModelsNames()
//
//    if ("Basic" !in modelsNames) {
//        api.addModel(
//            ApiNoteModel(
//                modelName = "Basic",
//                inOrderFields = listOf("Front", "Back", "Extra"),
//                cardTemplates = listOf(
//                    CardTemplate("{{Front}}", "{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}\n<br><br>\n{{Extra}}")
//                )
//            )
//        )
//    }
//
//    if ("Basic (and reversed card)" !in modelsNames) {
//        api.addModel(
//            ApiNoteModel(
//                modelName = "Basic (and reversed card)",
//                inOrderFields = listOf("Front", "Back", "Extra"),
//                cardTemplates = listOf(
//                    CardTemplate("{{Front}}", "{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}\n<br><br>\n{{Extra}}"),
//                    CardTemplate("{{Back}}", "{{FrontSide}}\n\n<hr id=answer>\n\n{{Front}}\n<br><br>\n{{Extra}}")
//                )
//            )
//        )
//    }
//
//    if ("Reminder" !in modelsNames) {
//        api.addModel(
//            ApiNoteModel(
//                modelName = "Reminder",
//                inOrderFields = listOf("Front", "Extra"),
//                cardTemplates = listOf(
//                    CardTemplate("{{Front}}", "{{FrontSide}}\n<br><br>\n{{Extra}}")
//                )
//            )
//        )
//    }
//
//    if ("Triple" !in modelsNames) {
//        api.addModel(
//            ApiNoteModel(
//                modelName = "Triple",
//                inOrderFields = listOf("First", "Second", "Third"),
//                cardTemplates = listOf(
//                    CardTemplate("{{First}}", "{{FrontSide}}\n\n<hr id=answer>\n\n{{Second}}\n<br>\n{{Third}}"),
//                    CardTemplate("{{Second}}", "{{FrontSide}}\n\n<hr id=answer>\n\n{{First}}\n<br>\n{{Third}}"),
//                    CardTemplate("{{Third}}", "{{FrontSide}}\n\n<hr id=answer>\n\n{{First}}\n<br>\n{{Second}}")
//                )
//            )
//        )
//    }
//
//    val name = "Test"
//    api.createDeck(name)
//    api.addNote(
//        ApiNote(
//            deckName = name,
//            modelName = "Triple",
//            fields = mapOf("First" to "A", "Second" to "B", "Third" to "C")
//        )
//    )
//}