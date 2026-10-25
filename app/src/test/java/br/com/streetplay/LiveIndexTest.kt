package br.com.streetplay

import br.com.streetplay.data.remote.LiveIndexBuilder
import br.com.streetplay.data.remote.LiveIndexCodec
import br.com.streetplay.data.remote.RawChannel
import br.com.streetplay.data.remote.RawLogo
import br.com.streetplay.data.remote.RawStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveIndexTest {
    private fun stream(ch: String, url: String, q: String? = null, labels: List<String> = emptyList()) =
        RawStream(ch, url, null, null, q, labels)

    private val channels = listOf(
        RawChannel("A.br", "Canal A", "BR", listOf("news"), nsfw = false, closed = false),
        RawChannel("B.br", "Canal B", "BR", emptyList(), nsfw = false, closed = false),
        RawChannel("C.br", "Adulto", "BR", listOf("xxx"), nsfw = true, closed = false),
        RawChannel("D.br", "Fechado", "BR", listOf("general"), nsfw = false, closed = true),
        RawChannel("E.br", "Bloqueado", "BR", listOf("general"), nsfw = false, closed = false),
        RawChannel("F.br", "Sem stream", "BR", listOf("general"), nsfw = false, closed = false),
    )
    private val streams = listOf(
        stream("A.br", "http://a/low.m3u8", "480p"),
        stream("A.br", "https://a/hd.m3u8", "1080p"),
        stream("A.br", "https://a/geo.m3u8", "2160p", listOf("Geo-blocked")),
        stream("B.br", "https://b/x.m3u8"), stream("C.br", "https://c/x.m3u8"),
        stream("D.br", "https://d/x.m3u8"), stream("E.br", "https://e/x.m3u8"),
    )
    private val logos = listOf(
        RawLogo("A.br", "https://l/a.svg", true, "SVG", 256),
        RawLogo("A.br", "https://l/a-big.png", true, "PNG", 1000),
        RawLogo("A.br", "https://l/a-ok.png", true, "PNG", 250),
        RawLogo("A.br", "https://l/a-old.png", false, "PNG", 256),
    )
    private val result = LiveIndexBuilder.build(streams, channels, logos, blocked = setOf("E.br"))

    @Test fun removesAdultClosedBlockedAndStreamless() = assertEquals(listOf("A.br", "B.br"), result.map { it.id })

    @Test fun picksBestStreamAvoidingGeoBlocked() = assertEquals("https://a/hd.m3u8", result.first().url)

    @Test fun keepsOtherSourcesAsFallbackInOrder() {
        val a = result.first()
        assertEquals(listOf("https://a/hd.m3u8", "http://a/low.m3u8", "https://a/geo.m3u8").sorted(), a.options.map { it.url }.sorted())
        assertEquals("https://a/hd.m3u8", a.options.first().url)
        assertEquals(3, a.options.size)
    }

    @Test fun sortsWithoutAccents() {
        assertTrue(LiveIndexBuilder.sortKey("Éxito") < LiveIndexBuilder.sortKey("Fox"))
    }

    @Test fun picksUsableLogoNotSvg() = assertEquals("https://l/a-ok.png", result.first().logo)

    @Test fun missingCategoryBecomesUndefinedAndNoLogoIsNull() {
        val b = result.first { it.id == "B.br" }
        assertEquals(listOf("undefined"), b.categories)
        assertNull(b.logo)
    }

    @Test fun codecRoundTrips() {
        val back = LiveIndexCodec.decode(LiveIndexCodec.encode(result))
        assertEquals(result, back)
    }

    @Test fun codecCleansTabsAndNewlinesInNames() {
        val odd = result.first().copy(name = "Canal\tcom\ntab")
        val back = LiveIndexCodec.decode(LiveIndexCodec.encode(listOf(odd))).single()
        assertTrue('\t' !in back.name && '\n' !in back.name)
    }
}
