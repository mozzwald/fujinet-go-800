package com.mantismoonlabs.fujinetgo800.storage

import android.content.Context
import com.mantismoonlabs.fujinetgo800.BuildConfig
import java.io.File

class RuntimePaths(
    val rootDirectory: File,
    val fujiNetWritableRootDirectory: File = rootDirectory.resolve("fujinet"),
    val fujiNetLegacyRuntimeDirectory: File = rootDirectory.resolve("fujinet"),
    val fujiNetLegacyRuntimeDirectories: List<File> = listOf(fujiNetLegacyRuntimeDirectory),
) {
    val importsDirectory: File = rootDirectory.resolve("imports")
    val disksDirectory: File = importsDirectory.resolve("disks")
    val cartsDirectory: File = importsDirectory.resolve("carts")
    val executablesDirectory: File = importsDirectory.resolve("executables")
    val romsDirectory: File = importsDirectory.resolve("roms")
    val hDevice1Directory: File = importsDirectory.resolve("h1")
    val hDevice2Directory: File = importsDirectory.resolve("h2")
    val hDevice3Directory: File = importsDirectory.resolve("h3")
    val hDevice4Directory: File = importsDirectory.resolve("h4")
    val stagedAssetsDirectory: File = rootDirectory.resolve("support")
    val configDirectory: File = rootDirectory.resolve("config")
    val mediaSelectionsFile: File = configDirectory.resolve("media-selections.json")
    val systemRomSelectionsFile: File = configDirectory.resolve("system-rom-selections.json")
    val bundledAssetVersionFile: File = stagedAssetsDirectory.resolve("version.txt")
    val bundledAssetManifestFile: File = stagedAssetsDirectory.resolve("manifest.json")
    val fujiNetRuntimeDirectory: File = fujiNetWritableRootDirectory
    val fujiNetDataDirectory: File = fujiNetRuntimeDirectory.resolve("data")
    val fujiNetSdDirectory: File = fujiNetRuntimeDirectory.resolve("SD")
    val fujiNetConfigFile: File = fujiNetRuntimeDirectory.resolve("fnconfig.ini")
    val fujiNetConsoleLogFile: File = fujiNetRuntimeDirectory.resolve("fujinet-console.log")
    val fujiNetVersionFile: File = fujiNetRuntimeDirectory.resolve("version.txt")
    val fujiNetManifestFile: File = fujiNetRuntimeDirectory.resolve("manifest.json")
    val fujiNetStorageDisplayPath: String = fujiNetRuntimeDirectory.absolutePath
    val fujiNetUsesVisibleStorage: Boolean = fujiNetRuntimeDirectory.absolutePath != fujiNetLegacyRuntimeDirectory.absolutePath

    fun ensureDirectories() {
        listOf(
            rootDirectory,
            importsDirectory,
            disksDirectory,
            cartsDirectory,
            executablesDirectory,
            romsDirectory,
            hDevice1Directory,
            hDevice2Directory,
            hDevice3Directory,
            hDevice4Directory,
            stagedAssetsDirectory,
            configDirectory,
            fujiNetRuntimeDirectory,
            fujiNetDataDirectory,
            fujiNetSdDirectory,
        ).forEach(::ensureDirectory)
    }

    fun importDirectoryFor(role: MediaRole): File = when (role) {
        MediaRole.DISK -> disksDirectory
        MediaRole.CARTRIDGE -> cartsDirectory
        MediaRole.EXECUTABLE -> executablesDirectory
        MediaRole.ROM -> romsDirectory
    }

    fun hDeviceDirectory(slot: Int): File = when (slot) {
        1 -> hDevice1Directory
        2 -> hDevice2Directory
        3 -> hDevice3Directory
        4 -> hDevice4Directory
        else -> error("Unsupported H: device slot $slot")
    }

    companion object {
        fun fromContext(context: Context): RuntimePaths {
            @Suppress("DEPRECATION")
            val legacyExternalMediaDirectory = context.getExternalMediaDirs().firstOrNull()
            return fromFilesDirectory(
                filesDirectory = context.filesDir,
                externalFilesDirectory = context.getExternalFilesDir(null),
                legacyExternalMediaDirectory = legacyExternalMediaDirectory,
            )
        }

        fun fromFilesDirectory(
            filesDirectory: File,
            externalFilesDirectory: File? = null,
            legacyExternalMediaDirectory: File? = null,
        ): RuntimePaths {
            val rootDirectory = filesDirectory.resolve("fujinetgo800")
            val legacyFujiNetDirectory = rootDirectory.resolve("fujinet")
            val visibleExternalMediaDirectory = legacyExternalMediaDirectory
                ?.resolve(BuildConfig.BRAND_EXTERNAL_MEDIA_DIR_NAME)
            val fujiNetDirectory = visibleExternalMediaDirectory
                ?: legacyFujiNetDirectory
            val legacyMigrationDirectories = buildList {
                add(legacyFujiNetDirectory)
                externalFilesDirectory
                    ?.resolve(BuildConfig.BRAND_EXTERNAL_MEDIA_DIR_NAME)
                    ?.takeIf { it.absolutePath != fujiNetDirectory.absolutePath }
                    ?.let(::add)
                legacyExternalMediaDirectory
                    ?.resolve("files")
                    ?.resolve(BuildConfig.BRAND_EXTERNAL_MEDIA_DIR_NAME)
                    ?.takeIf { it.absolutePath != fujiNetDirectory.absolutePath }
                    ?.let(::add)
                legacyExternalMediaDirectory
                    ?.resolve(BuildConfig.BRAND_EXTERNAL_MEDIA_DIR_NAME)
                    ?.takeIf { it.absolutePath != fujiNetDirectory.absolutePath }
                    ?.let(::add)
            }.distinctBy { it.absolutePath }
            return RuntimePaths(
                rootDirectory = rootDirectory,
                fujiNetWritableRootDirectory = fujiNetDirectory,
                fujiNetLegacyRuntimeDirectory = legacyFujiNetDirectory,
                fujiNetLegacyRuntimeDirectories = legacyMigrationDirectories,
            )
        }

        private fun ensureDirectory(directory: File) {
            if (directory.isDirectory) {
                return
            }
            check(directory.mkdirs()) { "Unable to create runtime directory: ${directory.absolutePath}" }
        }
    }
}
