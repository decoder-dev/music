package it.decoder.music.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import it.decoder.compose.routing.Route0
import it.decoder.compose.routing.Route1
import it.decoder.compose.routing.RouteHandlerScope
import it.decoder.music.enums.BuiltInPlaylist
import it.decoder.music.enums.StatisticsType
import it.decoder.music.ui.screens.album.AlbumScreen
import it.decoder.music.ui.screens.artist.ArtistScreen
import it.decoder.music.ui.screens.playlist.PlaylistScreen
import it.decoder.music.ui.screens.home.QuickPicks
import it.decoder.music.ui.screens.statistics.StatisticsScreen

val quickpicksRoute = Route1<String?>("quickpicksRoute")
val albumRoute = Route1<String?>("albumRoute")
val artistRoute = Route1<String?>("artistRoute")
val builtInPlaylistRoute = Route1<BuiltInPlaylist>("builtInPlaylistRoute")
val statisticsTypeRoute = Route1<StatisticsType>("statisticsTypeRoute")
val localPlaylistRoute = Route1<Long?>("localPlaylistRoute")
val playlistRoute = Route1<String?>("playlistRoute")
val searchResultRoute = Route1<String>("searchResultRoute")
val searchRoute = Route1<String>("searchRoute")
val settingsRoute = Route0("settingsRoute")

@SuppressLint("ComposableNaming")
@Suppress("NOTHING_TO_INLINE")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
inline fun RouteHandlerScope.globalRoutes() {
    albumRoute { browseId ->
        AlbumScreen(
            browseId = browseId ?: error("browseId cannot be null")
        )
    }

    artistRoute { browseId ->
        ArtistScreen(
            browseId = browseId ?: error("browseId cannot be null")
        )
    }

    playlistRoute { browseId ->
        PlaylistScreen(
            browseId = browseId ?: error("browseId cannot be null")
        )
    }

    statisticsTypeRoute { browseId ->
        StatisticsScreen(
            statisticsType = browseId ?: error("browseId cannot be null")
        )
    }

    quickpicksRoute { browseId ->

    }
}
