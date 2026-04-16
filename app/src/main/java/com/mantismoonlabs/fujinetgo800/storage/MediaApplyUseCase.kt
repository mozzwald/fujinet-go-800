package com.mantismoonlabs.fujinetgo800.storage

import android.content.ContentResolver
import android.net.Uri
import com.mantismoonlabs.fujinetgo800.session.SessionCommand
import java.io.File

class MediaApplyUseCase(
    private val importer: MediaImporter,
) {
    fun prepareApply(selection: MediaSelection): PreparedMediaCommand {
        val importedSelection = importer.import(selection)
        val command = when (selection.mediaKind) {
            MediaKind.DISK -> SessionCommand.MountDisk(importedSelection.importedPath)
            MediaKind.CARTRIDGE -> SessionCommand.InsertCartridge(importedSelection.importedPath)
            MediaKind.EXECUTABLE -> SessionCommand.LoadExecutable(importedSelection.importedPath)
            MediaKind.ROM -> SessionCommand.ApplyCustomRom(importedSelection.importedPath)
        }
        return PreparedMediaCommand(
            importedSelection = importedSelection,
            command = command,
            requiresReset = selection.mediaKind == MediaKind.ROM,
        )
    }

    fun clearCommand(role: MediaRole): SessionCommand? = when (role) {
        MediaRole.DISK -> SessionCommand.EjectDisk
        MediaRole.CARTRIDGE -> SessionCommand.RemoveCartridge
        MediaRole.EXECUTABLE -> null
        MediaRole.ROM -> SessionCommand.ClearCustomRom
    }

    data class PreparedMediaCommand(
        val importedSelection: MediaSelection,
        val command: SessionCommand,
        val requiresReset: Boolean,
    )

    fun interface MediaImporter {
        fun import(selection: MediaSelection): MediaSelection
    }

    class ContentResolverImporter(
        private val contentResolver: ContentResolver,
    ) : MediaImporter {
        override fun import(selection: MediaSelection): MediaSelection {
            val uri = Uri.parse(selection.uriString)
            val destination = File(selection.importedPath)
            destination.parentFile?.mkdirs()
            contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open imported media URI: ${selection.uriString}")
            return selection.copy(importedPath = destination.absolutePath)
        }
    }
}
