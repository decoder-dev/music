package it.decoder.music.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import it.decoder.compose.persist.PersistMapCleanup
import it.decoder.compose.routing.RouteHandler
import it.decoder.compose.routing.defaultStacking
import it.decoder.compose.routing.defaultStill
import it.decoder.compose.routing.defaultUnstacking
import it.decoder.compose.routing.isStacking
import it.decoder.compose.routing.isUnknown
import it.decoder.compose.routing.isUnstacking
import it.decoder.music.Database
import it.decoder.music.R
import it.decoder.music.enums.StatisticsType
import it.decoder.music.models.SearchQuery
import it.decoder.music.query
import it.decoder.music.ui.components.themed.Scaffold
import it.decoder.music.ui.screens.albumRoute
import it.decoder.music.ui.screens.artistRoute
import it.decoder.music.ui.screens.builtInPlaylistRoute
import it.decoder.music.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import it.decoder.music.ui.screens.globalRoutes
import it.decoder.music.ui.screens.localPlaylistRoute
import it.decoder.music.ui.screens.localplaylist.LocalPlaylistScreen
import it.decoder.music.ui.screens.playlistRoute
import it.decoder.music.ui.screens.quickpicksRoute
import it.decoder.music.ui.screens.search.SearchScreen
import it.decoder.music.ui.screens.searchResultRoute
import it.decoder.music.ui.screens.searchRoute
import it.decoder.music.ui.screens.searchresult.SearchResultScreen
import it.decoder.music.ui.screens.settings.SettingsScreen
import it.decoder.music.ui.screens.settingsRoute
import it.decoder.music.ui.screens.statisticsTypeRoute
import it.decoder.music.utils.homeScreenTabIndexKey
import it.decoder.music.utils.pauseSearchHistoryKey
import it.decoder.music.utils.preferences
import it.decoder.music.utils.rememberPreference

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeScreen(onPlaylistUrl: (String) -> Unit) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup("home/")

    RouteHandler(
        listenToGlobalEmitter = true,
        transitionSpec = {
            when {
                isStacking -> defaultStacking
                isUnstacking -> defaultUnstacking
                isUnknown -> when {
                    initialState.route == searchRoute && targetState.route == searchResultRoute -> defaultStacking
                    initialState.route == searchResultRoute && targetState.route == searchRoute -> defaultUnstacking
                    else -> defaultStill
                }

                else -> defaultStill
            }
        }
    ) {
        globalRoutes()

        settingsRoute {
            SettingsScreen()
        }

        localPlaylistRoute { playlistId ->
            LocalPlaylistScreen(
                playlistId = playlistId ?: error("playlistId cannot be null")
            )
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(
                builtInPlaylist = builtInPlaylist
            )
        }

        searchResultRoute { query ->
            SearchResultScreen(
                query = query,
                onSearchAgain = {
                    searchRoute(query)
                }
            )
        }

        searchRoute { initialTextInput ->
            val context = LocalContext.current

            SearchScreen(
                initialTextInput = initialTextInput,
                onSearch = { query ->
                    pop()
                    searchResultRoute(query)

                    if (!context.preferences.getBoolean(pauseSearchHistoryKey, false)) {
                        query {
                            Database.insert(SearchQuery(query = query))
                        }
                    }
                },
                onViewPlaylist = onPlaylistUrl
            )
        }

        host {
            val (tabIndex, onTabChanged) = rememberPreference(
                homeScreenTabIndexKey,
                defaultValue = 0
            )

            Scaffold(
                topIconButtonId = R.drawable.equalizer,
                onTopIconButtonClick = { settingsRoute() },
                //onTopIconButtonClick = { searchRoute("") },
                tabIndex = tabIndex,
                onTabChanged = onTabChanged,
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.quick_picks), R.drawable.sparkles)
                    Item(1, stringResource(R.string.songs), R.drawable.musical_notes)
                    Item(2, stringResource(R.string.playlists), R.drawable.playlist)
                    Item(3, stringResource(R.string.artists), R.drawable.person)
                    Item(4, stringResource(R.string.albums), R.drawable.disc)
                    //Item(5, "Statistics", R.drawable.query_stats)
                    //Item(6, "Settings", R.drawable.equalizer)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> QuickPicks(
                            onAlbumClick = { albumRoute(it) },
                            onArtistClick = { artistRoute(it) },
                            onPlaylistClick = { playlistRoute(it) },
                            onSearchClick = { searchRoute("") }
                        )

                        1 -> HomeSongs(
                            onSearchClick = { searchRoute("") }
                        )

                        2 -> HomePlaylists(
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )

                        3 -> HomeArtistList(
                            onArtistClick = { artistRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )

                        4 -> HomeAlbums(
                            onAlbumClick = { albumRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )
/*
                        5 -> HomeStatistics(
                            onStatisticsType = { statisticsTypeRoute(it)},
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )
*/
                        //6 -> settingsRoute()
                    }
                }
            }
        }
    }
}
