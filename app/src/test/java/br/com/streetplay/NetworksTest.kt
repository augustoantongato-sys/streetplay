package br.com.streetplay

import br.com.streetplay.data.remote.Networks
import br.com.streetplay.domain.LiveChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworksTest {
    private fun ch(name: String, country: String = "BR", network: String? = null) =
        LiveChannel(name + country, name, null, country, emptyList(), "https://x/$name", network = network)

    private val br = listOf(
        "Band Bahia", "BandNews", "Band", "TV Globo Minas", "GloboNews", "TV Globo Sao Paulo", "Rede Globo",
        "SBT Rio", "SBT Interior", "+SBT Novelas", "RecordTV Belem", "Record News", "NDTV Record TV Criciuma",
        "TV Cultura", "Cultura Para", "Canal Aleatorio",
    ).map { ch(it) }
    private val groups = Networks.group(br)
    private fun members(name: String) = groups.first { it.name == name }.channels.map { it.name }.sorted()

    @Test fun globoGathersAllVariations() =
        assertEquals(listOf("GloboNews", "Rede Globo", "TV Globo Minas", "TV Globo Sao Paulo"), members("Globo"))

    @Test fun sbtRecordAndBandGatherTheirs() {
        assertEquals(listOf("+SBT Novelas", "SBT Interior", "SBT Rio"), members("SBT"))
        assertEquals(3, members("Record").size)
        assertEquals(listOf("Band", "Band Bahia", "BandNews"), members("Band"))
    }

    @Test fun bigBrazilianNetworksComeFirstInFixedOrder() =
        assertEquals(listOf("Globo", "SBT", "Record", "Band"), groups.take(4).map { it.name })

    @Test fun cultureAndSingletons() {
        assertEquals(listOf("Cultura Para", "TV Cultura"), members("TV Cultura"))
        val alone = groups.first { it.channels.size == 1 }
        assertEquals("Canal Aleatorio", alone.name)
    }

    @Test fun otherCountriesGroupByFirstWordOrApiNetwork() {
        val world = Networks.group(
            listOf(ch("Sky News", "UK"), ch("Sky Sports", "UK"), ch("BBC One", "UK", "BBC"), ch("BBC Two", "UK", "BBC"), ch("Arte", "FR")),
        )
        assertEquals(listOf("Sky News", "Sky Sports"), world.first { it.name == "Sky" }.channels.map { it.name })
        assertEquals(2, world.first { it.name == "BBC" }.channels.size)
        assertTrue(world.any { it.name == "Arte" && it.channels.size == 1 })
    }

    @Test fun globoFamilyIncludesRegionalAffiliatesWithTheirStates() {
        val list = listOf(
            "TV Globo Sao Paulo", "TV Globo Minas", "TV Bahia", "TV Clube", "TV Diario Macapa", "TV Fronteira",
            "TV Sul de Minas", "RBS TV Porto Alegre", "EPTV Central", "RPC TV Curitiba", "TV Gazeta", "TV Gazeta Alagoas", "Record Rio",
        ).map { ch(it) }
        val g = Networks.group(list)
        val globo = g.first { it.name == "Globo" }
        assertEquals(11, globo.channels.size)
        assertTrue("TV Gazeta" !in globo.channels.map { it.name })          // a Gazeta de São Paulo é outra emissora
        assertEquals(listOf("AL", "AP", "BA", "MG", "PI", "PR", "RS", "SP"), globo.states)
    }

    @Test fun stateOfRecognizesPraças() {
        assertEquals("RS", Networks.stateOf(ch("RBS TV Porto Alegre")))
        assertEquals("SC", Networks.stateOf(ch("RBS TV Florianopolis")))
        assertEquals("MG", Networks.stateOf(ch("EPTV Sul de Minas")))
        assertEquals(null, Networks.stateOf(ch("SBT Rio")))
    }
}
