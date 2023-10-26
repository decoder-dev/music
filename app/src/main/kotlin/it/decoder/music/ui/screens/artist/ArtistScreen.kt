package it.decoder.music.ui.screens.artist

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer
import it.decoder.compose.persist.PersistMapCleanup
import it.decoder.compose.persist.persist
import it.decoder.innertube.Innertube
import it.decoder.innertube.models.bodies.BrowseBody
import it.decoder.innertube.models.bodies.ContinuationBody
import it.decoder.innertube.requests.artistPage
import it.decoder.innertube.requests.itemsPage
import it.decoder.innertube.utils.from
import it.decoder.compose.routing.RouteHandler
import it.decoder.music.Database
import it.decoder.music.LocalPlayerServiceBinder
import it.decoder.music.R
import it.decoder.music.models.Artist
import it.decoder.music.query
import it.decoder.music.ui.components.LocalMenuState
import it.decoder.music.ui.components.themed.Header
import it.decoder.music.ui.components.themed.HeaderIconButton
import it.decoder.music.ui.components.themed.HeaderPlaceholder
import it.decoder.music.ui.components.themed.NonQueuedMediaItemMenu
import it.decoder.music.ui.components.themed.Scaffold
import it.decoder.music.ui.components.themed.adaptiveThumbnailContent
import it.decoder.music.ui.items.AlbumItem
import it.decoder.music.ui.items.AlbumItemPlaceholder
import it.decoder.music.ui.items.SongItem
import it.decoder.music.ui.items.SongItemPlaceholder
import it.decoder.music.ui.screens.albumRoute
import it.decoder.music.ui.screens.globalRoutes
import it.decoder.music.ui.screens.searchresult.ItemsPage
import it.decoder.music.ui.styling.Dimensions
import it.decoder.music.ui.styling.LocalAppearance
import it.decoder.music.ui.styling.px
import it.decoder.music.utils.artistScreenTabIndexKey
import it.decoder.music.utils.asMediaItem
import it.decoder.music.utils.forcePlay
import it.decoder.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun ArtistScreen(browseId: String) {
    val saveableStateHolder = rememberSaveableStateHolder()

    var tabIndex by rememberPreference(artistScreenTabIndexKey, defaultValue = 0)

    PersistMapCleanup(tagPrefix = "artist/$browseId/")

    var artist by persist<Artist?>("artist/$browseId/artist")

    var artistPage by persist<Innertube.ArtistPage?>("artist/$browseId/artistPage")

    LaunchedEffect(Unit) {
        Database
            .artist(browseId)
            .combine(snapshotFlow { tabIndex }.map { it != 4 }) { artist, mustFetch -> artist to mustFetch }
            .distinctUntilChanged()
            .collect { (currentArtist, mustFetch) ->
                artist = currentArtist

                if (artistPage == null && (currentArtist?.timestamp == null || mustFetch)) {
                    withContext(Dispatchers.IO) {
                        Innertube.artistPage(BrowseBody(browseId = browseId))
                            ?.onSuccess { currentArtistPage ->
                                artistPage = currentArtistPage

                                Database.upsert(
                                    Artist(
                                        id = browseId,
                                        name = currentArtistPage.name,
                                        thumbnailUrl = currentArtistPage.thumbnail?.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = currentArtist?.bookmarkedAt
                                    )
                                )
                            }
                    }
                }
            }
    }

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            val thumbnailContent =
                adaptiveThumbnailContent(
                    artist?.timestamp == null,
                    artist?.thumbnailUrl,
                    CircleShape
                )

            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit =
                { textButton ->
                    if (artist?.timestamp == null) {
                        HeaderPlaceholder(
                            modifier = Modifier
                                .shimmer()
                        )
                    } else {
                        val (colorPalette) = LocalAppearance.current
                        val context = LocalContext.current

                        Header(title = artist?.name ?: "Unknown") {
                            textButton?.invoke()

                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                            )

                            HeaderIconButton(
                                icon = if (artist?.bookmarkedAt == null) {
                                    R.drawable.bookmark_outline
                                } else {
                                    R.drawable.bookmark
                                },
                                color = colorPalette.accent,
                                onClick = {
                                    val bookmarkedAt =
                                        if (artist?.bookmarkedAt == null) System.currentTimeMillis() else null

                                    query {
                                        artist
                                            ?.copy(bookmarkedAt = bookmarkedAt)
                                            ?.let(Database::update)
                                    }
                                }
                            )

                            HeaderIconButton(
                                icon = R.drawable.share_social,
                                color = colorPalette.text,
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "https://music.youtube.com/channel/$browseId"
                                        )
                                    }

                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                }
                            )
                        }
                    }
                }

            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = { tabIndex = it },
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.overview), R.drawable.sparkles)
                    Item(1, stringResource(R.string.songs), R.drawable.musical_notes)
                    Item(2, stringResource(R.string.albums), R.drawable.disc)
                    Item(3, stringResource(R.string.singles), R.drawable.disc)
                    Item(4, stringResource(R.string.library), R.drawable.library)
                },
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> {
                            ArtistOverview(
                                youtubeArtistPage = artistPage,
                                thumbnailContent = thumbnailContent,
                                headerContent = headerContent,
                                onAlbumClick = { albumRoute(it) },
                                onViewAllSongsClick = { tabIndex = 1 },
                                onViewAllAlbumsClick = { tabIndex = 2 },
                                onViewAllSinglesClick = { tabIndex = 3 },
                            )
                        }

                        1 -> {
                            val binder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailSizeDp = Dimensions.thumbnails.song
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "artist/$browseId/songs",
                                headerContent = headerContent,
                                itemsPageProvider = artistPage?.let {
                                    ({ continuation ->
                                        continuation?.let {
                                            Innertube.itemsPage(
                                                body = ContinuationBody(continuation = continuation),
                                                fromMusicResponsiveListItemRenderer = Innertube.SongItem::from,
                                            )
                                        } ?: artistPage
                                            ?.songsEndpoint
                                            ?.takeIf { it.browseId != null }
                                            ?.let { endpoint ->
                                                Innertube.itemsPage(
                                                    body = BrowseBody(
                                                        browseId = endpoint.browseId!!,
                                                        params = endpoint.params,
                                                    ),
                                                    fromMusicResponsiveListItemRenderer = Innertube.SongItem::from,
                                                )
                                            }
                                        ?: Result.success(
                                            Innertube.ItemsPage(
                                                items = artistPage?.songs,
                                                continuation = null
                                            )
                                        )
                                    })
                                },
                                itemContent = { song ->
                                    SongItem(
                                        song = song,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        thumbnailSizePx = thumbnailSizePx,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        NonQueuedMediaItemMenu(
                                                            onDismiss = menuState::hide,
                                                            mediaItem = song.asMediaItem,
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlay(song.asMediaItem)
                                                    binder?.setupRadio(song.info?.endpoint)
                                                }
                                            )
                                    )
                                },
                                itemPlaceholderContent = {
                                    SongItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        2 -> {
                            val thumbnailSizeDp = 108.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "artist/$browseId/albums",
                                headerContent = headerContent,
                                emptyItemsText = stringResource(R.string.artist_no_release_album),
                                itemsPageProvider = artistPage?.let {
                                    ({ continuation ->
                                        continuation?.let {
                                            Innertube.itemsPage(
                                                body = ContinuationBody(continuation = continuation),
                                                fromMusicTwoRowItemRenderer = Innertube.AlbumItem::from,
                                            )
                                        } ?: artistPage
                                            ?.albumsEndpoint
                                            ?.takeIf { it.browseId != null }
                                            ?.let { endpoint ->
                                                Innertube.itemsPage(
                                                    body = BrowseBody(
                                                        browseId = endpoint.browseId!!,
                                                        params = endpoint.params,
                                                    ),
                                                    fromMusicTwoRowItemRenderer = Innertube.AlbumItem::from,
                                                )
                                            }
                                        ?: Result.success(
                                            Innertube.ItemsPage(
                                                items = artistPage?.albums,
                                                continuation = null
                                            )
                                        )
                                    })
                                },
                                itemContent = { album ->
                                    AlbumItem(
                                        album = album,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = { albumRoute(album.key) })
                                    )
                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        3 -> {
                            val thumbnailSizeDp = 108.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "artist/$browseId/singles",
                                headerContent = headerContent,
                                emptyItemsText = stringResource(R.string.artist_no_release_single),
                                itemsPageProvider = artistPage?.let {
                                    ({ continuation ->
                                        continuation?.let {
                                            Innertube.itemsPage(
                                                body = ContinuationBody(continuation = continuation),
                                                fromMusicTwoRowItemRenderer = Innertube.AlbumItem::from,
                                            )
                                        } ?: artistPage
                                            ?.singlesEndpoint
                                            ?.takeIf { it.browseId != null }
                                            ?.let { endpoint ->
                                                Innertube.itemsPage(
                                                    body = BrowseBody(
                                                        browseId = endpoint.browseId!!,
                                                        params = endpoint.params,
                                                    ),
                                                    fromMusicTwoRowItemRenderer = Innertube.AlbumItem::from,
                                                )
                                            }
                                        ?: Result.success(
                                            Innertube.ItemsPage(
                                                items = artistPage?.singles,
                                                continuation = null
                                            )
                                        )
                                    })
                                },
                                itemContent = { album ->
                                    AlbumItem(
                                        album = album,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = { albumRoute(album.key) })
                                    )
                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        4 -> {
                            ArtistLocalSongs(
                                browseId = browseId,
                                headerContent = headerContent,
                                thumbnailContent = thumbnailContent,
                            )
                        }
                    }
                }
            }
        }
    }
}
