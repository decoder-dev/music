package it.decoder.music.ui.screens.album

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.valentinilk.shimmer.shimmer
import it.decoder.compose.persist.PersistMapCleanup
import it.decoder.compose.persist.persist
import it.decoder.compose.routing.RouteHandler
import it.decoder.innertube.Innertube
import it.decoder.innertube.models.bodies.BrowseBody
import it.decoder.innertube.requests.albumPage
import it.decoder.music.Database
import it.decoder.music.R
import it.decoder.music.models.Album
import it.decoder.music.models.SongAlbumMap
import it.decoder.music.query
import it.decoder.music.ui.components.themed.Header
import it.decoder.music.ui.components.themed.HeaderIconButton
import it.decoder.music.ui.components.themed.HeaderPlaceholder
import it.decoder.music.ui.components.themed.PlaylistInfo
import it.decoder.music.ui.components.themed.Scaffold
import it.decoder.music.ui.components.themed.adaptiveThumbnailContent
import it.decoder.music.ui.items.AlbumItem
import it.decoder.music.ui.items.AlbumItemPlaceholder
import it.decoder.music.ui.screens.GlobalRoutes
import it.decoder.music.ui.screens.Route
import it.decoder.music.ui.screens.albumRoute
import it.decoder.music.ui.screens.searchresult.ItemsPage
import it.decoder.music.ui.styling.Dimensions
import it.decoder.music.ui.styling.LocalAppearance
import it.decoder.music.utils.asMediaItem
import it.decoder.music.utils.stateFlowSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Route
@Composable
fun AlbumScreen(browseId: String) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val tabIndexState = rememberSaveable(saver = stateFlowSaver()) { MutableStateFlow(0) }
    val tabIndex by tabIndexState.collectAsState()

    var album by persist<Album?>("album/$browseId/album")
    var albumPage by persist<Innertube.PlaylistOrAlbumPage?>("album/$browseId/albumPage")

    PersistMapCleanup(prefix = "album/$browseId/")

    LaunchedEffect(Unit) {
        Database
            .album(browseId)
            .combine(tabIndexState) { album, tabIndex -> album to tabIndex }
            .collect { (currentAlbum, tabIndex) ->
                album = currentAlbum

                if (albumPage == null && (currentAlbum?.timestamp == null || tabIndex == 1))
                    withContext(Dispatchers.IO) {
                        Innertube.albumPage(BrowseBody(browseId = browseId))
                            ?.onSuccess { currentAlbumPage ->
                                albumPage = currentAlbumPage

                                Database.clearAlbum(browseId)

                                Database.upsert(
                                    album = Album(
                                        id = browseId,
                                        title = currentAlbumPage.title,
                                        description = currentAlbumPage.description,
                                        thumbnailUrl = currentAlbumPage.thumbnail?.url,
                                        year = currentAlbumPage.year,
                                        authorsText = currentAlbumPage.authors
                                            ?.joinToString("") { it.name.orEmpty() },
                                        shareUrl = currentAlbumPage.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = album?.bookmarkedAt,
                                        otherInfo = currentAlbumPage.otherInfo
                                    ),
                                    songAlbumMaps = currentAlbumPage
                                        .songsPage
                                        ?.items
                                        ?.map(Innertube.SongItem::asMediaItem)
                                        ?.onEach(Database::insert)
                                        ?.mapIndexed { position, mediaItem ->
                                            SongAlbumMap(
                                                songId = mediaItem.mediaId,
                                                albumId = browseId,
                                                position = position
                                            )
                                        } ?: emptyList()
                                )
                            }
                    }
            }
    }

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            val headerContent: @Composable (
                beforeContent: (@Composable () -> Unit)?,
                afterContent: (@Composable () -> Unit)?
            ) -> Unit = { beforeContent, afterContent ->
                if (album?.timestamp == null) HeaderPlaceholder(modifier = Modifier.shimmer())
                else {
                    val (colorPalette) = LocalAppearance.current
                    val context = LocalContext.current

                    Header(title = album?.title ?: stringResource(R.string.unknown)) {
                        beforeContent?.invoke()

                        Spacer(modifier = Modifier.weight(1f))

                        afterContent?.invoke()

                        HeaderIconButton(
                            icon = if (album?.bookmarkedAt == null) R.drawable.bookmark_outline
                            else R.drawable.bookmark,
                            color = colorPalette.accent,
                            onClick = {
                                val bookmarkedAt =
                                    if (album?.bookmarkedAt == null) System.currentTimeMillis() else null

                                query {
                                    album
                                        ?.copy(bookmarkedAt = bookmarkedAt)
                                        ?.let(Database::update)
                                }
                            }
                        )

                        HeaderIconButton(
                            icon = R.drawable.share_social,
                            color = colorPalette.text,
                            onClick = {
                                album?.shareUrl?.let { url ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, url)
                                    }

                                    context.startActivity(
                                        Intent.createChooser(sendIntent, null)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            val thumbnailContent = adaptiveThumbnailContent(
                isLoading = album?.timestamp == null,
                url = album?.thumbnailUrl
            )

            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = { newTab -> tabIndexState.update { newTab } },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                    item(1, stringResource(R.string.other_versions), R.drawable.disc)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> AlbumSongs(
                            browseId = browseId,
                            headerContent = headerContent,
                            thumbnailContent = thumbnailContent,
                            afterHeaderContent = {
                                if (album == null) PlaylistInfo(playlist = albumPage)
                                else PlaylistInfo(playlist = album)
                            }
                        )

                        1 -> {
                            ItemsPage(
                                tag = "album/$browseId/alternatives",
                                header = headerContent,
                                initialPlaceholderCount = 1,
                                continuationPlaceholderCount = 1,
                                emptyItemsText = stringResource(R.string.no_alternative_version),
                                provider = albumPage?.let {
                                    {
                                        Result.success(
                                            Innertube.ItemsPage(
                                                items = albumPage?.otherVersions,
                                                continuation = null
                                            )
                                        )
                                    }
                                },
                                itemContent = { album ->
                                    AlbumItem(
                                        album = album,
                                        thumbnailSize = Dimensions.thumbnails.album,
                                        modifier = Modifier.clickable { albumRoute(album.key) }
                                    )
                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
