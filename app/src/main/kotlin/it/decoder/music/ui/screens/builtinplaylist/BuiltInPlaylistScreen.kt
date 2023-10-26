package it.decoder.music.ui.screens.builtinplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import it.decoder.compose.persist.PersistMapCleanup
import it.decoder.compose.routing.RouteHandler
import it.decoder.music.R
import it.decoder.music.enums.BuiltInPlaylist
import it.decoder.music.ui.components.themed.Scaffold
import it.decoder.music.ui.screens.globalRoutes

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableStateOf(when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> 0
            BuiltInPlaylist.Offline -> 1
        })
    }

    PersistMapCleanup(tagPrefix = "${builtInPlaylist.name}/")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanged,
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.favorites), R.drawable.heart)
                    Item(1, stringResource(R.string.offline), R.drawable.airplane)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Favorites)
                        1 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Offline)
                    }
                }
            }
        }
    }
}
