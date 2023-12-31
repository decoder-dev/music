package it.decoder.music.ui.screens.player


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.Log
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.valentinilk.shimmer.shimmer
import it.decoder.compose.reordering.ReorderingLazyColumn
import it.decoder.compose.reordering.animateItemPlacement
import it.decoder.compose.reordering.draggedItem
import it.decoder.compose.reordering.rememberReorderingState
import it.decoder.compose.reordering.reorder
import it.decoder.music.LocalPlayerServiceBinder
import it.decoder.music.R
import it.decoder.music.service.LocalDownloadService
import it.decoder.music.ui.components.BottomSheet
import it.decoder.music.ui.components.BottomSheetState
import it.decoder.music.ui.components.LocalMenuState
import it.decoder.music.ui.components.MusicBars
import it.decoder.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.decoder.music.ui.components.themed.IconButton
import it.decoder.music.ui.components.themed.QueuedMediaItemMenu
import it.decoder.music.ui.items.SongItem
import it.decoder.music.ui.items.SongItemPlaceholder
import it.decoder.music.ui.styling.Dimensions
import it.decoder.music.ui.styling.LocalAppearance
import it.decoder.music.ui.styling.onOverlay
import it.decoder.music.ui.styling.primaryButton
import it.decoder.music.ui.styling.px
import it.decoder.music.utils.DisposableListener
import it.decoder.music.utils.medium
import it.decoder.music.utils.queueLoopEnabledKey
import it.decoder.music.utils.rememberPreference
import it.decoder.music.utils.shouldBePlaying
import it.decoder.music.utils.shuffleQueue
import it.decoder.music.utils.smoothScrollToTop
import it.decoder.music.utils.windows
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun Queue(
    backgroundColorProvider: () -> Color,
    layoutState: BottomSheetState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current

    val windowInsets = WindowInsets.systemBars

    val horizontalBottomPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()

    val context = LocalContext.current

    BottomSheet(
        state = layoutState,
        modifier = modifier,
        collapsedContent = {
            Box(
                modifier = Modifier
                    .drawBehind { drawRect(backgroundColorProvider()) }
                    .fillMaxSize()
                    .padding(horizontalBottomPaddingValues)
            ) {
                Image(
                    painter = painterResource(R.drawable.playlist),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.text),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(18.dp)
                )

                content()
            }
        }
    ) {
        val binder = LocalPlayerServiceBinder.current

        binder?.player ?: return@BottomSheet

        val player = binder.player

        var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = true)

        val menuState = LocalMenuState.current

        val thumbnailSizeDp = Dimensions.thumbnails.song
        val thumbnailSizePx = thumbnailSizeDp.px

        var mediaItemIndex by remember {
            mutableStateOf(if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex)
        }

        var windows by remember {
            mutableStateOf(player.currentTimeline.windows)
        }

        var shouldBePlaying by remember {
            mutableStateOf(binder.player.shouldBePlaying)
        }

        player.DisposableListener {
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItemIndex =
                        if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    windows = timeline.windows
                    mediaItemIndex =
                        if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    shouldBePlaying = binder.player.shouldBePlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    shouldBePlaying = binder.player.shouldBePlaying
                }
            }
        }

        val reorderingState = rememberReorderingState(
            lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = mediaItemIndex),
            key = windows,
            onDragEnd = player::moveMediaItem,
            extraItemCount = 0
        )

        val rippleIndication = rememberRipple(bounded = false)

        val musicBarsTransition = updateTransition(targetState = mediaItemIndex, label = "")


        Column {
            Box(
                modifier = Modifier
                    .background(colorPalette.background1)
                    .weight(1f)
            ) {

                ReorderingLazyColumn(
                    reorderingState = reorderingState,
                    contentPadding = windowInsets
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .asPaddingValues(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .nestedScroll(layoutState.preUpPostDownNestedScrollConnection)

                ) {
                    items(
                        items = windows,
                        key = { it.uid.hashCode() }
                    ) { window ->
                        val isPlayingThisMediaItem = mediaItemIndex == window.firstPeriodIndex

                        SongItem(
                            song = window.mediaItem,
                            thumbnailSizePx = thumbnailSizePx,
                            thumbnailSizeDp = thumbnailSizeDp,
                            onThumbnailContent = {
                                musicBarsTransition.AnimatedVisibility(
                                    visible = { it == window.firstPeriodIndex },
                                    enter = fadeIn(tween(800)),
                                    exit = fadeOut(tween(800)),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(
                                                color = Color.Black.copy(alpha = 0.25f),
                                                shape = thumbnailShape
                                            )
                                            .size(Dimensions.thumbnails.song)
                                    ) {
                                        if (shouldBePlaying) {
                                            MusicBars(
                                                color = colorPalette.onOverlay,
                                                modifier = Modifier
                                                    .height(24.dp)
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(R.drawable.play),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(colorPalette.onOverlay),
                                                modifier = Modifier
                                                    .size(24.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            trailingContent = {
                                IconButton(
                                    icon = R.drawable.reorder,
                                    color = colorPalette.textDisabled,
                                    indication = rippleIndication,
                                    onClick = {},
                                    modifier = Modifier
                                        .reorder(
                                            reorderingState = reorderingState,
                                            index = window.firstPeriodIndex
                                        )
                                        .size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            QueuedMediaItemMenu(
                                                mediaItem = window.mediaItem,
                                                indexInQueue = if (isPlayingThisMediaItem) null else window.firstPeriodIndex,
                                                onDismiss = menuState::hide,
                                                onDownload = {
                                                    Log.d("downloadEvent","Download started from Queue?")
                                                    val contentUri = "https://www.youtube.com/watch?v=${window.mediaItem.mediaId}".toUri()
                                                    val downloadRequest = DownloadRequest.Builder(window.mediaItem.mediaId, contentUri).build()

                                                    DownloadService.sendAddDownload(
                                                        context,
                                                        LocalDownloadService::class.java,
                                                        downloadRequest,
                                                        /* foreground= */ false
                                                    )

                                                    DownloadService.sendSetStopReason(
                                                        context,
                                                        LocalDownloadService::class.java,
                                                        window.mediaItem.mediaId,
                                                        Download.STOP_REASON_NONE,
                                                        /* foreground= */ false
                                                    )

                                                    DownloadService.start(
                                                        context,
                                                        LocalDownloadService::class.java
                                                    )

                                                }
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (isPlayingThisMediaItem) {
                                            if (shouldBePlaying) {
                                                player.pause()
                                            } else {
                                                player.play()
                                            }
                                        } else {
                                            player.seekToDefaultPosition(window.firstPeriodIndex)
                                            player.playWhenReady = true
                                        }
                                    }
                                )
                                .animateItemPlacement(reorderingState = reorderingState)
                                .draggedItem(
                                    reorderingState = reorderingState,
                                    index = window.firstPeriodIndex
                                )
                        )
                    }

                    item {
                        if (binder.isLoadingRadio) {
                            Column(
                                modifier = Modifier
                                    .shimmer()
                            ) {
                                repeat(3) { index ->
                                    SongItemPlaceholder(
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .alpha(1f - index * 0.125f)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                FloatingActionsContainerWithScrollToTop(
                    lazyListState = reorderingState.lazyListState,
                    iconId = R.drawable.shuffle,
                    visible = !reorderingState.isDragging,
                    windowInsets = windowInsets.only(WindowInsetsSides.Horizontal),
                    onClick = {
                        reorderingState.coroutineScope.launch {
                            reorderingState.lazyListState.smoothScrollToTop()
                        }.invokeOnCompletion {
                            player.shuffleQueue()
                        }
                    }
                )
            }


            Box(
                modifier = Modifier
                    .clickable(onClick = layoutState::collapseSoft)
                    .background(colorPalette.background2)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(horizontalBottomPaddingValues)
                    .height(64.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.chevron_down),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.text),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(18.dp)
                        //.padding(all = 10.dp)
                )

                IconButton(
                    icon = R.drawable.trash,
                    color = colorPalette.text,
                    onClick = {
                        val mediacount = binder.player.mediaItemCount-1
                        for (i in mediacount.downTo(0)) {
                            if (i == mediaItemIndex) null else binder.player.removeMediaItem(i)
                        }

                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .size(18.dp)
                )

                BasicText(
                    text = "${binder.player.mediaItemCount} " + stringResource(R.string.songs) + " " + stringResource(R.string.on_queue),
                    style = typography.xxs.medium,
                    modifier = Modifier
                        .background(
                            color = colorPalette.background1,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .align(Alignment.BottomCenter)
                        //.padding(all = 8.dp)
                        //.offset(x = 25.dp)
                        //.paddingFromBaseline(top = 50.dp)

                )



                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { queueLoopEnabled = !queueLoopEnabled }
                        .background(colorPalette.primaryButton)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.CenterEnd)
                        .animateContentSize()
                ) {
                   /* BasicText(
                        text = stringResource(R.string.queue_loop),
                        style = typography.xxs.medium,
                    )*/
                    Image(
                        painter = painterResource(R.drawable.infinite),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.text),
                        modifier = Modifier
                            //.align(Alignment.Center)
                            .size(18.dp)

                    )

                    AnimatedContent(
                        targetState = queueLoopEnabled,
                        transitionSpec = {
                            val slideDirection = if (targetState) AnimatedContentScope.SlideDirection.Up else AnimatedContentScope.SlideDirection.Down

                            ContentTransform(
                                targetContentEnter = slideIntoContainer(slideDirection) + fadeIn(),
                                initialContentExit = slideOutOfContainer(slideDirection) + fadeOut(),
                            )
                        }
                    ) {
                        BasicText(
                            text = if (it) " on" else " off",
                            style = typography.xxs.medium,
                        )
                    }
                }
            }
        }
    }
}
