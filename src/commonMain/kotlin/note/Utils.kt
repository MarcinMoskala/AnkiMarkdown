package note

fun String.removeMultipleBreaks() = replace("\\n+".toRegex(), "\n")
fun String.newLinesToBrs() = replace("\n", "<br>\n")
fun String.brsToNewLines() = replace("<br>\n".toRegex(), "\n")