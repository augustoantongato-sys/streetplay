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

/** Uma fonte de vídeo de um canal (os canais costumam ter várias). */
data class StreamOption(val url: String, val userAgent: String? = null, val referrer: String? = null)

data class LiveChannel(
    val id: String,
    val name: String,
    val logo: String?,
    val country: String,
    val categories: List<String>,
    val url: String,
    val userAgent: String? = null,
    val referrer: String? = null,
    val quality: String? = null,
    val labels: List<String> = emptyList(),
    val alternates: List<StreamOption> = emptyList(),
    val network: String? = null,
) {
    /** Canais que só existem no YouTube ou na Twitch: abrem no app deles, não no player do Street Play. */
    val external: Boolean get() = isExternalUrl(url)

    /** Todas as fontes, da melhor para a pior. O player tenta uma por vez. */
    val options: List<StreamOption> get() = listOf(StreamOption(url, userAgent, referrer)) + alternates
}

fun isExternalUrl(url: String): Boolean =
    url.contains("youtube.com", true) || url.contains("youtu.be", true) || url.contains("twitch.tv", true)

/** Uma emissora e todas as variações dos seus canais (praças, notícias, esportes...). */
data class NetworkGroup(val key: String, val name: String, val logo: String?, val channels: List<LiveChannel>, val rank: Int? = null, val states: List<String> = emptyList())

enum class GroupKind { ALL, FAVORITES, COUNTRY, CATEGORY, BRAZIL }

/** Grupo da lista de canais (como as pastas de um app de IPTV). */
data class LiveGroup(val id: String, val label: String, val count: Int, val kind: GroupKind)

/** Os dois modos do app. O usuário não escolhe cores nem fundo: só o modo. */
enum class AppMode(val label: String) { STREET("Street Play"), LITE("Lite") }

/** Cores principais do tema (o fundo continua escuro). Laranja é a cor da marca e o padrão. */
enum class AccentTheme(val label: String, val argb: Long) {
    ORANGE("Laranja", 0xFFFF6A00),
    BLUE("Azul", 0xFF2F80FF),
    PINK("Rosa", 0xFFFF4D9D),
    YELLOW("Amarelo", 0xFFFFC107),
    GREEN("Verde", 0xFF2ECC71),
    PURPLE("Roxo", 0xFF9B5CFF),
}

/** Um item da fileira "Aplicativos" da Home. id = "sc:<atalho>" ou "pkg:<pacote>". */
data class HomeEntry(
    val id: String,
    val label: String,
    val packageName: String?,
    val installed: Boolean,
    val brandColor: Long,
    val storePackage: String?,
)
