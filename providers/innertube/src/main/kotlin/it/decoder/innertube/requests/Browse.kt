package it.decoder.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.decoder.extensions.runCatchingCancellable
import it.decoder.innertube.Innertube
import it.decoder.innertube.models.BrowseResponse
import it.decoder.innertube.models.MusicTwoRowItemRenderer
import it.decoder.innertube.models.bodies.BrowseBody
import it.decoder.innertube.utils.from

suspend fun Innertube.browse(body: BrowseBody) = runCatchingCancellable {
    val response = client.post(BROWSE) {
        setBody(body)
    }.body<BrowseResponse>()

    BrowseResult(
        title = response.header?.musicImmersiveHeaderRenderer?.title?.text ?: response.header
            ?.musicDetailHeaderRenderer?.title?.text,
        items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
                when {
                    content.gridRenderer != null -> BrowseResult.Item(
                        title = content.gridRenderer.header?.gridHeaderRenderer?.title?.runs
                            ?.firstOrNull()?.text ?: return@mapNotNull null,
                        items = content.gridRenderer.items?.mapNotNull { it.musicTwoRowItemRenderer?.toItem() }
                            .orEmpty()
                    )

                    content.musicCarouselShelfRenderer != null -> BrowseResult.Item(
                        title = content
                            .musicCarouselShelfRenderer
                            .header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return@mapNotNull null,
                        items = content
                            .musicCarouselShelfRenderer
                            .contents
                            ?.mapNotNull { it.musicTwoRowItemRenderer?.toItem() }
                            .orEmpty()
                    )

                    else -> null
                }
            }.orEmpty()
    )
}

data class BrowseResult(
    val title: String?,
    val items: List<Item>
) {
    data class Item(
        val title: String,
        val items: List<Innertube.Item>
    )
}

fun MusicTwoRowItemRenderer.toItem() = when {
    isAlbum -> Innertube.AlbumItem.from(this)
    isPlaylist -> Innertube.PlaylistItem.from(this)
    isArtist -> Innertube.ArtistItem.from(this)
    else -> null
}
