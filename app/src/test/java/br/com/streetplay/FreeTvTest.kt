package br.com.streetplay

import br.com.streetplay.data.remote.FreeTvParser
import br.com.streetplay.data.remote.LiveIndexBuilder
import br.com.streetplay.domain.LiveChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeTvTest {
    private val sample = """
        #EXTM3U x-tvg-url="https://epg.example/x.xml.gz"
        #EXTINF:-1 tvg-name="COM Brasil" tvg-logo="https://l/c.png" tvg-id="COMBrasil.br" tvg-country="BR" group-title="Brazil",COM Brasil
        https://br.example/abc/playlist.m3u8
        #EXTINF:-1 tvg-name="SBT" tvg-id="SBTNacional.br" tvg-country="BR" group-title="Brazil",SBT Ⓨ
        https://www.youtube.com/watch?v=ABVQXgr2LW4
        #EXTINF:-1 tvg-name="BBC One" tvg-id="BBCOne.uk" tvg-country="GB" group-title="UK",BBC One Ⓖ
        https://uk.example/bbc.m3u8
        #EXTINF:-1 tvg-name="Filme X" tvg-id="" group-title="VOD Movies (EN)",Filme X
        https://vod.example/x.mp4
        #EXTINF:-1 tvg-name="Euronews" tvg-id="EuronewsEnglish.fr" group-title="News",Euronews
        https://news.example/euro.m3u8
    """.trimIndent()

    private val entries = FreeTvParser.parse(sample)

    @Test fun skipsVodAndStripsMarkers() {
        assertEquals(listOf("COM Brasil", "SBT", "BBC One", "Euronews"), entries.map { it.name })
    }

    @Test fun mapsGbToUkAndDerivesCountryFromId() {
        assertEquals("UK", entries.first { it.name == "BBC One" }.country)
        assertEquals("FR", entries.first { it.name == "Euronews" }.country)
    }

    private val base = listOf(LiveChannel("COMBrasil.br", "COM Brasil", null, "BR", listOf("general"), "https://iptv.example/com.m3u8"))
    private val merged = LiveIndexBuilder.merge(base, entries, blocked = emptySet())

    @Test fun addsSourceToExistingChannelAndNewChannelsForTheRest() {
        assertEquals(listOf("BBC One", "COM Brasil", "Euronews", "SBT"), merged.map { it.name })
        val com = merged.first { it.id == "COMBrasil.br" }
        assertEquals(listOf("https://iptv.example/com.m3u8", "https://br.example/abc/playlist.m3u8"), com.options.map { it.url })
    }

    @Test fun youtubeOnlyChannelsAreExternal() {
        assertTrue(merged.first { it.name == "SBT" }.external)
        assertFalse(merged.first { it.name == "BBC One" }.external)
    }

    @Test fun blockedIdsAreSkipped() {
        val r = LiveIndexBuilder.merge(emptyList(), entries, blocked = setOf("BBCOne.uk"))
        assertFalse(r.any { it.id == "BBCOne.uk" })
    }
}
