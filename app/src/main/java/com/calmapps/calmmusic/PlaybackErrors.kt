package com.calmapps.calmmusic

import androidx.media3.common.PlaybackException

/** The innermost cause of [error]. */
fun rootCause(error: Throwable): Throwable {
    var cause: Throwable = error
    while (true) {
        val next = cause.cause ?: return cause
        if (next === cause) return cause
        cause = next
    }
}

/** YouTube says this video can't be played (removed, region-locked, age-gated). Retrying won't help. */
fun isContentUnavailable(error: Throwable): Boolean {
    var cause: Throwable? = error
    while (cause != null) {
        val message = cause.message.orEmpty()
        if (cause.javaClass.name.startsWith("org.schabi.newpipe.extractor.exceptions.ContentNotAvailable") ||
            cause.javaClass.name.endsWith("AgeRestrictedContentException") ||
            message.contains("ContentNotAvailable", ignoreCase = true)
        ) return true
        if (cause.cause === cause) break
        cause = cause.cause
    }
    return false
}

/** What the playback service does after a player error. */
enum class ErrorAction { RETRY, SKIP, PAUSE }

/** Wait before retry number [retriesSoFar] + 1. */
fun retryDelayMs(retriesSoFar: Int): Long = when (retriesSoFar) {
    0 -> 1_000L
    1 -> 5_000L
    else -> 20_000L
}

/**
 * - Network trouble and expired stream URLs (HTTP 403/410): retry with a freshly
 *   resolved URL, up to [maxRetries] times; then pause on the song (offline: don't
 *   skip through the whole queue).
 * - Content YouTube refuses to play, or a broken file: skip to the next song, so
 *   the queue keeps going with the screen off.
 */
fun errorAction(error: PlaybackException, retriesSoFar: Int, maxRetries: Int = 3): ErrorAction {
    if (isContentUnavailable(error)) return ErrorAction.SKIP
    val network = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_TIMEOUT -> true
        else -> false
    }
    return when {
        !network -> ErrorAction.SKIP
        retriesSoFar < maxRetries -> ErrorAction.RETRY
        else -> ErrorAction.PAUSE
    }
}
