package com.mantismoonlabs.fujinetgo800.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mantismoonlabs.fujinetgo800.session.LocalMediaUiState
import com.mantismoonlabs.fujinetgo800.session.MediaSlotUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class LocalMediaViewModel(
    private val runtimePaths: RuntimePaths,
    private val documentStore: MediaDocumentStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocalMediaUiState())
    val uiState: StateFlow<LocalMediaUiState> = _uiState.asStateFlow()

    private val _pickerRequests = MutableSharedFlow<MediaRole>(extraBufferCapacity = 1)
    val pickerRequests: SharedFlow<MediaRole> = _pickerRequests.asSharedFlow()

    init {
        refreshSelections()
    }

    fun onDiskPickRequested() {
        _pickerRequests.tryEmit(MediaRole.DISK)
    }

    fun onCartridgePickRequested() {
        _pickerRequests.tryEmit(MediaRole.CARTRIDGE)
    }

    fun onExecutablePickRequested() {
        _pickerRequests.tryEmit(MediaRole.EXECUTABLE)
    }

    fun onRomPickRequested() {
        _pickerRequests.tryEmit(MediaRole.ROM)
    }

    fun onDocumentPicked(
        role: MediaRole,
        uriString: String,
        displayName: String,
    ) {
        val normalizedName = displayName.ifBlank { defaultDisplayName(role) }
        val selection = MediaSelection(
            mediaKind = role.toMediaKind(),
            uriString = uriString,
            importedPath = runtimePaths.importDirectoryFor(role)
                .resolve(normalizedName.sanitizeForImport())
                .absolutePath,
            displayName = normalizedName,
            lastUpdatedEpochMillis = clock(),
        )
        documentStore.saveSelection(role, selection)
        refreshSelections()
    }

    fun clearSelection(role: MediaRole) {
        documentStore.clearSelection(role)
        refreshSelections()
    }

    private fun refreshSelections() {
        runtimePaths.ensureDirectories()
        val selections = documentStore.loadAllSelections()
        val current = _uiState.value
        _uiState.value = current.copy(
            disk = current.disk.fromSelection(selections[MediaRole.DISK]),
            cartridge = current.cartridge.fromSelection(selections[MediaRole.CARTRIDGE]),
            executable = current.executable.fromSelection(selections[MediaRole.EXECUTABLE]),
            rom = current.rom.fromSelection(selections[MediaRole.ROM]),
        )
    }

    private fun MediaSlotUiState.fromSelection(selection: MediaSelection?): MediaSlotUiState {
        return copy(
            selectedLabel = selection?.displayName.orEmpty(),
            hasSelection = selection != null,
        )
    }

    private fun MediaRole.toMediaKind(): MediaKind = when (this) {
        MediaRole.DISK -> MediaKind.DISK
        MediaRole.CARTRIDGE -> MediaKind.CARTRIDGE
        MediaRole.EXECUTABLE -> MediaKind.EXECUTABLE
        MediaRole.ROM -> MediaKind.ROM
    }

    private fun defaultDisplayName(role: MediaRole): String = when (role) {
        MediaRole.DISK -> "disk.atr"
        MediaRole.CARTRIDGE -> "cartridge.car"
        MediaRole.EXECUTABLE -> "program.xex"
        MediaRole.ROM -> "custom-os.rom"
    }

    private fun String.sanitizeForImport(): String {
        return replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .ifBlank { "imported.bin" }
    }

    companion object {
        fun provideFactory(
            filesDirectory: File,
            externalMediaDirectory: File? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val runtimePaths = RuntimePaths.fromFilesDirectory(filesDirectory, externalMediaDirectory)
                LocalMediaViewModel(
                    runtimePaths = runtimePaths,
                    documentStore = MediaDocumentStore(runtimePaths),
                )
            }
        }
    }
}
