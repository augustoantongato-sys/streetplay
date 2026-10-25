package br.com.streetplay

import br.com.streetplay.data.remote.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {
    private val sample = """
        #EXTM3U
        #EXTINF:-1 tvg-id="TVBrasil.br" tvg-logo="https://x/logo.png" group-title="General",TV Brasil (720p)
        https://stream.example/tvbrasil.m3u8
        #EXTINF:-1 tvg-id="Canal2.br" group-title="Movies",Canal Filmes
        #EXTVLCOPT:http-referrer=https://site.example/
        #EXTVLCOPT:http-user-agent=Agente/1.0
        http://stream.example/filmes.m3u8
        #EXTINF:-1 tvg-id="Adulto.xx" group-title="XXX",Canal Adulto
        https://stream.example/adulto.m3u8
        #EXTINF:-1 tvg-id="TVBrasil.br" group-title="General",TV Brasil duplicado
        https://stream.example/outro.m3u8
        #EXTINF:-1 tvg-id="Invalido.br",Sem http
        rtmp://stream.example/x
    """.trimIndent()

    private val result = M3uParser.parse(sample)

    @Test fun parsesNameLogoAndUrl() {
        val c = result.first()
        assertEquals("TV Brasil (720p)", c.name)
        assertEquals("https://x/logo.png", c.logo)
        assertEquals("https://stream.example/tvbrasil.m3u8", c.url)
    }

    @Test fun readsReferrerAndUserAgent() {
        val c = result.first { it.id == "Canal2.br" }
        assertEquals("https://site.example/", c.referrer)
        assertEquals("Agente/1.0", c.userAgent)
    }

    @Test fun dropsAdultDuplicatesAndNonHttp() {
        assertEquals(listOf("TVBrasil.br", "Canal2.br"), result.map { it.id })
        assertNull(result.firstOrNull { it.group == "XXX" })
    }
}
