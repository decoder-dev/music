package it.decoder.music.preferences

import it.decoder.music.GlobalPreferencesHolder
import it.decoder.music.enums.AlbumSortBy
import it.decoder.music.enums.ArtistSortBy
import it.decoder.music.enums.PlaylistSortBy
import it.decoder.music.enums.SongSortBy
import it.decoder.music.enums.SortOrder

object OrderPreferences : GlobalPreferencesHolder() {
    var songSortOrder by enum(SortOrder.Descending)
    var localSongSortOrder by enum(SortOrder.Descending)
    var playlistSortOrder by enum(SortOrder.Descending)
    var albumSortOrder by enum(SortOrder.Descending)
    var artistSortOrder by enum(SortOrder.Descending)

    var songSortBy by enum(SongSortBy.DateAdded)
    var localSongSortBy by enum(SongSortBy.DateAdded)
    var playlistSortBy by enum(PlaylistSortBy.DateAdded)
    var albumSortBy by enum(AlbumSortBy.DateAdded)
    var artistSortBy by enum(ArtistSortBy.DateAdded)
}
