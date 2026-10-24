package br.com.streetplay.data

import br.com.streetplay.domain.StreamingShortcut

object StreamingCatalog {
    const val PLAY_STORE_PACKAGE = "com.android.vending"

    val all: List<StreamingShortcut> = listOf(
        StreamingShortcut("youtube", "YouTube", listOf("com.google.android.youtube.tv", "com.google.android.youtube")),
        StreamingShortcut("netflix", "Netflix", listOf("com.netflix.ninja", "com.netflix.mediaclient")),
        StreamingShortcut("prime", "Prime Video", listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient")),
        StreamingShortcut("disney", "Disney+", listOf("com.disney.disneyplus")),
        StreamingShortcut("spotify", "Spotify", listOf("com.spotify.tv.android", "com.spotify.music")),
        StreamingShortcut("playstore", "Google Play Store", listOf(PLAY_STORE_PACKAGE)),
    )

    /** Primeiro pacote candidato instalado, ou null. Nunca simula disponibilidade. */
    fun resolve(shortcut: StreamingShortcut, isInstalled: (String) -> Boolean): String? =
        shortcut.packages.firstOrNull(isInstalled)
}
