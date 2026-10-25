package br.com.streetplay

import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.features.live.buildLetterIndex
import br.com.streetplay.features.live.letterOf
import org.junit.Assert.assertEquals
import org.junit.Test

class LetterIndexTest {
    private fun ch(name: String) = LiveChannel(name, name, null, "BR", emptyList(), "https://x")

    @Test fun accentedAndDigitsMapToLetters() {
        assertEquals('E', letterOf("Éxito TV"))
        assertEquals('#', letterOf("24 Horas"))
    }

    @Test fun indexPointsToFirstChannelOfEachLetter() {
        val list = listOf("24h", "Alfa", "Amigo", "Bravo", "Canal").map(::ch)
        assertEquals(listOf('#' to 0, 'A' to 1, 'B' to 3, 'C' to 4), buildLetterIndex(list))
    }
}
