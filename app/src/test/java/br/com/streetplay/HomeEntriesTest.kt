package br.com.streetplay

import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.AppCategory
import br.com.streetplay.domain.ShortcutState
import br.com.streetplay.data.StreamingCatalog
import br.com.streetplay.features.home.resolvePin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeEntriesTest {
    private val netflix = StreamingCatalog.all.first { it.id == "netflix" }
    private val shortcuts = listOf(
        ShortcutState(netflix, "com.netflix.ninja"),
        ShortcutState(StreamingCatalog.all.first { it.id == "disney" }, null),
    )
    private val apps = listOf(InstalledApp("com.exemplo.app", "Meu App", AppCategory.OTHER))

    @Test fun installedShortcutIsMarkedInstalledWithRealPackage() {
        val e = resolvePin("sc:netflix", apps, shortcuts)!!
        assertTrue(e.installed); assertEquals("com.netflix.ninja", e.packageName)
    }

    @Test fun notInstalledShortcutKeepsLogoSlotButIsNotInstalled() {
        val e = resolvePin("sc:disney", apps, shortcuts)!!
        assertFalse(e.installed); assertEquals("Disney+", e.label)
    }

    @Test fun androidAppPinResolvesAndMissingOnesAreDropped() {
        assertEquals("Meu App", resolvePin("pkg:com.exemplo.app", apps, shortcuts)!!.label)
        assertNull(resolvePin("pkg:com.removido", apps, shortcuts))
        assertNull(resolvePin("sc:inexistente", apps, shortcuts))
    }

    @Test fun defaultPinsAllExistInCatalog() {
        val ids = StreamingCatalog.all.map { it.id }.toSet()
        assertTrue(StreamingCatalog.DEFAULT_PINS.all { it.removePrefix("sc:") in ids })
    }
}
