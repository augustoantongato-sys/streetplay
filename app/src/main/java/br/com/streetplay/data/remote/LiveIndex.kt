package br.com.streetplay.data.remote

import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.StreamOption
import br.com.streetplay.domain.isExternalUrl
import java.text.Normalizer
import kotlin.math.abs

data class RawStream(
    val channel: String, val url: String, val referrer: String?, val userAgent: String?,
    val quality: String?, val labels: List<String>,
)
data class RawChannel(
    val id: String, val name: String, val country: String, val categories: List<String>,
    val nsfw: Boolean, val closed: Boolean, val network: String? = null,
)
/** Entrada da lista curada Free-TV/IPTV. */
data class FreeTvEntry(val id: String, val name: String, val logo: String?, val country: String, val group: String, val url: String)
data class RawLogo(val channel: String, val url: String, val inUse: Boolean, val format: String?, val width: Int)

/** Junta streams + canais + logos da API do IPTV-org em uma lista única, já filtrada. */
object LiveIndexBuilder {
    private const val MAX_SOURCES = 4
    private val LOGO_FORMATS = setOf("PNG", "JPEG", "WEBP", "GIF") // o Android não desenha SVG nem AVIF antigo

    fun build(streams: List<RawStream>, channels: List<RawChannel>, logos: List<RawLogo>, blocked: Set<String>): List<LiveChannel> {
        val ranked = streams.groupBy { it.channel }.mapValues { (_, l) -> l.sortedByDescending { score(it) }.distinctBy { it.url }.take(MAX_SOURCES) }
        val bestLogo = logos.groupBy { it.channel }.mapValues { (_, l) -> pickLogo(l) }
        return channels.asSequence()
            .filter { !it.nsfw && !it.closed && it.id !in blocked && "xxx" !in it.categories }
            .mapNotNull { c ->
                val list = ranked[c.id] ?: return@mapNotNull null
                val s = list.first()
                LiveChannel(
                    id = c.id, name = c.name, logo = bestLogo[c.id], country = c.country,
                    categories = c.categories.ifEmpty { listOf("undefined") },
                    url = s.url, userAgent = s.userAgent, referrer = s.referrer, quality = s.quality, labels = s.labels,
                    alternates = list.drop(1).map { StreamOption(it.url, it.userAgent, it.referrer) },
                    network = c.network?.ifBlank { null },
                )
            }
            .sortedBy { sortKey(it.name) }
            .toList()
    }

    /** Junta a lista curada da Free-TV: soma fontes a canais que já existem e acrescenta os que faltam. */
    fun merge(base: List<LiveChannel>, extra: List<FreeTvEntry>, blocked: Set<String>): List<LiveChannel> {
        val updated = LinkedHashMap<String, LiveChannel>()
        base.forEach { updated[it.id] = it }
        val keyToId = HashMap<String, String>()
        base.forEach { keyToId.putIfAbsent(key(it.name, it.country), it.id) }
        for (e in extra) {
            if (e.id in blocked) continue
            val external = isExternalUrl(e.url)
            val targetId = if (e.id.isNotBlank() && updated.containsKey(e.id)) e.id else keyToId[key(e.name, e.country)]
            if (targetId != null) {
                val t = updated[targetId]!!
                when {
                    t.external && !external -> updated[targetId] = t.copy(url = e.url, userAgent = null, referrer = null, alternates = emptyList())
                    !external && t.options.none { it.url == e.url } && t.options.size <= MAX_SOURCES ->
                        updated[targetId] = t.copy(alternates = t.alternates + StreamOption(e.url))
                }
            } else {
                val id = e.id.ifBlank { "ftv:" + key(e.name, e.country) }
                if (updated.containsKey(id)) continue
                updated[id] = LiveChannel(
                    id = id, name = e.name, logo = e.logo, country = e.country,
                    categories = if (e.group.equals("News", true)) listOf("news") else listOf("undefined"), url = e.url,
                )
                keyToId.putIfAbsent(key(e.name, e.country), id)
            }
        }
        return updated.values.sortedBy { sortKey(it.name) }
    }

    private fun key(name: String, country: String) = sortKey(name) + "|" + country

    /** Ordem alfabética sem acentos ("Éxito" fica junto do E). */
    fun sortKey(name: String): String =
        Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD).filter { it.isLetterOrDigit() || it == ' ' }

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
    private const val FIELD = '\u001f'
    private const val ENTRY = '\u001e'

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').replace(FIELD, ' ').replace(ENTRY, ' ')

    fun encode(list: List<LiveChannel>): String = list.joinToString("\n") { c ->
        val alts = c.alternates.joinToString(ENTRY.toString()) { listOf(it.url, it.userAgent.orEmpty(), it.referrer.orEmpty()).joinToString(FIELD.toString()) { f -> clean(f) } }
        listOf(
            c.id, c.name, c.country, c.categories.joinToString(","), c.logo.orEmpty(), c.url,
            c.userAgent.orEmpty(), c.referrer.orEmpty(), c.quality.orEmpty(), c.labels.joinToString(","),
        ).joinToString("\t") { clean(it) } + "\t" + alts + "\t" + clean(c.network.orEmpty())
    }

    fun decode(text: String): List<LiveChannel> = text.lineSequence().mapNotNull { line ->
        val p = line.split('\t')
        if (p.size < 10) null else LiveChannel(
            id = p[0], name = p[1], logo = p[4].ifBlank { null }, country = p[2],
            categories = p[3].split(',').filter { it.isNotBlank() }, url = p[5],
            userAgent = p[6].ifBlank { null }, referrer = p[7].ifBlank { null }, quality = p[8].ifBlank { null },
            labels = p[9].split(',').filter { it.isNotBlank() },
            alternates = p.getOrNull(10).orEmpty().split(ENTRY).filter { it.isNotBlank() }.map { e ->
                val f = e.split(FIELD)
                StreamOption(f[0], f.getOrNull(1)?.ifBlank { null }, f.getOrNull(2)?.ifBlank { null })
            },
            network = p.getOrNull(11)?.ifBlank { null },
        )
    }.toList()
}
