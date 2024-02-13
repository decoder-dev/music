package it.decoder.github.requests

import io.ktor.client.call.body
import io.ktor.client.request.get
import it.decoder.extensions.runCatchingCancellable
import it.decoder.github.GitHub
import it.decoder.github.models.Release

suspend fun GitHub.releases(
    owner: String,
    repo: String,
    page: Int = 1,
    pageSize: Int = 30
) = runCatchingCancellable {
    httpClient.get("repos/$owner/$repo/releases") {
        withPagination(page = page, size = pageSize)
    }.body<List<Release>>()
}
