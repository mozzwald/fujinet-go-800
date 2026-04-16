package com.mantismoonlabs.fujinetgo800.storage

import com.mantismoonlabs.fujinetgo800.settings.SystemRomKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SystemRomDocumentStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun savedSelectionsRoundTrip() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val store = SystemRomDocumentStore(runtimePaths)
        val selection = SystemRomSelection(
            uriString = "content://provider/roms/xlxe.rom",
            importedPath = "/tmp/imports/roms/xlxe.rom",
            displayName = "My XLXE ROM.rom",
            lastUpdatedEpochMillis = 100L,
        )

        store.saveSelection(SystemRomKind.XL_XE, selection)

        val reloadedStore = SystemRomDocumentStore(runtimePaths)
        assertEquals(selection, reloadedStore.loadSelection(SystemRomKind.XL_XE))
    }

    @Test
    fun clearingSelectionRemovesIt() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val store = SystemRomDocumentStore(runtimePaths)
        val selection = SystemRomSelection(
            uriString = "content://provider/roms/basic.rom",
            importedPath = "/tmp/imports/roms/basic.rom",
            displayName = "basic.rom",
            lastUpdatedEpochMillis = 100L,
        )

        store.saveSelection(SystemRomKind.BASIC, selection)
        assertEquals(selection, store.loadSelection(SystemRomKind.BASIC))

        store.clearSelection(SystemRomKind.BASIC)

        assertEquals(null, store.loadSelection(SystemRomKind.BASIC))
        assertFalse(store.loadAllSelections().containsKey(SystemRomKind.BASIC))
    }
}
