package it.decoder.music.ui.screens.builtinplaylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import it.decoder.compose.persist.PersistMapCleanup
import it.decoder.compose.routing.RouteHandler
import it.decoder.music.R
import it.decoder.music.enums.BuiltInPlaylist
import it.decoder.music.preferences.DataPreferences
import it.decoder.music.ui.components.themed.Scaffold
import it.decoder.music.ui.screens.GlobalRoutes
import it.decoder.music.ui.screens.Route

@Route
@Composable
fun BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableIntStateOf(
            when (builtInPlaylist) {
                BuiltInPlaylist.Favorites -> 0
                BuiltInPlaylist.Offline -> 1
                BuiltInPlaylist.Top -> 2
            }
        )
    }

    PersistMapCleanup(prefix = "${builtInPlaylist.name}/")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanged,
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.favorites), R.drawable.heart)
                    item(1, stringResource(R.string.offline), R.drawable.airplane)
                    item(
                        2,
                        stringResource(R.string.format_top_playlist, DataPreferences.topListLength),
                        R.drawable.trending_up
                    )
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Favorites)
                        1 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Offline)
                        2 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Top)
                    }
                }
            }
        }
    }
}
