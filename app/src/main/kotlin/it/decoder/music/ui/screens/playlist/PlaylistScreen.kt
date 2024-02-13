package it.decoder.music.ui.screens.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import it.decoder.compose.persist.PersistMapCleanup
import it.decoder.compose.routing.RouteHandler
import it.decoder.music.R
import it.decoder.music.ui.components.themed.Scaffold
import it.decoder.music.ui.screens.GlobalRoutes
import it.decoder.music.ui.screens.Route

@Route
@Composable
fun PlaylistScreen(
    browseId: String,
    params: String?,
    maxDepth: Int? = null
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    PersistMapCleanup(prefix = "playlist/$browseId")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChanged = { },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> PlaylistSongList(
                            browseId = browseId,
                            params = params,
                            maxDepth = maxDepth
                        )
                    }
                }
            }
        }
    }
}
