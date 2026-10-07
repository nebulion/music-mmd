package com.calmapps.calmmusic

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class PlaybackErrorsTest {

    private fun error(code: Int, cause: Throwable? = null) =
        PlaybackException("test", cause, code)

    @Test
    fun `expired url is retried, then pauses instead of skipping`() {
        val e = error(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        assertEquals(ErrorAction.RETRY, errorAction(e, 0))
        assertEquals(ErrorAction.RETRY, errorAction(e, 2))
        assertEquals(ErrorAction.PAUSE, errorAction(e, 3))
    }

    @Test
    fun `offline does not skip through the queue`() {
        val e = error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        assertEquals(ErrorAction.PAUSE, errorAction(e, 3))
    }

    @Test
    fun `unplayable video is skipped at once`() {
        val cause = IOException("wrapped", RuntimeException("ContentNotAvailable: removed"))
        val e = error(PlaybackException.ERROR_CODE_IO_UNSPECIFIED, cause)
        assertEquals(ErrorAction.SKIP, errorAction(e, 0))
    }

    @Test
    fun `broken file is skipped`() {
        val e = error(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)
        assertEquals(ErrorAction.SKIP, errorAction(e, 0))
    }
}
