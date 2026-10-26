package br.com.streetplay.data

import br.com.streetplay.domain.StreamingShortcut

object StreamingCatalog {
    const val PLAY_STORE_PACKAGE = "com.android.vending"

    val all: List<StreamingShortcut> = listOf(
        StreamingShortcut("youtube", "YouTube", listOf("com.google.android.youtube.tv", "com.google.android.youtube"), 0xFFFF0000),
        StreamingShortcut("netflix", "Netflix", listOf("com.netflix.ninja", "com.netflix.mediaclient"), 0xFFE50914),
        StreamingShortcut("prime", "Prime Video", listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"), 0xFF00A8E1),
        StreamingShortcut("disney", "Disney+", listOf("com.disney.disneyplus"), 0xFF1D4FD8),
        StreamingShortcut("globoplay", "Globoplay", listOf("com.globo.globotv"), 0xFFFF4E00),
        StreamingShortcut("pluto", "Pluto TV", listOf("tv.pluto.android"), 0xFFFFD400),
        StreamingShortcut("hbomax", "HBO Max", listOf("com.wbd.stream", "com.hbo.hbonow"), 0xFF7B2FF7),
        StreamingShortcut("spotify", "Spotify", listOf("com.spotify.tv.android", "com.spotify.music"), 0xFF1DB954),
        StreamingShortcut("playstore", "Google Play", listOf(PLAY_STORE_PACKAGE), 0xFF34A853),
    )

    /** Primeiro pacote candidato instalado, ou null. Nunca simula disponibilidade. */
    fun resolve(shortcut: StreamingShortcut, isInstalled: (String) -> Boolean): String? =
        shortcut.packages.firstOrNull(isInstalled)
}
