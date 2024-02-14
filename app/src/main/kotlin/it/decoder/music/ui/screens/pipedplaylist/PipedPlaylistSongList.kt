package it.decoder.music.ui.screens.pipedplaylist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.valentinilk.shimmer.shimmer
import it.decoder.compose.persist.persist
import it.decoder.music.LocalPlayerAwareWindowInsets
import it.decoder.music.LocalPlayerServiceBinder
import it.decoder.music.R
import it.decoder.music.ui.components.LocalMenuState
import it.decoder.music.ui.components.ShimmerHost
import it.decoder.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.decoder.music.ui.components.themed.Header
import it.decoder.music.ui.components.themed.HeaderPlaceholder
import it.decoder.music.ui.components.themed.LayoutWithAdaptiveThumbnail
import it.decoder.music.ui.components.themed.NonQueuedMediaItemMenu
import it.decoder.music.ui.components.themed.SecondaryTextButton
import it.decoder.music.ui.components.themed.adaptiveThumbnailContent
import it.decoder.music.ui.items.SongItem
import it.decoder.music.ui.items.SongItemPlaceholder
import it.decoder.music.ui.styling.Dimensions
import it.decoder.music.ui.styling.LocalAppearance
import it.decoder.music.utils.asMediaItem
import it.decoder.music.utils.enqueue
import it.decoder.music.utils.forcePlayAtIndex
import it.decoder.music.utils.forcePlayFromBeginning
import it.decoder.music.utils.isLandscape
import it.decoder.piped.Piped
import it.decoder.piped.models.Playlist
import it.decoder.piped.models.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PipedPlaylistSongList(
    session: Session,
    playlistId: UUID,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    var playlist by persist<Playlist>(tag = "pipedplaylist/$playlistId/playlistPage")

    LaunchedEffect(Unit) {
        playlist = withContext(Dispatchers.IO) {
            Piped.playlist.songs(
                session = session,
                id = playlistId
            )?.getOrNull()
        }
    }

    val lazyListState = rememberLazyListState()

    val thumbnailContent = adaptiveThumbnailContent(
        isLoading = playlist == null,
        url = playlist?.thumbnailUrl?.toString()
    )

    LayoutWithAdaptiveThumbnail(
        thumbnailContent = thumbnailContent,
        modifier = modifier
    ) {
        Box {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (playlist == null) HeaderPlaceholder(modifier = Modifier.shimmer())
                        else Header(title = playlist?.name ?: stringResource(R.string.unknown)) {
                            SecondaryTextButton(
                                text = stringResource(R.string.enqueue),
                                enabled = playlist?.videos?.isNotEmpty() == true,
                                onClick = {
                                    playlist?.videos?.mapNotNull(Playlist.Video::asMediaItem)
                                        ?.let { mediaItems -> binder?.player?.enqueue(mediaItems) }
                                }
                            )
                        }

                        if (!isLandscape) thumbnailContent()
                    }
                }

                itemsIndexed(items = playlist?.videos ?: emptyList()) { index, song ->
                    song.asMediaItem?.let { mediaItem ->
                        SongItem(
                            song = mediaItem,
                            thumbnailSize = Dimensions.thumbnails.song,
                            modifier = Modifier.combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            onDismiss = menuState::hide,
                                            mediaItem = mediaItem
                                        )
                                    }
                                },
                                onClick = {
                                    playlist?.videos?.mapNotNull(Playlist.Video::asMediaItem)
                                        ?.let { mediaItems ->
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(mediaItems, index)
                                        }
                                }
                            )
                        )
                    }
                }

                if (playlist == null) item(key = "loading") {
                    ShimmerHost(modifier = Modifier.fillParentMaxSize()) {
                        repeat(4) {
                            SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = R.drawable.shuffle,
                onClick = {
                    playlist?.videos?.let { songs ->
                        if (songs.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(
                                songs.shuffled().mapNotNull(Playlist.Video::asMediaItem)
                            )
                        }
                    }
                }
            )
        }
    }
}
