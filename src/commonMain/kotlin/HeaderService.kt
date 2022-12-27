package deckmarkdown

import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlBuilder

internal class HeaderService {
    private val yaml by lazy {
        Yaml {
            encodeDefaultValues = false
            stringSerialization = YamlBuilder.StringSerialization.DOUBLE_QUOTATION
        }
    }

    fun separateHeaderFromFile(fileContent: String): HeaderSeparationResult {
        val matchResult = Regex("""^(---([\w\W]*)\n---[\n]+)([\w\W]*)""")
            .find(fileContent.trim())
            ?: throw FileMustHaveHeader
        val markdown = matchResult.groupValues[3]
        val originalHeader = matchResult.groupValues[1]
        val headerContent = matchResult.groupValues[2]
        if (HeaderConfig::deckName.name !in headerContent) throw HeaderMustSpecifyName
        val headerConfig = yaml.decodeFromString(HeaderConfig.serializer(), headerContent)
        return HeaderSeparationResult(markdown, headerConfig, originalHeader)
    }

    fun headerToText(headerConfig: HeaderConfig): String =
        yaml.encodeToString(HeaderConfig.serializer(), headerConfig)
            .let { "---\n$it\n---\n\n" }

    data class HeaderSeparationResult(
        val markdown: String,
        val headerConfig: HeaderConfig,
        val originalHeader: String?,
    )
}