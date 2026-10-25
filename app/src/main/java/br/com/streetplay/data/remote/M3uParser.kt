package br.com.streetplay.data.remote

import br.com.streetplay.domain.LiveChannel

object M3uParser {
    private val ATTR = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String): List<LiveChannel> {
        val out = ArrayList<LiveChannel>()
        val seen = HashSet<String>()
        var name: String? = null
        var attrs: Map<String, String> = emptyMap()
        var ua: String? = null
        var ref: String? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    attrs = ATTR.findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
                    name = line.substringAfterLast(",").trim()
                    ua = null; ref = null
                }
                line.startsWith("#EXTVLCOPT:http-user-agent=") -> ua = line.substringAfter("=").trim().ifBlank { null }
                line.startsWith("#EXTVLCOPT:http-referrer=") -> ref = line.substringAfter("=").trim().ifBlank { null }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    val n = name
                    val group = attrs["group-title"].orEmpty()
                    val adult = group.contains("xxx", true) || attrs["tvg-id"].orEmpty().contains("xxx", true)
                    if (n != null && n.isNotBlank() && !adult && (line.startsWith("http://") || line.startsWith("https://"))) {
                        val id = attrs["tvg-id"].orEmpty().ifBlank { n }
                        if (seen.add(id)) {
                            out += LiveChannel(id, n, attrs["tvg-logo"]?.ifBlank { null }, group, line, ua, ref)
                        }
                    }
                    name = null
                }
            }
        }
        return out
    }
}
