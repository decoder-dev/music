package it.decoder.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.decoder.extensions.runCatchingCancellable
import it.decoder.innertube.Innertube
import it.decoder.innertube.models.BrowseResponse
import it.decoder.innertube.models.ContinuationResponse
import it.decoder.innertube.models.GridRenderer
import it.decoder.innertube.models.MusicResponsiveListItemRenderer
import it.decoder.innertube.models.MusicShelfRenderer
import it.decoder.innertube.models.MusicTwoRowItemRenderer
import it.decoder.innertube.models.bodies.BrowseBody
import it.decoder.innertube.models.bodies.ContinuationBody

suspend fun <T : Innertube.Item> Innertube.itemsPage(
    body: BrowseBody,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null }
) = runCatchingCancellable {
    val response = client.post(BROWSE) {
        setBody(body)
    }.body<BrowseResponse>()

    val sectionListRendererContent = response
        .contents
        ?.singleColumnBrowseResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()

    itemsPageFromMusicShelRendererOrGridRenderer(
        musicShelfRenderer = sectionListRendererContent
            ?.musicShelfRenderer,
        gridRenderer = sectionListRendererContent
            ?.gridRenderer,
        fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
        fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer
    )
}

suspend fun <T : Innertube.Item> Innertube.itemsPage(
    body: ContinuationBody,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null }
) = runCatchingCancellable {
    val response = client.post(BROWSE) {
        setBody(body)
    }.body<ContinuationResponse>()

    itemsPageFromMusicShelRendererOrGridRenderer(
        musicShelfRenderer = response
            .continuationContents
            ?.musicShelfContinuation,
        gridRenderer = null,
        fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
        fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer
    )
}

private fun <T : Innertube.Item> itemsPageFromMusicShelRendererOrGridRenderer(
    musicShelfRenderer: MusicShelfRenderer?,
    gridRenderer: GridRenderer?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T?,
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T?
) = when {
    musicShelfRenderer != null -> Innertube.ItemsPage(
        continuation = musicShelfRenderer
            .continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation,
        items = musicShelfRenderer
            .contents
            ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(fromMusicResponsiveListItemRenderer)
    )

    gridRenderer != null -> Innertube.ItemsPage(
        continuation = null,
        items = gridRenderer
            .items
            ?.mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
            ?.mapNotNull(fromMusicTwoRowItemRenderer)
    )

    else -> null
}
