package br.com.streetplay.data.remote

import java.util.Locale

object LiveLabels {
    private val categories = mapOf(
        "general" to "Geral", "news" to "Notícias", "sports" to "Esportes", "movies" to "Filmes", "series" to "Séries",
        "entertainment" to "Entretenimento", "kids" to "Infantil", "music" to "Música", "documentary" to "Documentários",
        "education" to "Educação", "comedy" to "Comédia", "animation" to "Animação", "religious" to "Religioso",
        "lifestyle" to "Estilo de vida", "culture" to "Cultura", "family" to "Família", "business" to "Negócios",
        "auto" to "Automóveis", "outdoor" to "Natureza e aventura", "relax" to "Relax", "science" to "Ciência",
        "travel" to "Viagens", "weather" to "Clima", "legislative" to "Legislativo", "public" to "Público",
        "shop" to "Compras", "cooking" to "Culinária", "classic" to "Clássicos", "interactive" to "Interativo",
        "undefined" to "Outros",
    )

    fun category(id: String): String = categories[id] ?: id.replaceFirstChar { it.uppercase() }

    /** Nome do país em português a partir do código (a API usa "UK" para o Reino Unido). */
    fun country(code: String): String {
        val iso = if (code.equals("UK", true)) "GB" else code
        val name = Locale("", iso).getDisplayCountry(Locale("pt", "BR"))
        return if (name.isBlank() || name.equals(iso, true)) code else name.replaceFirstChar { it.uppercase() }
    }
}
