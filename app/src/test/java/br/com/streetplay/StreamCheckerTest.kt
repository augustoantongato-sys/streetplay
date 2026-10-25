package br.com.streetplay

import br.com.streetplay.data.remote.StreamChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamCheckerTest {
    @Test fun hlsPlaylistIsPlayable() = assertTrue(StreamChecker.looksPlayable("application/vnd.apple.mpegurl", "#EXTM3U\n#EXT-X-VERSION:3"))
    @Test fun binaryTransportStreamIsPlayable() = assertTrue(StreamChecker.looksPlayable("video/mp2t", "G\u0040\u0011\u0010"))
    @Test fun htmlErrorPageIsNot() = assertFalse(StreamChecker.looksPlayable("text/html", "<html><body>403</body></html>"))
    @Test fun htmlContentTypeIsNot() = assertFalse(StreamChecker.looksPlayable("text/html; charset=utf-8", "Acesso negado"))
    @Test fun emptyBodyIsNot() = assertFalse(StreamChecker.looksPlayable("video/mp4", ""))
}
