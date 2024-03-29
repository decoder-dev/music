package it.decoder.music.utils

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import it.decoder.music.Database
import it.decoder.music.LocalPlayerServiceBinder
import it.decoder.music.R
import it.decoder.music.models.Format
import it.decoder.music.service.PrecacheService
import it.decoder.music.service.downloadState
import it.decoder.music.ui.components.themed.HeaderIconButton
import it.decoder.music.ui.styling.LocalAppearance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PlaylistDownloadIcon(
    songs: ImmutableList<MediaItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    val isDownloading by downloadState.collectAsState()

    if (
        !songs.all {
            isCached(
                mediaId = it.mediaId,
                key = isDownloading
            )
        }
    ) HeaderIconButton(
        icon = R.drawable.download,
        color = colorPalette.text,
        onClick = {
            songs.forEach {
                PrecacheService.scheduleCache(context.applicationContext, it)
            }
        },
        modifier = modifier
    )
}

@OptIn(UnstableApi::class)
@Composable
fun isCached(
    mediaId: String,
    key: Any? = Unit
): Boolean {
    val cache = LocalPlayerServiceBinder.current?.cache ?: return false
    var format: Format? by remember { mutableStateOf(null) }

    LaunchedEffect(mediaId, key) {
        Database.format(mediaId).distinctUntilChanged().collect { format = it }
    }

    return remember(key) {
        format?.contentLength?.let { len ->
            cache.isCached(mediaId, 0, len)
        } ?: false
    }
}
