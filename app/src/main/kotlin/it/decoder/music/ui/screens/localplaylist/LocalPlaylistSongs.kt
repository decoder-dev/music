package it.decoder.music.ui.screens.localplaylist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.decoder.compose.persist.persist
import it.decoder.compose.reordering.animateItemPlacement
import it.decoder.compose.reordering.draggedItem
import it.decoder.compose.reordering.rememberReorderingState
import it.decoder.innertube.Innertube
import it.decoder.innertube.models.bodies.BrowseBody
import it.decoder.innertube.requests.playlistPage
import it.decoder.music.Database
import it.decoder.music.LocalPlayerAwareWindowInsets
import it.decoder.music.LocalPlayerServiceBinder
import it.decoder.music.R
import it.decoder.music.models.Playlist
import it.decoder.music.models.Song
import it.decoder.music.models.SongPlaylistMap
import it.decoder.music.query
import it.decoder.music.transaction
import it.decoder.music.ui.components.LocalMenuState
import it.decoder.music.ui.components.themed.ConfirmationDialog
import it.decoder.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.decoder.music.ui.components.themed.Header
import it.decoder.music.ui.components.themed.HeaderIconButton
import it.decoder.music.ui.components.themed.InPlaylistMediaItemMenu
import it.decoder.music.ui.components.themed.Menu
import it.decoder.music.ui.components.themed.MenuEntry
import it.decoder.music.ui.components.themed.ReorderHandle
import it.decoder.music.ui.components.themed.SecondaryTextButton
import it.decoder.music.ui.components.themed.TextFieldDialog
import it.decoder.music.ui.items.SongItem
import it.decoder.music.ui.styling.Dimensions
import it.decoder.music.ui.styling.LocalAppearance
import it.decoder.music.utils.PlaylistDownloadIcon
import it.decoder.music.utils.asMediaItem
import it.decoder.music.utils.completed
import it.decoder.music.utils.enqueue
import it.decoder.music.utils.forcePlayAtIndex
import it.decoder.music.utils.forcePlayFromBeginning
import it.decoder.music.utils.launchYouTubeMusic
import it.decoder.music.utils.toast
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalPlaylistSongs(
    playlistId: Long,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var playlist by persist<Playlist?>("localPlaylist/$playlistId/playlist")
    var songs by persist<List<Song>?>("localPlaylist/$playlistId/Songs")

    LaunchedEffect(Unit) {
        Database
            .playlist(playlistId)
            .filterNotNull()
            .distinctUntilChanged()
            .collect { playlist = it }
    }

    LaunchedEffect(Unit) {
        Database
            .playlistSongs(playlistId)
            .filterNotNull()
            .distinctUntilChanged()
            .collect { songs = it }
    }

    val lazyListState = rememberLazyListState()

    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = songs ?: emptyList<Any>(),
        onDragEnd = { fromIndex, toIndex ->
            query {
                Database.move(playlistId, fromIndex, toIndex)
            }
        },
        extraItemCount = 1
    )

    var isRenaming by rememberSaveable { mutableStateOf(false) }

    if (isRenaming) TextFieldDialog(
        hintText = stringResource(R.string.enter_playlist_name_prompt),
        initialTextInput = playlist?.name.orEmpty(),
        onDismiss = { isRenaming = false },
        onDone = { text ->
            query {
                playlist?.copy(name = text)?.let(Database::update)
            }
        }
    )

    var isDeleting by rememberSaveable { mutableStateOf(false) }

    if (isDeleting) ConfirmationDialog(
        text = stringResource(R.string.confirm_delete_playlist),
        onDismiss = { isDeleting = false },
        onConfirm = {
            query {
                playlist?.let(Database::delete)
            }
            onDelete()
        }
    )

    Box(modifier = modifier) {
        LookaheadScope {
            LazyColumn(
                state = reorderingState.lazyListState,
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
                    Header(
                        title = playlist?.name
                            ?: stringResource(R.string.unknown),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        SecondaryTextButton(
                            text = stringResource(R.string.enqueue),
                            enabled = songs?.isNotEmpty() == true,
                            onClick = {
                                songs?.map(Song::asMediaItem)
                                    ?.let { mediaItems ->
                                        binder?.player?.enqueue(mediaItems)
                                    }
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        songs?.map(Song::asMediaItem)
                            ?.let { PlaylistDownloadIcon(songs = it.toImmutableList()) }

                        HeaderIconButton(
                            icon = R.drawable.ellipsis_horizontal,
                            color = colorPalette.text,
                            onClick = {
                                menuState.display {
                                    Menu {
                                        playlist?.browseId?.let { browseId ->
                                            MenuEntry(
                                                icon = R.drawable.sync,
                                                text = stringResource(R.string.sync),
                                                onClick = {
                                                    menuState.hide()
                                                    transaction {
                                                        runBlocking(Dispatchers.IO) {
                                                            Innertube.playlistPage(
                                                                BrowseBody(
                                                                    browseId = browseId
                                                                )
                                                            )?.completed()
                                                        }?.getOrNull()?.let { remotePlaylist ->
                                                            Database.clearPlaylist(playlistId)

                                                            remotePlaylist.songsPage
                                                                ?.items
                                                                ?.map(Innertube.SongItem::asMediaItem)
                                                                ?.onEach(Database::insert)
                                                                ?.mapIndexed { position, mediaItem ->
                                                                    SongPlaylistMap(
                                                                        songId = mediaItem.mediaId,
                                                                        playlistId = playlistId,
                                                                        position = position
                                                                    )
                                                                }
                                                                ?.let(Database::insertSongPlaylistMaps)
                                                        }
                                                    }
                                                }
                                            )

                                            songs?.firstOrNull()?.id?.let { firstSongId ->
                                                MenuEntry(
                                                    icon = R.drawable.play,
                                                    text = stringResource(R.string.watch_playlist_on_youtube),
                                                    onClick = {
                                                        menuState.hide()
                                                        binder?.player?.pause()
                                                        uriHandler.openUri(
                                                            "https://youtube.com/watch?v=$firstSongId&list=${
                                                                playlist?.browseId
                                                                    ?.drop(2)
                                                            }"
                                                        )
                                                    }
                                                )

                                                MenuEntry(
                                                    icon = R.drawable.musical_notes,
                                                    text = stringResource(R.string.open_in_youtube_music),
                                                    onClick = {
                                                        menuState.hide()
                                                        binder?.player?.pause()
                                                        if (
                                                            !launchYouTubeMusic(
                                                                context = context,
                                                                endpoint = "watch?v=$firstSongId&list=${
                                                                    playlist?.browseId
                                                                        ?.drop(2)
                                                                }"
                                                            )
                                                        ) context.toast(
                                                            context.getString(R.string.youtube_music_not_installed)
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        MenuEntry(
                                            icon = R.drawable.pencil,
                                            text = stringResource(R.string.rename),
                                            onClick = {
                                                menuState.hide()
                                                isRenaming = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.trash,
                                            text = stringResource(R.string.delete),
                                            onClick = {
                                                menuState.hide()
                                                isDeleting = true
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                itemsIndexed(
                    items = songs ?: emptyList(),
                    key = { _, song -> song.id },
                    contentType = { _, song -> song }
                ) { index, song ->
                    SongItem(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        InPlaylistMediaItemMenu(
                                            playlistId = playlistId,
                                            positionInPlaylist = index,
                                            song = song,
                                            onDismiss = menuState::hide
                                        )
                                    }
                                },
                                onClick = {
                                    songs
                                        ?.map(Song::asMediaItem)
                                        ?.let { mediaItems ->
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(mediaItems, index)
                                        }
                                }
                            )
                            .animateItemPlacement(reorderingState)
                            .draggedItem(
                                reorderingState = reorderingState,
                                index = index
                            )
                            .background(colorPalette.background0),
                        song = song,
                        thumbnailSize = Dimensions.thumbnails.song
                    ) {
                        ReorderHandle(
                            reorderingState = reorderingState,
                            index = index
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.shuffle,
            visible = !reorderingState.isDragging,
            onClick = {
                songs?.let { songs ->
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(
                            songs.shuffled().map(Song::asMediaItem)
                        )
                    }
                }
            }
        )
    }
}
