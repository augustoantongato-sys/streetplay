package br.com.streetplay.domain

enum class AppCategory(val label: String) {
    VIDEO("Vídeo"), AUDIO("Música"), GAME("Jogos"), SOCIAL("Social"),
    NEWS("Notícias"), TOOLS("Utilitários"), OTHER("Outros")
}

data class InstalledApp(
    val packageName: String,
    val label: String,
    val category: AppCategory,
)

/** Atalho oficial (YouTube, Netflix...) com pacotes candidatos (versão TV e mobile). */
data class StreamingShortcut(
    val id: String,
    val name: String,
    val packages: List<String>,
    val brandColor: Long = 0xFFFF6A00,
)

data class ShortcutState(
    val shortcut: StreamingShortcut,
    val resolvedPackage: String?,
) {
    val installed: Boolean get() = resolvedPackage != null
}

/** Filme ou série em destaque (metadados e imagens do TMDB; nada é reproduzido por aqui). */
data class TrendingItem(
    val id: Int,
    val title: String,
    val type: String, // "movie" | "tv"
    val year: String,
    val rating: Double,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
) {
    val typeLabel: String get() = if (type == "tv") "Série" else "Filme"
}

data class LiveChannel(
    val id: String,
    val name: String,
    val logo: String?,
    val group: String,
    val url: String,
    val userAgent: String? = null,
    val referrer: String? = null,
)

/** Listas públicas oficiais do projeto IPTV-org. */
enum class LiveSource(val label: String, val url: String) {
    BRAZIL("Brasil", "https://iptv-org.github.io/iptv/countries/br.m3u"),
    MOVIES("Filmes", "https://iptv-org.github.io/iptv/categories/movies.m3u"),
    SERIES("Séries", "https://iptv-org.github.io/iptv/categories/series.m3u"),
    NEWS("Notícias", "https://iptv-org.github.io/iptv/categories/news.m3u"),
}
