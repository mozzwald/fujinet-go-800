package com.mantismoonlabs.fujinetgo800.storage

import com.mantismoonlabs.fujinetgo800.BuildConfig
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RuntimePathsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultsFujiNetRuntimeToPrivateStorageWhenExternalMediaIsUnavailable() {
        val filesDirectory = temporaryFolder.newFolder("files")

        val runtimePaths = RuntimePaths.fromFilesDirectory(filesDirectory)

        assertEquals(
            File(filesDirectory, "fujinetgo800/fujinet").absolutePath,
            runtimePaths.fujiNetRuntimeDirectory.absolutePath,
        )
        assertFalse(runtimePaths.fujiNetUsesVisibleStorage)
        assertEquals(
            runtimePaths.fujiNetLegacyRuntimeDirectory.absolutePath,
            runtimePaths.fujiNetRuntimeDirectory.absolutePath,
        )
        assertEquals(
            listOf(runtimePaths.fujiNetLegacyRuntimeDirectory.absolutePath),
            runtimePaths.fujiNetLegacyRuntimeDirectories.map { it.absolutePath },
        )
    }

    @Test
    fun usesAppScopedExternalFilesForFujiNetWritableRuntimeWhenAvailable() {
        val filesDirectory = temporaryFolder.newFolder("files")
        val externalFilesDirectory = temporaryFolder.newFolder("external-files")
        val legacyExternalMediaDirectory = temporaryFolder.newFolder("external-media")

        val runtimePaths = RuntimePaths.fromFilesDirectory(
            filesDirectory = filesDirectory,
            externalFilesDirectory = externalFilesDirectory,
            legacyExternalMediaDirectory = legacyExternalMediaDirectory,
        )

        assertEquals(
            File(externalFilesDirectory, BuildConfig.BRAND_EXTERNAL_MEDIA_DIR_NAME).absolutePath,
            runtimePaths.fujiNetRuntimeDirectory.absolutePath,
        )
        assertTrue(runtimePaths.fujiNetUsesVisibleStorage)
        assertEquals(
            File(filesDirectory, "fujinetgo800/fujinet").absolutePath,
            runtimePaths.fujiNetLegacyRuntimeDirectory.absolutePath,
        )
        assertEquals(
            listOf(
                File(filesDirectory, "fujinetgo800/fujinet").absolutePath,
                File(legacyExternalMediaDirectory, BuildConfig.BRAND_EXTERNAL_MEDIA_DIR_NAME).absolutePath,
            ),
            runtimePaths.fujiNetLegacyRuntimeDirectories.map { it.absolutePath },
        )
    }
}
