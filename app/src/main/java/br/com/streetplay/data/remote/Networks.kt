package br.com.streetplay.data.remote

import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.NetworkGroup

/** Agrupa canais por emissora: "Globo" reúne TV Globo Minas, GloboNews, etc. */
object Networks {
    /** Emissoras brasileiras conhecidas, na ordem em que aparecem na lista. */
    private val BR = listOf(
        "Globo" to Regex("""\bglobo"""),
        "SBT" to Regex("""\bsbt\b"""),
        "Record" to Regex("""\brecord"""),
        "Band" to Regex("""(^|\s)band(news|sports)?(\s|$)|bandeirantes"""),
        "RedeTV!" to Regex("""\bredetv\b|\brede tv\b"""),
        "TV Brasil" to Regex("""\btv brasil\b"""),
        "TV Cultura" to Regex("""\bcultura\b"""),
        "TV Gazeta" to Regex("""\bgazeta\b"""),
        "CNN Brasil" to Regex("""\bcnn brasil\b"""),
        "Jovem Pan" to Regex("""\bjovem pan\b"""),
        "Canção Nova" to Regex("""\bcancao nova\b"""),
        "Rede Vida" to Regex("""\brede vida\b"""),
        "TV Aparecida" to Regex("""\baparecida\b"""),
        "Rede CNT" to Regex("""\bcnt\b"""),
        "Futura" to Regex("""\bfutura\b"""),
        "TV Câmara" to Regex("""\bcamara\b"""),
        "TV Senado" to Regex("""\bsenado\b"""),
        "TV Justiça" to Regex("""\bjustica\b"""),
    )
    private val STOP = setOf(
        "tv", "canal", "rede", "radio", "telecanal", "tele", "kanal", "the", "el", "la", "le", "il", "los", "las",
        "my", "new", "live", "hd", "sd", "big", "super", "mega", "channel",
    )
    private val BRACKETS = Regex("""[(\[].*?[)\]]""")
    private val SPLIT = Regex("""[\s\-_/|]+""")

    private fun norm(s: String) = LiveIndexBuilder.sortKey(s)

    /** (chave, nome exibido, posição fixa na lista ou null) */
    private data class Brand(val key: String, val name: String, val rank: Int?)

    private fun brandOf(c: LiveChannel): Brand {
        val n = norm(c.name)
        if (c.country == "BR") {
            BR.forEachIndexed { i, (name, rx) -> if (rx.containsMatchIn(n)) return Brand(norm(name), name, i) }
        }
        c.network?.trim()?.takeIf { it.isNotBlank() }?.let { return Brand(norm(it), it, null) }
        val toks = c.name.replace(BRACKETS, " ").split(SPLIT).filter { it.isNotBlank() }
        if (toks.isEmpty()) return Brand(n, c.name, null)
        val take = if (norm(toks[0]) in STOP && toks.size > 1) 2 else 1
        val display = toks.take(take).joinToString(" ")
        // palavras curtas ou só números ("24 Horas", "A&E") não servem de emissora
        if (take == 1 && (toks[0].length < 3 || toks[0].all { it.isDigit() })) return Brand(n, c.name, null)
        return Brand(norm(display), display, null)
    }

    /** Emissoras com 2 ou mais canais viram pastas; canais sozinhos aparecem soltos. */
    fun group(channels: List<LiveChannel>): List<NetworkGroup> {
        val map = LinkedHashMap<String, Pair<Brand, MutableList<LiveChannel>>>()
        for (c in channels) {
            val b = brandOf(c)
            map.getOrPut(b.key) { b to ArrayList() }.second += c
        }
        return map.values
            .map { (b, list) ->
                NetworkGroup(
                    key = b.key,
                    name = if (list.size == 1 && b.rank == null) list[0].name else b.name,
                    logo = list.firstNotNullOfOrNull { it.logo },
                    channels = list,
                    rank = b.rank,
                )
            }
            .sortedWith(compareBy({ it.rank ?: Int.MAX_VALUE }, { norm(it.name) }))
    }
}
