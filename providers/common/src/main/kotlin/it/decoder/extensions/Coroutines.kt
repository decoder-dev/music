package it.decoder.extensions

import kotlinx.coroutines.CancellationException

inline fun <T> runCatchingCancellable(block: () -> T) =
    runCatching(block).takeIf { it.exceptionOrNull() !is CancellationException }
