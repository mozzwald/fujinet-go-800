package com.mantismoonlabs.fujinetgo800.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BundledAssetInstallerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun ensureInstalledCopiesMissingAssets() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val assets = listOf(
            BundledAssetInstaller.BundledAsset("support/defaults.txt", "default support".toByteArray()),
            BundledAssetInstaller.BundledAsset("support/layouts/slots.txt", "slot metadata".toByteArray()),
        )
        val installer = BundledAssetInstaller(
            runtimePaths = runtimePaths,
            version = "baseline-v1",
            bundledAssets = assets,
        )

        val result = installer.ensureInstalled()

        assertTrue(result.installedFiles.contains("support/defaults.txt"))
        assertTrue(result.installedFiles.contains("support/layouts/slots.txt"))
        assertEquals("default support", runtimePaths.stagedAssetsDirectory.resolve("support/defaults.txt").readText())
        assertEquals("slot metadata", runtimePaths.stagedAssetsDirectory.resolve("support/layouts/slots.txt").readText())
        assertEquals("baseline-v1", runtimePaths.bundledAssetVersionFile.readText().trim())
        assertTrue(runtimePaths.bundledAssetManifestFile.isFile)
    }

    @Test
    fun ensureInstalledIsIdempotent() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val installer = BundledAssetInstaller(
            runtimePaths = runtimePaths,
            version = "baseline-v1",
            bundledAssets = listOf(
                BundledAssetInstaller.BundledAsset("support/defaults.txt", "default support".toByteArray()),
            ),
        )

        installer.ensureInstalled()
        val secondRun = installer.ensureInstalled()

        assertTrue(secondRun.installedFiles.isEmpty())
        assertFalse(secondRun.versionChanged)
        assertEquals("default support", runtimePaths.stagedAssetsDirectory.resolve("support/defaults.txt").readText())
    }

    @Test
    fun installerWritesVersionMarker() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val firstInstaller = BundledAssetInstaller(
            runtimePaths = runtimePaths,
            version = "baseline-v1",
            bundledAssets = listOf(
                BundledAssetInstaller.BundledAsset("support/defaults.txt", "v1".toByteArray()),
            ),
        )
        val secondInstaller = BundledAssetInstaller(
            runtimePaths = runtimePaths,
            version = "baseline-v2",
            bundledAssets = listOf(
                BundledAssetInstaller.BundledAsset("support/defaults.txt", "v2".toByteArray()),
            ),
        )

        firstInstaller.ensureInstalled()
        runtimePaths.stagedAssetsDirectory.resolve("stale.txt").writeText("stale")

        val result = secondInstaller.ensureInstalled()

        assertTrue(result.versionChanged)
        assertEquals("baseline-v2", runtimePaths.bundledAssetVersionFile.readText().trim())
        assertEquals("v2", runtimePaths.stagedAssetsDirectory.resolve("support/defaults.txt").readText())
        assertFalse(runtimePaths.stagedAssetsDirectory.resolve("stale.txt").exists())
    }
}
