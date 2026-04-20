package com.mantismoonlabs.fujinetgo800.storage

import java.io.File
import java.security.MessageDigest

class FujiNetRuntimeAssetInstaller(
    private val runtimePaths: RuntimePaths,
    private val version: String = DEFAULT_VERSION,
    private val bundledAssets: List<BundledAsset> = emptyList(),
) {
    data class BundledAsset(
        val relativePath: String,
        val bytes: ByteArray,
    )

    data class InstallResult(
        val installedFiles: List<String>,
        val versionChanged: Boolean,
        val runtimeRootDirectory: File,
        val dataDirectory: File,
        val sdDirectory: File,
        val configFile: File,
    )

    fun ensureInstalled(): InstallResult {
        runtimePaths.ensureDirectories()
        migrateLegacyRuntimeIfNeeded()

        val versionChanged = runtimePaths.fujiNetVersionFile.readVersion() != version
        val previousManifest = runtimePaths.fujiNetManifestFile.readManifestEntries()
        if (versionChanged && previousManifest.isNotEmpty()) {
            removeRetiredBundledAssets(previousManifest)
        }

        val installedFiles = mutableListOf<String>()
        bundledAssets
            .sortedBy { it.relativePath }
            .forEach { asset ->
                val destination = runtimePaths.fujiNetRuntimeDirectory.resolve(asset.relativePath)
                val shouldWrite = when {
                    !destination.exists() -> true
                    versionChanged && !isMutableRuntimeAsset(asset.relativePath) -> true
                    else -> false
                }
                if (shouldWrite && destination.writeIfDifferent(asset.bytes)) {
                    installedFiles += asset.relativePath
                }
            }

        if (runtimePaths.fujiNetVersionFile.writeIfDifferent("$version\n".toByteArray())) {
            installedFiles += runtimePaths.fujiNetVersionFile.relativeTo(runtimePaths.fujiNetRuntimeDirectory)
                .invariantSeparatorsPath
        }
        if (runtimePaths.fujiNetManifestFile.writeIfDifferent(buildManifest().toByteArray())) {
            installedFiles += runtimePaths.fujiNetManifestFile.relativeTo(runtimePaths.fujiNetRuntimeDirectory)
                .invariantSeparatorsPath
        }

        return InstallResult(
            installedFiles = installedFiles,
            versionChanged = versionChanged,
            runtimeRootDirectory = runtimePaths.fujiNetRuntimeDirectory,
            dataDirectory = runtimePaths.fujiNetDataDirectory,
            sdDirectory = runtimePaths.fujiNetSdDirectory,
            configFile = runtimePaths.fujiNetConfigFile,
        )
    }

    private fun migrateLegacyRuntimeIfNeeded() {
        val liveRoot = runtimePaths.fujiNetRuntimeDirectory
        runtimePaths.fujiNetLegacyRuntimeDirectories.forEach { legacyRoot ->
            if (legacyRoot.absolutePath == liveRoot.absolutePath || !legacyRoot.exists()) {
                return@forEach
            }
            if (legacyRoot.isSameAsOrAncestorOf(liveRoot)) {
                return@forEach
            }
            copyRecursivelyPreservingLiveFiles(
                source = legacyRoot,
                destination = liveRoot,
            )
            // Startup should not block on recursively deleting old visible-storage trees.
            // Leave legacy directories in place after migration and only treat them as
            // read-once sources for missing files.
        }
    }

    private fun removeRetiredBundledAssets(previousManifest: Set<String>) {
        val currentPaths = bundledAssets.mapTo(linkedSetOf()) { it.relativePath }
        previousManifest
            .filterNot { path ->
                path in currentPaths || path in MUTABLE_RUNTIME_ASSET_PATHS || path in RESERVED_RUNTIME_PATHS
            }
            .forEach { relativePath ->
                runtimePaths.fujiNetRuntimeDirectory.resolve(relativePath).deleteRecursively()
            }
    }

    private fun copyRecursivelyPreservingLiveFiles(
        source: File,
        destination: File,
    ) {
        if (!source.exists()) {
            return
        }
        source.walkTopDown()
            .onEnter { current ->
                val relativePath = current.relativeTo(source).invariantSeparatorsPath
                if (relativePath == ".") {
                    return@onEnter true
                }
                // Older broken Android/media migrations could nest a clone of the runtime root
                // inside itself. Skip that subtree instead of copying it forward again.
                relativePath.substringBefore('/') != destination.name
            }
            .forEach { current ->
            val relativePath = current.relativeTo(source).invariantSeparatorsPath
            if (relativePath == ".") {
                destination.mkdirs()
                return@forEach
            }
            val target = destination.resolve(relativePath)
            if (current.isDirectory) {
                target.mkdirs()
                return@forEach
            }
            if (!target.exists()) {
                target.parentFile?.mkdirs()
                current.copyTo(target)
            }
            }
    }

    private fun File.isSameAsOrAncestorOf(other: File): Boolean {
        val normalizedThis = normalize().absoluteFile
        val normalizedOther = other.normalize().absoluteFile
        return normalizedOther.path == normalizedThis.path ||
            normalizedOther.path.startsWith(normalizedThis.path + File.separator)
    }

    private fun File.readVersion(): String? {
        if (!exists()) {
            return null
        }
        return readText().trim().ifEmpty { null }
    }

    private fun File.writeIfDifferent(bytes: ByteArray): Boolean {
        parentFile?.mkdirs()
        if (exists() && readBytes().contentEquals(bytes)) {
            return false
        }
        writeBytes(bytes)
        return true
    }

    private fun File.readManifestEntries(): Set<String> {
        if (!exists()) {
            return emptySet()
        }
        return readLines()
            .mapNotNull { line ->
                PATH_PATTERN.find(line)?.groupValues?.get(1)
            }
            .toSet()
    }

    private fun isMutableRuntimeAsset(relativePath: String): Boolean {
        return relativePath in MUTABLE_RUNTIME_ASSET_PATHS
    }

    private fun buildManifest(): String {
        val assetsJson = bundledAssets
            .sortedBy { it.relativePath }
            .joinToString(separator = ",\n") { asset ->
                """    {"path":"${asset.relativePath.escapeJson()}","sha256":"${asset.bytes.sha256()}","size":${asset.bytes.size}}"""
            }

        return buildString {
            appendLine("{")
            appendLine("  \"version\": \"${version.escapeJson()}\",")
            appendLine("  \"assets\": [")
            if (assetsJson.isNotBlank()) {
                appendLine(assetsJson)
            }
            appendLine("  ]")
            append('}')
        }
    }

    private fun ByteArray.sha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun String.escapeJson(): String {
        return replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private companion object {
        const val DEFAULT_VERSION = "fujinet-runtime-v1"
        val PATH_PATTERN = Regex(""""path":"([^"]+)"""")
        val MUTABLE_RUNTIME_ASSET_PATHS = setOf(
            "fnconfig.ini",
            "data/fnconfig.ini",
        )
        val RESERVED_RUNTIME_PATHS = setOf(
            "version.txt",
            "manifest.json",
        )
    }
}
