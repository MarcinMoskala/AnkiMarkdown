package deckmarkdown

sealed class Note {
    abstract val id: Long?

    data class Basic(
        override val id: Long? = null,
        val front: String,
        val back: String,
        val extra: String? = null,
    ) : Note()

    data class BasicAndReverse(
        override val id: Long? = null,
        val front: String,
        val back: String,
        val extra: String? = null,
    ) : Note()

    data class Cloze(
        override val id: Long? = null,
        val text: String,
        val extra: String? = null,
    ) : Note()

    data class Reminder(
        override val id: Long? = null,
        val text: String
    ) : Note()

    data class General(
        override val id: Long? = null,
        val modelName: String,
        val fields: Map<String, String>
    ) : Note()

    data class ListDeletion(
        override val id: Long? = null,
        val type: ListType = ListType.List,
        val title: String,
        val items: List<Item>,
        val generalComment: String = ""
    ) : Note() {
        data class Item(val value: String, val comment: String = "")

        enum class ListType {
            List, Set
        }
    }

    data class Text(
        val text: String
    ) : Note() {
        override val id: Long? = null
    }
}