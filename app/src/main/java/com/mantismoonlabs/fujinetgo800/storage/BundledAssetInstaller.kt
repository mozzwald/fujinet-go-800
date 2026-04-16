package com.mantismoonlabs.fujinetgo800.storage

import java.io.File
import java.security.MessageDigest

class BundledAssetInstaller(
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
    )

    fun ensureInstalled(): InstallResult {
        runtimePaths.ensureDirectories()

        val versionChanged = runtimePaths.bundledAssetVersionFile.readVersion() != version
        if (versionChanged) {
            resetStagedAssetsDirectory()
        }

        val installedFiles = mutableListOf<String>()
        bundledAssets
            .sortedBy { it.relativePath }
            .forEach { asset ->
                val destination = runtimePaths.stagedAssetsDirectory.resolve(asset.relativePath)
                if (destination.writeIfDifferent(asset.bytes)) {
                    installedFiles += asset.relativePath
                }
            }

        if (runtimePaths.bundledAssetManifestFile.writeIfDifferent(buildManifest().toByteArray())) {
            installedFiles += runtimePaths.bundledAssetManifestFile.relativeTo(runtimePaths.stagedAssetsDirectory)
                .invariantSeparatorsPath
        }
        if (runtimePaths.bundledAssetVersionFile.writeIfDifferent("$version\n".toByteArray())) {
            installedFiles += runtimePaths.bundledAssetVersionFile.relativeTo(runtimePaths.stagedAssetsDirectory)
                .invariantSeparatorsPath
        }

        return InstallResult(
            installedFiles = installedFiles,
            versionChanged = versionChanged,
        )
    }

    private fun resetStagedAssetsDirectory() {
        runtimePaths.stagedAssetsDirectory.deleteRecursively()
        runtimePaths.stagedAssetsDirectory.mkdirs()
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
            appendLine(
                "  \"defaultBoot\": " +
                    "\"Preserve built-in Altirra default boot; no ROM extraction required in Phase 2.\","
            )
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

    private companion object {
        const val DEFAULT_VERSION = "phase2-baseline-v1"
    }
}
