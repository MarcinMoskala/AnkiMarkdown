package deckmarkdown

inline fun hashCodeOf(vararg values: Any?) = values
    .fold(0) { acc, v -> (acc + v.hashCode()) * 31 }

fun String.recognizeKeyValueLines(): Map<String, String>? = this
    .split("\n")
    .fold(emptyList<Pair<String, String>>()) { acc, line ->
        val matches = KEY_REGEX.matchEntire(line)
        if (matches == null) {
            val (key, value) = acc.lastOrNull() ?: return@recognizeKeyValueLines null
            return@fold acc.dropLast(1) + (key to value + "\n" + line)
        }
        val (_, key, value) = matches.groupValues
        acc + (key to value)
    }.toMap()

private val KEY_REGEX = Regex("^([\\w-]+): ([\\W\\w]*)$")
