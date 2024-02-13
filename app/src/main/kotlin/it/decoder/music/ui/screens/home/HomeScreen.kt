package it.decoder.music.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import it.decoder.music.models.SearchQuery
import it.decoder.music.models.toUiMood
import it.decoder.music.preferences.DataPreferences
import it.decoder.music.preferences.UIStatePreferences
import it.decoder.music.query
import it.decoder.music.ui.components.themed.Scaffold
import it.decoder.music.ui.screens.GlobalRoutes
import it.decoder.music.ui.screens.Route
import it.decoder.music.ui.screens.albumRoute
import it.decoder.music.ui.screens.artistRoute
import it.decoder.music.ui.screens.builtInPlaylistRoute
import it.decoder.music.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import it.decoder.music.ui.screens.localPlaylistRoute
import it.decoder.music.ui.screens.localplaylist.LocalPlaylistScreen
import it.decoder.music.ui.screens.moodRoute
import it.decoder.music.ui.screens.pipedPlaylistRoute
import it.decoder.music.ui.screens.playlistRoute
import it.decoder.music.ui.screens.search.SearchScreen
import it.decoder.music.ui.screens.searchResultRoute
import it.decoder.music.ui.screens.searchRoute
import it.decoder.music.ui.screens.searchresult.SearchResultScreen
import it.decoder.music.ui.screens.settings.SettingsScreen
import it.decoder.music.ui.screens.settingsRoute

@OptIn(ExperimentalAnimationApi::class)
@Route
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
        GlobalRoutes()

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
                onSearchAgain = { searchRoute(query) }
            )
        }

        searchRoute { initialTextInput ->
            SearchScreen(
                initialTextInput = initialTextInput,
                onSearch = { query ->
                    pop()
                    searchResultRoute(query)

                    if (!DataPreferences.pauseSearchHistory) query {
                        Database.insert(SearchQuery(query = query))
                    }
                },
                onViewPlaylist = onPlaylistUrl
            )
        }

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.settings,
                onTopIconButtonClick = { settingsRoute() },
                tabIndex = UIStatePreferences.homeScreenTabIndex,
                onTabChanged = { UIStatePreferences.homeScreenTabIndex = it },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.quick_picks), R.drawable.sparkles)
                    item(1, stringResource(R.string.discover), R.drawable.globe)
                    item(2, stringResource(R.string.songs), R.drawable.musical_notes)
                    item(3, stringResource(R.string.playlists), R.drawable.playlist)
                    item(4, stringResource(R.string.artists), R.drawable.person)
                    item(5, stringResource(R.string.albums), R.drawable.disc)
                    item(6, stringResource(R.string.local), R.drawable.download)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    val onSearchClick = { searchRoute("") }
                    when (currentTabIndex) {
                        0 -> QuickPicks(
                            onAlbumClick = { albumRoute(it) },
                            onArtistClick = { artistRoute(it) },
                            onPlaylistClick = { playlistRoute(it) },
                            onSearchClick = onSearchClick
                        )

                        1 -> HomeDiscovery(
                            onMoodClick = { mood -> moodRoute(mood.toUiMood()) },
                            onNewReleaseAlbumClick = { albumRoute(it) },
                            onSearchClick = onSearchClick
                        )

                        2 -> HomeSongs(
                            onSearchClick = onSearchClick
                        )

                        3 -> HomePlaylists(
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onPipedPlaylistClick = { session, playlist ->
                                pipedPlaylistRoute(
                                    p0 = session.apiBaseUrl.toString(),
                                    p1 = session.token,
                                    p2 = playlist.id.toString()
                                )
                            },
                            onSearchClick = onSearchClick
                        )

                        4 -> HomeArtistList(
                            onArtistClick = { artistRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        5 -> HomeAlbums(
                            onAlbumClick = { albumRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        6 -> HomeLocalSongs(
                            onSearchClick = onSearchClick
                        )
                    }
                }
            }
        }
    }
}
