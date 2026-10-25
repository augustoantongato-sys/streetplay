package br.com.streetplay.data.remote

/** Lê a playlist pública da Free-TV/IPTV (https://github.com/Free-TV/IPTV): canais abertos de emissoras. */
object FreeTvParser {
    private val ATTR = Regex("""([\w-]+)="([^"]*)"""")
    private val MARKS = Regex("[ⒼⓈⓉⓎ]") // avisos no nome: região, YouTube, Twitch

    fun parse(text: String): List<FreeTvEntry> {
        val out = ArrayList<FreeTvEntry>()
        var attrs: Map<String, String> = emptyMap()
        var name: String? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    attrs = ATTR.findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
                    name = line.substringAfterLast(",").replace(MARKS, "").trim()
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    val n = name; name = null
                    val group = attrs["group-title"].orEmpty()
                    // "VOD ..." são filmes sob demanda, não canais ao vivo: ficam de fora
                    if (n != null && n.isNotBlank() && !group.startsWith("VOD", true) &&
                        (line.startsWith("http://") || line.startsWith("https://"))
                    ) {
                        val id = attrs["tvg-id"].orEmpty()
                        out += FreeTvEntry(id, n, attrs["tvg-logo"]?.ifBlank { null }, countryOf(attrs, id), group, line)
                    }
                }
            }
        }
        return out
    }

    /** A Free-TV usa "GB"; a API do IPTV-org usa "UK". Padroniza para casar os grupos. */
    fun countryOf(attrs: Map<String, String>, id: String): String {
        val fromAttr = attrs["tvg-country"].orEmpty().split(';', ',').first().trim().uppercase()
        val code = fromAttr.ifBlank { id.substringAfterLast('.', "").uppercase().takeIf { it.length == 2 } ?: "" }
        return if (code == "GB") "UK" else code
    }
}
