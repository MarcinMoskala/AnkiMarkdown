
inline fun hashCodeOf(vararg values: Any?) = values
    .fold(0) { acc, v -> (acc + v.hashCode()) * 31 }