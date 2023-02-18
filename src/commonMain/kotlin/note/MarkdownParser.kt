package note

object MarkdownParser {
    private val boldMdRegex = Regex("\\*\\*([\\w\\W]*?)\\*\\*")
    private val boldHtmlRegex = Regex("<b>([\\w\\W]*?)</b>")
    private val italianicMdRegex = Regex("\\*([\\w\\W]*?)\\*")
    private val italianicHtmlRegex = Regex("<i>([\\w\\W]*?)</i>")
    private val imgMdRegex = Regex("!\\[\\[(.+?)\\]\\]")
    private val imgHtmlRegex = Regex("<img src=\"([\\w\\W]*?)\" />")

    fun markdownToAnki(src: String): String = src
        .replace(boldMdRegex, "<b>$1</b>")
        .replace(italianicMdRegex, "<i>$1</i>")
        .replace(imgMdRegex, "<img src=\"$1\" />")
        .removeMultipleBreaks()
        .newLinesToBrs()

    fun ankiToMarkdown(src: String): String = src
        .replace(boldHtmlRegex, "**$1**")
        .replace(italianicHtmlRegex, "*$1*")
        .replace(imgHtmlRegex, "![[$1]]")
        .brsToNewLines()


    fun findImagesInMarkdown(noteContent: String): List<String> =
        imgMdRegex.findAll(noteContent).map { it.groupValues[1] }.toList()

    private fun String.removeMultipleBreaks() = replace("\\n+".toRegex(), "\n")
    private fun String.newLinesToBrs() = replace("\n", "<br>\n")
    private fun String.brsToNewLines() = replace("<br>\n".toRegex(), "\n")
}