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
