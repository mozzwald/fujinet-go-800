package com.mantismoonlabs.fujinetgo800.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MediaDocumentStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun savedSelectionsRoundTrip() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val store = MediaDocumentStore(runtimePaths)

        val diskSelection = MediaSelection(
            mediaKind = MediaKind.DISK,
            uriString = "content://media/disk",
            importedPath = "/tmp/imports/disks/demo.atr",
            displayName = "demo.atr",
            lastUpdatedEpochMillis = 100L,
        )
        val cartSelection = MediaSelection(
            mediaKind = MediaKind.CARTRIDGE,
            uriString = "content://media/cart",
            importedPath = "/tmp/imports/carts/demo.car",
            displayName = "demo.car",
            lastUpdatedEpochMillis = 200L,
        )
        val executableSelection = MediaSelection(
            mediaKind = MediaKind.EXECUTABLE,
            uriString = "content://media/xex",
            importedPath = "/tmp/imports/executables/demo.xex",
            displayName = "demo.xex",
            lastUpdatedEpochMillis = 300L,
        )
        val romSelection = MediaSelection(
            mediaKind = MediaKind.ROM,
            uriString = "content://media/rom",
            importedPath = "/tmp/imports/roms/demo.rom",
            displayName = "demo.rom",
            lastUpdatedEpochMillis = 400L,
        )

        store.saveSelection(MediaRole.DISK, diskSelection)
        store.saveSelection(MediaRole.CARTRIDGE, cartSelection)
        store.saveSelection(MediaRole.EXECUTABLE, executableSelection)
        store.saveSelection(MediaRole.ROM, romSelection)

        val reloadedStore = MediaDocumentStore(runtimePaths)

        assertEquals(diskSelection, reloadedStore.loadSelection(MediaRole.DISK))
        assertEquals(cartSelection, reloadedStore.loadSelection(MediaRole.CARTRIDGE))
        assertEquals(executableSelection, reloadedStore.loadSelection(MediaRole.EXECUTABLE))
        assertEquals(romSelection, reloadedStore.loadSelection(MediaRole.ROM))
    }

    @Test
    fun clearingSelectionRemovesIt() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val store = MediaDocumentStore(runtimePaths)
        val diskSelection = MediaSelection(
            mediaKind = MediaKind.DISK,
            uriString = "content://media/disk",
            importedPath = "/tmp/imports/disks/demo.atr",
            displayName = "demo.atr",
            lastUpdatedEpochMillis = 100L,
        )

        store.saveSelection(MediaRole.DISK, diskSelection)
        assertEquals(diskSelection, store.loadSelection(MediaRole.DISK))

        store.clearSelection(MediaRole.DISK)

        assertEquals(null, store.loadSelection(MediaRole.DISK))
        assertFalse(store.loadAllSelections().containsKey(MediaRole.DISK))
    }

    @Test
    fun runtimePathsCreateRoleDirectories() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))

        runtimePaths.ensureDirectories()

        assertTrue(runtimePaths.rootDirectory.isDirectory)
        assertTrue(runtimePaths.importsDirectory.isDirectory)
        assertTrue(runtimePaths.disksDirectory.isDirectory)
        assertTrue(runtimePaths.cartsDirectory.isDirectory)
        assertTrue(runtimePaths.executablesDirectory.isDirectory)
        assertTrue(runtimePaths.romsDirectory.isDirectory)
        assertTrue(runtimePaths.stagedAssetsDirectory.isDirectory)
        assertTrue(runtimePaths.configDirectory.isDirectory)
    }
}
