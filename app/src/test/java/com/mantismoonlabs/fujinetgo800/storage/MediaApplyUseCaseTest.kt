package com.mantismoonlabs.fujinetgo800.storage

import com.mantismoonlabs.fujinetgo800.session.SessionCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaApplyUseCaseTest {
    @Test
    fun diskSelectionsMountIntoDriveOne() {
        val useCase = MediaApplyUseCase(importer = noOpImporter())

        val prepared = useCase.prepareApply(
            MediaSelection(
                mediaKind = MediaKind.DISK,
                uriString = "content://provider/disks/demo.atr",
                importedPath = "/runtime/imports/disks/demo.atr",
                displayName = "demo.atr",
                lastUpdatedEpochMillis = 1L,
            ),
        )

        assertEquals(
            SessionCommand.MountDisk(importedPath = "/runtime/imports/disks/demo.atr", driveNumber = 1),
            prepared.command,
        )
    }

    @Test
    fun cartSelectionsInsertCartridge() {
        val useCase = MediaApplyUseCase(importer = noOpImporter())

        val prepared = useCase.prepareApply(
            MediaSelection(
                mediaKind = MediaKind.CARTRIDGE,
                uriString = "content://provider/carts/demo.car",
                importedPath = "/runtime/imports/carts/demo.car",
                displayName = "demo.car",
                lastUpdatedEpochMillis = 1L,
            ),
        )

        assertEquals(
            SessionCommand.InsertCartridge(importedPath = "/runtime/imports/carts/demo.car"),
            prepared.command,
        )
    }

    @Test
    fun clearingSelectionEjectsMountedMedia() {
        val useCase = MediaApplyUseCase(importer = noOpImporter())

        assertEquals(SessionCommand.EjectDisk, useCase.clearCommand(MediaRole.DISK))
        assertEquals(SessionCommand.RemoveCartridge, useCase.clearCommand(MediaRole.CARTRIDGE))
        assertEquals(SessionCommand.ClearCustomRom, useCase.clearCommand(MediaRole.ROM))
        assertEquals(null, useCase.clearCommand(MediaRole.EXECUTABLE))
    }

    @Test
    fun romSelectionsRequireReset() {
        val useCase = MediaApplyUseCase(importer = noOpImporter())

        val prepared = useCase.prepareApply(
            MediaSelection(
                mediaKind = MediaKind.ROM,
                uriString = "content://provider/roms/custom.rom",
                importedPath = "/runtime/imports/roms/custom.rom",
                displayName = "custom.rom",
                lastUpdatedEpochMillis = 1L,
            ),
        )

        assertEquals(
            SessionCommand.ApplyCustomRom(importedPath = "/runtime/imports/roms/custom.rom"),
            prepared.command,
        )
        assertTrue(prepared.requiresReset)
    }

    private fun noOpImporter(): MediaApplyUseCase.MediaImporter {
        return MediaApplyUseCase.MediaImporter { selection -> selection }
    }
}
