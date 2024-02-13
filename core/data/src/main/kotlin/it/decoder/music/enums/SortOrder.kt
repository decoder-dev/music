package it.decoder.music.enums

enum class SortOrder {
    Ascending,
    Descending;

    operator fun not() = when (this) {
        Ascending -> Descending
        Descending -> Ascending
    }
}
