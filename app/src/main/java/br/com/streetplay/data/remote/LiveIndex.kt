package br.com.streetplay.data.remote

import br.com.streetplay.domain.LiveChannel
import kotlin.math.abs

data class RawStream(
    val channel: String, val url: String, val referrer: String?, val userAgent: String?,
    val quality: String?, val labels: List<String>,
)
data class RawChannel(
    val id: String, val name: String, val country: String, val categories: List<String>,
    val nsfw: Boolean, val closed: Boolean,
)
data class RawLogo(val channel: String, val url: String, val inUse: Boolean, val format: String?, val width: Int)

/** Junta streams + canais + logos da API do IPTV-org em uma lista única, já filtrada. */
object LiveIndexBuilder {
    private val LOGO_FORMATS = setOf("PNG", "JPEG", "WEBP", "GIF") // o Android não desenha SVG nem AVIF antigo

    fun build(streams: List<RawStream>, channels: List<RawChannel>, logos: List<RawLogo>, blocked: Set<String>): List<LiveChannel> {
        val bestStream = streams.groupBy { it.channel }.mapValues { (_, l) -> l.maxByOrNull { score(it) }!! }
        val bestLogo = logos.groupBy { it.channel }.mapValues { (_, l) -> pickLogo(l) }
        return channels.asSequence()
            .filter { !it.nsfw && !it.closed && it.id !in blocked && "xxx" !in it.categories }
            .mapNotNull { c ->
                val s = bestStream[c.id] ?: return@mapNotNull null
                LiveChannel(
                    id = c.id, name = c.name, logo = bestLogo[c.id], country = c.country,
                    categories = c.categories.ifEmpty { listOf("undefined") },
                    url = s.url, userAgent = s.userAgent, referrer = s.referrer, quality = s.quality, labels = s.labels,
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    /** Prefere stream sem aviso de bloqueio regional, em boa qualidade e com https. */
    fun score(s: RawStream): Long {
        var v = s.quality?.takeWhile { it.isDigit() }?.toLongOrNull() ?: 0L
        if ("Geo-blocked" in s.labels) v -= 5000
        if ("Not 24/7" in s.labels) v -= 2000
        if (s.url.startsWith("https")) v += 1
        return v
    }

    fun pickLogo(list: List<RawLogo>): String? =
        list.filter { (it.format ?: "PNG").uppercase() in LOGO_FORMATS }
            .minWithOrNull(compareBy({ !it.inUse }, { abs(it.width - 256) }))?.url
}

/** Formato compacto (uma linha por canal) para guardar a lista pronta no aparelho. */
object LiveIndexCodec {
    fun encode(list: List<LiveChannel>): String = list.joinToString("\n") { c ->
        listOf(
            c.id, c.name, c.country, c.categories.joinToString(","), c.logo.orEmpty(), c.url,
            c.userAgent.orEmpty(), c.referrer.orEmpty(), c.quality.orEmpty(), c.labels.joinToString(","),
        ).joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ') }
    }

    fun decode(text: String): List<LiveChannel> = text.lineSequence().mapNotNull { line ->
        val p = line.split('\t')
        if (p.size < 10) null else LiveChannel(
            id = p[0], name = p[1], logo = p[4].ifBlank { null }, country = p[2],
            categories = p[3].split(',').filter { it.isNotBlank() }, url = p[5],
            userAgent = p[6].ifBlank { null }, referrer = p[7].ifBlank { null }, quality = p[8].ifBlank { null },
            labels = p[9].split(',').filter { it.isNotBlank() },
        )
    }.toList()
}
