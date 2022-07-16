package deckmarkdown.note

internal fun String.removeMultipleBreaks() = replace("\\n+".toRegex(), "\n")
internal fun String.newLinesToBrs() = replace("\n", "<br>\n")
internal fun String.brsToNewLines() = replace("<br>\n".toRegex(), "\n")