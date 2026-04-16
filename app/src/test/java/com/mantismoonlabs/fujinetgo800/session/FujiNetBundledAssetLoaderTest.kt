package com.mantismoonlabs.fujinetgo800.session

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class FujiNetBundledAssetLoaderTest {
    @Test
    fun loadsAssetsFromRootLayout() {
        val loader = loaderFor(
            mapOf(
                "fnconfig.ini" to "config",
                "data/fnconfig.ini" to "data-config",
                "data/www/index.html" to "<html>",
                "SD/README.txt" to "sd",
            ),
        )

        val assets = loader.load()

        assertEquals(
            listOf(
                "SD/README.txt",
                "data/fnconfig.ini",
                "data/www/index.html",
                "fnconfig.ini",
            ),
            assets.map { it.relativePath },
        )
    }

    @Test
    fun prefersPrefixedLayoutWhenAvailable() {
        val loader = loaderFor(
            mapOf(
                "fujinet/fnconfig.ini" to "config",
                "fujinet/data/www/index.html" to "<html>",
                "fujinet/SD/README.txt" to "sd",
                "fnconfig.ini" to "wrong-root",
            ),
        )

        val assets = loader.load()

        assertEquals(
            listOf(
                "SD/README.txt",
                "data/www/index.html",
                "fnconfig.ini",
            ),
            assets.map { it.relativePath },
        )
    }

    @Test
    fun returnsEmptyWhenNoFujiNetAssetsExist() {
        val loader = loaderFor(
            mapOf(
                "other.txt" to "ignored",
            ),
        )

        assertEquals(emptyList<String>(), loader.load().map { it.relativePath })
    }

    private fun loaderFor(files: Map<String, String>): FujiNetBundledAssetLoader {
        val directories = buildSet {
            files.keys.forEach { path ->
                path.substringBeforeLast('/', missingDelimiterValue = "")
                    .split('/')
                    .filter { it.isNotBlank() }
                    .fold("") { current, segment ->
                        val next = if (current.isEmpty()) segment else "$current/$segment"
                        add(next)
                        next
                    }
            }
        }

        return FujiNetBundledAssetLoader(
            listChildren = { path ->
                val normalized = path.trim('/')
                val prefix = if (normalized.isEmpty()) "" else "$normalized/"
                val childNames = (files.keys + directories)
                    .filter { candidate ->
                        candidate.startsWith(prefix) && candidate != normalized
                    }
                    .map { candidate ->
                        candidate.removePrefix(prefix).substringBefore('/')
                    }
                    .distinct()
                if (childNames.isEmpty()) {
                    null
                } else {
                    childNames
                }
            },
            openAsset = { path ->
                files[path.trim('/')]?.toByteArray()
                    ?: throw IOException("Missing asset: $path")
            },
        )
    }
}
