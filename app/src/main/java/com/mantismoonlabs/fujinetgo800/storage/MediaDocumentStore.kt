package com.mantismoonlabs.fujinetgo800.storage

import com.mantismoonlabs.fujinetgo800.BuildConfig
import java.util.Properties

class MediaDocumentStore(
    private val runtimePaths: RuntimePaths,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun saveSelection(role: MediaRole, selection: MediaSelection) {
        val savedSelections = loadAllSelections().toMutableMap()
        val savedSelection = if (selection.lastUpdatedEpochMillis > 0L) {
            selection
        } else {
            selection.copy(lastUpdatedEpochMillis = clock())
        }
        savedSelections[role] = savedSelection
        persistSelections(savedSelections)
    }

    fun loadSelection(role: MediaRole): MediaSelection? = loadAllSelections()[role]

    fun clearSelection(role: MediaRole) {
        val savedSelections = loadAllSelections().toMutableMap()
        savedSelections.remove(role)
        persistSelections(savedSelections)
    }

    fun loadAllSelections(): Map<MediaRole, MediaSelection> {
        runtimePaths.ensureDirectories()
        if (!runtimePaths.mediaSelectionsFile.exists()) {
            return emptyMap()
        }

        return runtimePaths.mediaSelectionsFile.inputStream().use { input ->
            val properties = Properties().apply { load(input) }
            MediaRole.entries.mapNotNull { role ->
                val prefix = "${role.name.lowercase()}."
                val mediaKind = properties.getProperty("${prefix}$MEDIA_KIND_KEY") ?: return@mapNotNull null
                MediaSelection(
                    mediaKind = MediaKind.valueOf(mediaKind.uppercase()),
                    uriString = properties.getProperty("${prefix}$URI_STRING_KEY") ?: return@mapNotNull null,
                    importedPath = properties.getProperty("${prefix}$IMPORTED_PATH_KEY") ?: return@mapNotNull null,
                    displayName = properties.getProperty("${prefix}$DISPLAY_NAME_KEY") ?: return@mapNotNull null,
                    lastUpdatedEpochMillis = properties.getProperty("${prefix}$LAST_UPDATED_KEY")
                        ?.toLongOrNull()
                        ?: return@mapNotNull null,
                ).let { role to it }
            }.toMap()
        }
    }

    private fun persistSelections(selections: Map<MediaRole, MediaSelection>) {
        runtimePaths.ensureDirectories()
        val properties = Properties().apply {
            setProperty(SCHEMA_VERSION_KEY, SCHEMA_VERSION.toString())
            selections.entries
                .sortedBy { it.key.name }
                .forEach { (role, selection) ->
                    val prefix = "${role.name.lowercase()}."
                    setProperty("${prefix}$MEDIA_KIND_KEY", selection.mediaKind.name.lowercase())
                    setProperty("${prefix}$URI_STRING_KEY", selection.uriString)
                    setProperty("${prefix}$IMPORTED_PATH_KEY", selection.importedPath)
                    setProperty("${prefix}$DISPLAY_NAME_KEY", selection.displayName)
                    setProperty("${prefix}$LAST_UPDATED_KEY", selection.lastUpdatedEpochMillis.toString())
                }
        }

        runtimePaths.mediaSelectionsFile.outputStream().use { output ->
            properties.store(output, BuildConfig.BRAND_MEDIA_SELECTION_COMMENT)
        }
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val SCHEMA_VERSION_KEY = "schemaVersion"
        const val MEDIA_KIND_KEY = "mediaKind"
        const val URI_STRING_KEY = "uriString"
        const val IMPORTED_PATH_KEY = "importedPath"
        const val DISPLAY_NAME_KEY = "displayName"
        const val LAST_UPDATED_KEY = "lastUpdatedEpochMillis"
    }
}
