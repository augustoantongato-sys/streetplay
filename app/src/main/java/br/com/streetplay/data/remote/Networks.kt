package br.com.streetplay.data.remote

import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.NetworkGroup

/** Agrupa canais por emissora: "Globo" reúne TV Globo Minas, GloboNews, etc. */
object Networks {
    /** Emissoras brasileiras conhecidas, na ordem em que aparecem na lista. */
    private val BR = listOf(
        "Globo" to Regex(
            """\bglobo|\brbs tv|\beptv|\btv anhanguera|\btv integracao|\brpc tv|\btv bahia|\btv clube|\btv sul bahia|""" +
                """\btv sul de minas|\btv fronteira|\btv diario macapa|\btv verdes mares|\btv asa branca|\btv mirante|""" +
                """\btv liberal|\brede amazonica|\btv tem\b|\btv morena|\btv centro america|\btv sergipe|""" +
                """\btv gazeta (alagoas|acre|espirito santo)|\btv pajucara|\btv cabo branco|\btv subae|\btv santa cruz|""" +
                """\btv rio sul|\btv vanguarda|\btv sao francisco|\btv tapajos|\btv amapa|\btv acre|\btv roraima|\btv grande rio"""
        ),
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

    /** Estado (UF) das emissoras da família Globo, a partir do nome. */
    private val STATES = listOf(
        Regex("""eptv sul de minas|tv sul de minas|globo minas|tv integracao""") to "MG",
        Regex("""eptv|\btv tem\b|tv fronteira|tv vanguarda|globo sao paulo|globo sp""") to "SP",
        Regex("""globo rio|tv rio sul|tv grande rio""") to "RJ",
        Regex("""globo brasilia""") to "DF",
        Regex("""globo nordeste|tv asa branca""") to "PE",
        Regex("""tv bahia|tv sul bahia|tv subae|tv santa cruz|tv sao francisco""") to "BA",
        Regex("""tv clube""") to "PI",
        Regex("""tv diario macapa|tv amapa""") to "AP",
        Regex("""rbs tv (florianopolis|blumenau|joinville|chapeco|criciuma)""") to "SC",
        Regex("""rbs tv""") to "RS",
        Regex("""\brpc\b""") to "PR",
        Regex("""tv anhanguera""") to "GO",
        Regex("""tv morena""") to "MS",
        Regex("""tv centro america""") to "MT",
        Regex("""rede amazonica (ariquemes|ji parana|porto velho)""") to "RO",
        Regex("""rede amazonica""") to "AM",
        Regex("""tv liberal|tv tapajos""") to "PA",
        Regex("""tv mirante""") to "MA",
        Regex("""tv verdes mares""") to "CE",
        Regex("""tv sergipe""") to "SE",
        Regex("""tv gazeta alagoas|tv pajucara""") to "AL",
        Regex("""tv gazeta espirito santo""") to "ES",
        Regex("""tv gazeta acre|tv acre""") to "AC",
        Regex("""tv roraima""") to "RR",
        Regex("""tv cabo branco""") to "PB",
    )

    fun stateOf(c: LiveChannel): String? {
        val n = norm(c.name)
        return STATES.firstOrNull { it.first.containsMatchIn(n) }?.second
    }

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
                    states = if (b.key == "globo") list.mapNotNull { stateOf(it) }.distinct().sorted() else emptyList(),
                )
            }
            .sortedWith(compareBy({ it.rank ?: Int.MAX_VALUE }, { norm(it.name) }))
    }
}
