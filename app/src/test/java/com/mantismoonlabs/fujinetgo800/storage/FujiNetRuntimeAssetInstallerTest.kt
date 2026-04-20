package com.mantismoonlabs.fujinetgo800.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FujiNetRuntimeAssetInstallerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun stagesFujiNetRuntimeLayout() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val installer = FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = "fujinet-v1",
            bundledAssets = listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset("data/www/index.html", "<html>config</html>".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("SD/AUTORUN.ATR", "atr".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("fnconfig.ini", "[network]\n".toByteArray()),
            ),
        )

        val result = installer.ensureInstalled()

        assertTrue(result.installedFiles.contains("data/www/index.html"))
        assertTrue(result.installedFiles.contains("SD/AUTORUN.ATR"))
        assertTrue(result.installedFiles.contains("fnconfig.ini"))
        assertEquals("<html>config</html>", runtimePaths.fujiNetDataDirectory.resolve("www/index.html").readText())
        assertEquals("atr", runtimePaths.fujiNetSdDirectory.resolve("AUTORUN.ATR").readText())
        assertEquals("[network]\n", runtimePaths.fujiNetConfigFile.readText())
        assertEquals("fujinet-v1", runtimePaths.fujiNetVersionFile.readText().trim())
        assertTrue(runtimePaths.fujiNetManifestFile.isFile)
        assertFalse(runtimePaths.stagedAssetsDirectory.resolve("data/www/index.html").exists())
        assertFalse(runtimePaths.stagedAssetsDirectory.resolve("SD/AUTORUN.ATR").exists())
    }

    @Test
    fun versionChangeRefreshesBundledAssetsWithoutWipingLiveContent() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val firstInstaller = FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = "fujinet-v1",
            bundledAssets = listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset("data/persist.txt", "v1".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("data/old-default.txt", "old".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("fnconfig.ini", "version=1\n".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("data/fnconfig.ini", "data-version=1\n".toByteArray()),
            ),
        )
        val secondInstaller = FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = "fujinet-v2",
            bundledAssets = listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset("data/persist.txt", "v2".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("fnconfig.ini", "version=2\n".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("data/fnconfig.ini", "data-version=2\n".toByteArray()),
            ),
        )

        firstInstaller.ensureInstalled()
        runtimePaths.fujiNetRuntimeDirectory.resolve("user-created.txt").writeText("stale")
        runtimePaths.fujiNetConfigFile.writeText("custom=1\n")
        runtimePaths.fujiNetDataDirectory.resolve("fnconfig.ini").writeText("custom-data=1\n")

        val result = secondInstaller.ensureInstalled()

        assertTrue(result.versionChanged)
        assertEquals("fujinet-v2", runtimePaths.fujiNetVersionFile.readText().trim())
        assertEquals("v2", runtimePaths.fujiNetDataDirectory.resolve("persist.txt").readText())
        assertEquals("custom=1\n", runtimePaths.fujiNetConfigFile.readText())
        assertEquals("custom-data=1\n", runtimePaths.fujiNetDataDirectory.resolve("fnconfig.ini").readText())
        assertFalse(runtimePaths.fujiNetRuntimeDirectory.resolve("data/old-default.txt").exists())
        assertTrue(runtimePaths.fujiNetRuntimeDirectory.resolve("user-created.txt").exists())
        assertEquals("version.txt", runtimePaths.fujiNetVersionFile.name)
    }

    @Test
    fun restartDoesNotOverwriteExistingLiveConfig() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val installer = FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = "fujinet-v1",
            bundledAssets = listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset("data/www/index.html", "<html>v1</html>".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("fnconfig.ini", "default=1\n".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("data/fnconfig.ini", "data-default=1\n".toByteArray()),
            ),
        )

        installer.ensureInstalled()
        runtimePaths.fujiNetConfigFile.writeText("saved=1\n")
        runtimePaths.fujiNetDataDirectory.resolve("fnconfig.ini").writeText("data-saved=1\n")

        val result = installer.ensureInstalled()

        assertFalse(result.versionChanged)
        assertEquals("saved=1\n", runtimePaths.fujiNetConfigFile.readText())
        assertEquals("data-saved=1\n", runtimePaths.fujiNetDataDirectory.resolve("fnconfig.ini").readText())
        assertFalse(result.installedFiles.contains("fnconfig.ini"))
        assertFalse(result.installedFiles.contains("data/fnconfig.ini"))
    }

    @Test
    fun exposesRuntimePathsForServiceLaunch() {
        val runtimePaths = RuntimePaths(temporaryFolder.newFolder("runtime"))
        val installer = FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = "fujinet-v1",
            bundledAssets = listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset("data/bootstrap.atr", "boot".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("SD/README.txt", "sd".toByteArray()),
                FujiNetRuntimeAssetInstaller.BundledAsset("fnconfig.ini", "mode=fujinet\n".toByteArray()),
            ),
        )

        val result = installer.ensureInstalled()

        assertEquals(runtimePaths.fujiNetRuntimeDirectory, result.runtimeRootDirectory)
        assertEquals(runtimePaths.fujiNetDataDirectory, result.dataDirectory)
        assertEquals(runtimePaths.fujiNetSdDirectory, result.sdDirectory)
        assertEquals(runtimePaths.fujiNetConfigFile, result.configFile)
        assertTrue(result.dataDirectory.isDirectory)
        assertTrue(result.sdDirectory.isDirectory)
        assertTrue(result.configFile.isFile)
    }

    @Test
    fun migratesLegacyPrivateRuntimeIntoVisibleStorageWithoutOverwritingNewerFiles() {
        val privateRoot = temporaryFolder.newFolder("private-root")
        val visibleRoot = temporaryFolder.newFolder("visible-root")
        val runtimePaths = RuntimePaths(
            rootDirectory = privateRoot,
            fujiNetWritableRootDirectory = visibleRoot,
            fujiNetLegacyRuntimeDirectory = privateRoot.resolve("fujinet"),
        )
        runtimePaths.fujiNetLegacyRuntimeDirectory.resolve("data").mkdirs()
        runtimePaths.fujiNetLegacyRuntimeDirectory.resolve("SD").mkdirs()
        runtimePaths.fujiNetLegacyRuntimeDirectory.resolve("fnconfig.ini").writeText("legacy=1\n")
        runtimePaths.fujiNetLegacyRuntimeDirectory.resolve("data/history.txt").writeText("legacy-data")
        runtimePaths.fujiNetRuntimeDirectory.mkdirs()
        runtimePaths.fujiNetRuntimeDirectory.resolve("SD").mkdirs()
        runtimePaths.fujiNetRuntimeDirectory.resolve("fnconfig.ini").writeText("visible=1\n")
        runtimePaths.fujiNetRuntimeDirectory.resolve("SD/existing.txt").writeText("visible-sd")

        val installer = FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = "fujinet-v1",
            bundledAssets = listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset("data/www/index.html", "<html>ok</html>".toByteArray()),
            ),
        )

        installer.ensureInstalled()

        assertEquals("visible=1\n", runtimePaths.fujiNetConfigFile.readText())
        assertEquals("legacy-data", runtimePaths.fujiNetDataDirectory.resolve("history.txt").readText())
        assertEquals("visible-sd", runtimePaths.fujiNetSdDirectory.resolve("existing.txt").readText())
        assertEquals("<html>ok</html>", runtimePaths.fujiNetDataDirectory.resolve("www/index.html").readText())
    }

    @Test
    fun migratesLegacyExternalMediaRuntimeIntoExternalFilesStorage() {
        val privateRoot = temporaryFolder.newFolder("private-root")
        val visibleRoot = temporaryFolder.newFolder("visible-root")
        val legacyExternalMediaRoot = temporaryFolder.newFolder("legacy-external-media")
        val legacyExternalMediaRuntime = legacyExternalMediaRoot.resolve("fujinet")
        val runtimePaths = RuntimePaths(
            rootDirectory = privateRoot,
            fujiNetWritableRootDirectory = visibleRoot,
            fujiNetLegacyRuntimeDirectory = privateRoot.resolve("fujinet"),
            fujiNetLegacyRuntimeDirectories = listOf(
                privateRoot.resolve("fujinet"),
                legacyExternalMediaRuntime,
            ),
        )
        legacyExternalMediaRuntime.resolve("data").mkdirs()
        legacyExternalMediaRuntime.resolve("SD").mkdirs()
        legacyExternalMediaRuntime.resolve("fnconfig.ini").writeText("legacy-media=1\n")
        legacyExternalMediaRuntime.resolve("SD/autorun.atr").writeText("legacy-atr")

        val installer = FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = "fujinet-v1",
            bundledAssets = listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset("data/www/index.html", "<html>ok</html>".toByteArray()),
            ),
        )

        installer.ensureInstalled()

        assertEquals("legacy-media=1\n", runtimePaths.fujiNetConfigFile.readText())
        assertEquals("legacy-atr", runtimePaths.fujiNetSdDirectory.resolve("autorun.atr").readText())
        assertEquals("<html>ok</html>", runtimePaths.fujiNetDataDirectory.resolve("www/index.html").readText())
    }

    @Test
    fun migrationSkipsNestedRuntimeCloneLeftByBrokenVisibleStoragePath() {
        val privateRoot = temporaryFolder.newFolder("private-root")
        val visibleRoot = temporaryFolder.newFolder("visible-root")
        val legacyVisibleRoot = temporaryFolder.newFolder("legacy-visible-root")
        val nestedClone = legacyVisibleRoot.resolve(visibleRoot.name)

        legacyVisibleRoot.resolve("data").mkdirs()
        legacyVisibleRoot.resolve("SD").mkdirs()
        legacyVisibleRoot.resolve("fnconfig.ini").writeText("legacy-visible=1\n")
        legacyVisibleRoot.resolve("SD/autorun.atr").writeText("legacy-atr")

        nestedClone.resolve("data").mkdirs()
        nestedClone.resolve("SD").mkdirs()
        nestedClone.resolve("fnconfig.ini").writeText("broken-nested=1\n")
        nestedClone.resolve("SD/nested.atr").writeText("nested-atr")

        val runtimePaths = RuntimePaths(
            rootDirectory = privateRoot,
            fujiNetWritableRootDirectory = visibleRoot,
            fujiNetLegacyRuntimeDirectory = privateRoot.resolve("fujinet"),
            fujiNetLegacyRuntimeDirectories = listOf(
                privateRoot.resolve("fujinet"),
                legacyVisibleRoot,
            ),
        )

        val installer = FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = "fujinet-v1",
            bundledAssets = listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset("data/www/index.html", "<html>ok</html>".toByteArray()),
            ),
        )

        installer.ensureInstalled()

        assertFalse(runtimePaths.fujiNetRuntimeDirectory.resolve(runtimePaths.fujiNetRuntimeDirectory.name).exists())
        assertEquals("legacy-visible=1\n", runtimePaths.fujiNetConfigFile.readText())
        assertEquals("legacy-atr", runtimePaths.fujiNetSdDirectory.resolve("autorun.atr").readText())
        assertFalse(runtimePaths.fujiNetSdDirectory.resolve("nested.atr").exists())
    }
}
