package com.mantismoonlabs.fujinetgo800.session

import com.mantismoonlabs.fujinetgo800.storage.FujiNetRuntimeAssetInstaller
import java.io.IOException

internal class FujiNetBundledAssetLoader(
    private val listChildren: (String) -> List<String>?,
    private val openAsset: (String) -> ByteArray,
) {
    fun load(): List<FujiNetRuntimeAssetInstaller.BundledAsset> {
        val prefixedAssets = collectTree(rootPath = "fujinet")
        if (prefixedAssets.isNotEmpty()) {
            return prefixedAssets.sortedBy { it.relativePath }
        }

        return ROOT_LAYOUT_PATHS
            .flatMap { rootPath -> collectExistingPath(assetPath = rootPath, relativePath = rootPath) }
            .sortedBy { it.relativePath }
    }

    private fun collectTree(
        rootPath: String,
        relativePath: String = "",
    ): List<FujiNetRuntimeAssetInstaller.BundledAsset> {
        val assetPath = if (relativePath.isEmpty()) {
            rootPath
        } else {
            "$rootPath/$relativePath"
        }
        val childNames = listChildren(assetPath)?.filter { it.isNotBlank() }.orEmpty()
        if (childNames.isEmpty()) {
            return openExistingAsset(assetPath, relativePath).orEmpty()
        }

        return childNames.flatMap { childName ->
            val childRelativePath = if (relativePath.isEmpty()) {
                childName
            } else {
                "$relativePath/$childName"
            }
            collectTree(rootPath = rootPath, relativePath = childRelativePath)
        }
    }

    private fun collectExistingPath(
        assetPath: String,
        relativePath: String,
    ): List<FujiNetRuntimeAssetInstaller.BundledAsset> {
        val childNames = listChildren(assetPath)?.filter { it.isNotBlank() }.orEmpty()
        if (childNames.isEmpty()) {
            return openExistingAsset(assetPath, relativePath).orEmpty()
        }

        return childNames.flatMap { childName ->
            val childAssetPath = "$assetPath/$childName"
            val childRelativePath = "$relativePath/$childName"
            collectExistingPath(assetPath = childAssetPath, relativePath = childRelativePath)
        }
    }

    private fun openExistingAsset(
        assetPath: String,
        relativePath: String,
    ): List<FujiNetRuntimeAssetInstaller.BundledAsset>? =
        try {
            listOf(
                FujiNetRuntimeAssetInstaller.BundledAsset(
                    relativePath = relativePath,
                    bytes = openAsset(assetPath),
                ),
            )
        } catch (_: IOException) {
            null
        }

    private companion object {
        val ROOT_LAYOUT_PATHS = listOf("fnconfig.ini", "data", "SD")
    }
}
