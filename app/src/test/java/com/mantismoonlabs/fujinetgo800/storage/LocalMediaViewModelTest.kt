package com.mantismoonlabs.fujinetgo800.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalMediaViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun diskPickUpdatesState() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val viewModel = createViewModel(runtimePaths)

        viewModel.onDocumentPicked(
            role = MediaRole.DISK,
            uriString = "content://provider/disks/demo.atr",
            displayName = "Demo Disk.atr",
        )

        val diskState = viewModel.uiState.value.disk
        assertTrue(diskState.hasSelection)
        assertEquals("Demo Disk.atr", diskState.displayLabel)
        val storedSelection = MediaDocumentStore(runtimePaths).loadSelection(MediaRole.DISK)
        assertEquals("content://provider/disks/demo.atr", storedSelection?.uriString)
        assertTrue(storedSelection?.importedPath?.endsWith("/imports/disks/Demo_Disk.atr") == true)
    }

    @Test
    fun romPickUpdatesOnlyRomSlot() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val viewModel = createViewModel(runtimePaths)
        viewModel.onDocumentPicked(
            role = MediaRole.DISK,
            uriString = "content://provider/disks/demo.atr",
            displayName = "demo.atr",
        )

        viewModel.onDocumentPicked(
            role = MediaRole.ROM,
            uriString = "content://provider/roms/custom.rom",
            displayName = "custom.rom",
        )

        val state = viewModel.uiState.value
        assertTrue(state.disk.hasSelection)
        assertEquals("demo.atr", state.disk.displayLabel)
        assertTrue(state.rom.hasSelection)
        assertEquals("custom.rom", state.rom.displayLabel)
        assertFalse(state.cartridge.hasSelection)
        assertFalse(state.executable.hasSelection)
    }

    @Test
    fun clearingSlotRemovesSelection() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val viewModel = createViewModel(runtimePaths)
        viewModel.onDocumentPicked(
            role = MediaRole.CARTRIDGE,
            uriString = "content://provider/carts/demo.car",
            displayName = "demo.car",
        )
        assertTrue(viewModel.uiState.value.cartridge.hasSelection)

        viewModel.clearSelection(MediaRole.CARTRIDGE)

        assertFalse(viewModel.uiState.value.cartridge.hasSelection)
        assertEquals("No cartridge selected", viewModel.uiState.value.cartridge.displayLabel)
        assertEquals(null, MediaDocumentStore(runtimePaths).loadSelection(MediaRole.CARTRIDGE))
    }

    private fun createViewModel(runtimePaths: RuntimePaths): LocalMediaViewModel {
        return LocalMediaViewModel(
            runtimePaths = runtimePaths,
            documentStore = MediaDocumentStore(runtimePaths),
            clock = { 1234L },
        )
    }
}
