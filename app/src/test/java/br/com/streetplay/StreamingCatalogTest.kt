package br.com.streetplay

import br.com.streetplay.data.StreamingCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamingCatalogTest {
    private fun shortcut(id: String) = StreamingCatalog.all.first { it.id == id }

    @Test fun hasNineOfficialShortcuts() = assertEquals(9, StreamingCatalog.all.size)

    @Test fun prefersTvPackageWhenBothInstalled() =
        assertEquals("com.netflix.ninja", StreamingCatalog.resolve(shortcut("netflix")) { true })

    @Test fun fallsBackToMobilePackage() =
        assertEquals("com.netflix.mediaclient", StreamingCatalog.resolve(shortcut("netflix")) { it == "com.netflix.mediaclient" })

    @Test fun neverSimulatesAvailability() = assertNull(StreamingCatalog.resolve(shortcut("disney")) { false })
}
