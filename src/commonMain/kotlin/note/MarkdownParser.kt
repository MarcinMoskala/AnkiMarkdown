package note

object MarkdownParser {
    val boldMdRegex = Regex("\\*\\*([\\w\\W]*)\\*\\*")
    val boldHtmlRegex = Regex("<b>([\\w\\W]*)</b>")
    val italianicMdRegex = Regex("\\*([\\w\\W]*)\\*")
    val italianicHtmlRegex = Regex("<i>([\\w\\W]*)</i>")
    val imgMdRegex = Regex("!\\[\\[(.+)]]")
    val imgHtmlRegex = Regex("<img src=\"([\\w\\W]*)\" />")

    fun markdownToAnki(src: String): String = src
        .replace(boldMdRegex, "<b>$1</b>")
        .replace(italianicMdRegex, "<i>$1</i>")
        .replace(imgMdRegex, "<img src=\"$1\" />")
        .removeMultipleBreaks()
        .newLinesToBrs()

    fun ankiToMarkdown(src: String): String = src
        .replace(boldHtmlRegex, "**$1**")
        .replace(italianicHtmlRegex, "*$1*")
        .replace(imgHtmlRegex, "![]($1)")
        .brsToNewLines()


    fun findImagesInMarkdown(noteContent: String): List<String> =
        imgMdRegex.findAll(noteContent).map { it.groupValues[1] }.toList()

    private fun String.removeMultipleBreaks() = replace("\\n+".toRegex(), "\n")
    private fun String.newLinesToBrs() = replace("\n", "<br>\n")
    private fun String.brsToNewLines() = replace("<br>\n".toRegex(), "\n")
}