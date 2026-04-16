package com.mantismoonlabs.fujinetgo800.storage

import com.mantismoonlabs.fujinetgo800.BuildConfig
import com.mantismoonlabs.fujinetgo800.settings.SystemRomKind
import java.util.Properties

data class SystemRomSelection(
    val uriString: String,
    val importedPath: String,
    val displayName: String,
    val lastUpdatedEpochMillis: Long,
)

class SystemRomDocumentStore(
    private val runtimePaths: RuntimePaths,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun saveSelection(kind: SystemRomKind, selection: SystemRomSelection) {
        val savedSelections = loadAllSelections().toMutableMap()
        val savedSelection = if (selection.lastUpdatedEpochMillis > 0L) {
            selection
        } else {
            selection.copy(lastUpdatedEpochMillis = clock())
        }
        savedSelections[kind] = savedSelection
        persistSelections(savedSelections)
    }

    fun loadSelection(kind: SystemRomKind): SystemRomSelection? = loadAllSelections()[kind]

    fun clearSelection(kind: SystemRomKind) {
        val savedSelections = loadAllSelections().toMutableMap()
        savedSelections.remove(kind)
        persistSelections(savedSelections)
    }

    fun loadAllSelections(): Map<SystemRomKind, SystemRomSelection> {
        runtimePaths.ensureDirectories()
        if (!runtimePaths.systemRomSelectionsFile.exists()) {
            return emptyMap()
        }

        return runtimePaths.systemRomSelectionsFile.inputStream().use { input ->
            val properties = Properties().apply { load(input) }
            SystemRomKind.entries.mapNotNull { kind ->
                val prefix = "${kind.name.lowercase()}."
                SystemRomSelection(
                    uriString = properties.getProperty("${prefix}$URI_STRING_KEY") ?: return@mapNotNull null,
                    importedPath = properties.getProperty("${prefix}$IMPORTED_PATH_KEY") ?: return@mapNotNull null,
                    displayName = properties.getProperty("${prefix}$DISPLAY_NAME_KEY") ?: return@mapNotNull null,
                    lastUpdatedEpochMillis = properties.getProperty("${prefix}$LAST_UPDATED_KEY")
                        ?.toLongOrNull()
                        ?: return@mapNotNull null,
                ).let { kind to it }
            }.toMap()
        }
    }

    private fun persistSelections(selections: Map<SystemRomKind, SystemRomSelection>) {
        runtimePaths.ensureDirectories()
        val properties = Properties().apply {
            setProperty(SCHEMA_VERSION_KEY, SCHEMA_VERSION.toString())
            selections.entries
                .sortedBy { it.key.name }
                .forEach { (kind, selection) ->
                    val prefix = "${kind.name.lowercase()}."
                    setProperty("${prefix}$URI_STRING_KEY", selection.uriString)
                    setProperty("${prefix}$IMPORTED_PATH_KEY", selection.importedPath)
                    setProperty("${prefix}$DISPLAY_NAME_KEY", selection.displayName)
                    setProperty("${prefix}$LAST_UPDATED_KEY", selection.lastUpdatedEpochMillis.toString())
                }
        }

        runtimePaths.systemRomSelectionsFile.outputStream().use { output ->
            properties.store(output, BuildConfig.BRAND_SYSTEM_ROM_SELECTION_COMMENT)
        }
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val SCHEMA_VERSION_KEY = "schemaVersion"
        const val URI_STRING_KEY = "uriString"
        const val IMPORTED_PATH_KEY = "importedPath"
        const val DISPLAY_NAME_KEY = "displayName"
        const val LAST_UPDATED_KEY = "lastUpdatedEpochMillis"
    }
}
