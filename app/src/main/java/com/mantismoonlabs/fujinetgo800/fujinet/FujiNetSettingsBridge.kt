package com.mantismoonlabs.fujinetgo800.fujinet

import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class FujiNetBootMode(val formValue: String) {
    CONFIG("0"),
    MOUNT_ALL("1"),
}

data class FujiNetPrinterModel(
    val value: String,
    val label: String,
)

data class FujiNetSettingsState(
    val runtimeStoragePath: String = "",
    val printerEnabled: Boolean = true,
    val printerPort: Int = 1,
    val printerModelValue: String = "",
    val printerModelLabel: String = "Default",
    val printerModels: List<FujiNetPrinterModel> = emptyList(),
    val hsioIndex: Int = 8,
    val configBootEnabled: Boolean = true,
    val configNgEnabled: Boolean = false,
    val statusWaitEnabled: Boolean = true,
    val bootMode: FujiNetBootMode = FujiNetBootMode.CONFIG,
    val webUiReachable: Boolean = false,
    val loadError: String? = null,
)

class FujiNetSettingsBridge(
    private val runtimePaths: RuntimePaths,
    private val baseUrl: String = "http://127.0.0.1:8000",
) {
    suspend fun load(): FujiNetSettingsState = withContext(Dispatchers.IO) {
        val fromFile = parseConfigFile(runtimePaths.fujiNetConfigFile)
        val html = fetchIndexHtml()
        if (html == null) {
            return@withContext fromFile.copy(
                runtimeStoragePath = runtimePaths.fujiNetStorageDisplayPath,
                webUiReachable = false,
                loadError = "Start a FujiNet session to use native FujiNet controls.",
            )
        }
        val parsed = parseIndexHtml(html)
        val models = parsed.printerModels.ifEmpty { fromFile.printerModels }
        val printerValue = parsed.printerModelValue?.takeUnless { it.isBlank() } ?: fromFile.printerModelValue
        val printerLabel = models.firstOrNull { it.value == printerValue }?.label
            ?: parsed.printerModelLabel
            ?: fromFile.printerModelLabel
        fromFile.copy(
            runtimeStoragePath = runtimePaths.fujiNetStorageDisplayPath,
            printerEnabled = parsed.printerEnabled ?: fromFile.printerEnabled,
            printerPort = parsed.printerPort ?: fromFile.printerPort,
            printerModelValue = printerValue,
            printerModelLabel = printerLabel,
            printerModels = models,
            hsioIndex = parsed.hsioIndex ?: fromFile.hsioIndex,
            configBootEnabled = parsed.configBootEnabled ?: fromFile.configBootEnabled,
            configNgEnabled = parsed.configNgEnabled ?: fromFile.configNgEnabled,
            statusWaitEnabled = parsed.statusWaitEnabled ?: fromFile.statusWaitEnabled,
            bootMode = parsed.bootMode ?: fromFile.bootMode,
            webUiReachable = true,
            loadError = null,
        )
    }

    suspend fun update(field: String, value: String): FujiNetSettingsState = withContext(Dispatchers.IO) {
        postForm(mapOf(field to value))
        load()
    }

    suspend fun swapDisks(): Boolean = withContext(Dispatchers.IO) {
        val connection = openConnection(URL("$baseUrl/swap")) ?: return@withContext false
        runCatching {
            connection.inputStream.bufferedReader(UTF_8).use { reader ->
                JSONObject(reader.readText()).optInt("result", -1) == 0
            }
        }.getOrDefault(false).also {
            connection.disconnect()
        }
    }

    private fun fetchIndexHtml(): String? {
        val connection = openConnection(URL("$baseUrl/")) ?: return null
        return runCatching {
            connection.inputStream.bufferedReader(UTF_8).use { it.readText() }
        }.getOrNull().also {
            connection.disconnect()
        }
    }

    private fun postForm(values: Map<String, String>) {
        val payload = values.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }.toByteArray(UTF_8)
        val connection = openConnection(URL("$baseUrl/config"), method = "POST") ?: return
        runCatching {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Content-Length", payload.size.toString())
            connection.outputStream.use { it.write(payload) }
            connection.inputStream.use { input -> while (input.read() != -1) { } }
        }
        connection.disconnect()
    }

    private fun openConnection(url: URL, method: String = "GET"): HttpURLConnection? {
        return runCatching {
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 1_500
                readTimeout = 1_500
                useCaches = false
            }
        }.getOrNull()
    }

    private fun parseConfigFile(file: File): FujiNetSettingsState {
        if (!file.isFile) {
            return FujiNetSettingsState(runtimeStoragePath = runtimePaths.fujiNetStorageDisplayPath)
        }
        val values = mutableMapOf<String, String>()
        var section = ""
        file.forEachLine { rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() || line.startsWith("#") || line.startsWith(";") -> Unit
                line.startsWith("[") && line.endsWith("]") -> section = line.removePrefix("[").removeSuffix("]")
                '=' in line -> {
                    val key = line.substringBefore('=').trim()
                    val value = line.substringAfter('=').trim()
                    values["$section.$key"] = value
                }
            }
        }
        return FujiNetSettingsState(
            runtimeStoragePath = runtimePaths.fujiNetStorageDisplayPath,
            printerEnabled = values["General.printer_enabled"].toBool(defaultValue = true),
            hsioIndex = values["General.hsioindex"]?.toIntOrNull() ?: 8,
            configBootEnabled = values["General.configenabled"].toBool(defaultValue = true),
            configNgEnabled = values["General.config_ng"].toBool(defaultValue = false),
            statusWaitEnabled = values["General.status_wait_enabled"].toBool(defaultValue = true),
            bootMode = values["General.boot_mode"].toBootMode(),
            webUiReachable = false,
        )
    }

    private fun parseIndexHtml(html: String): ParsedHtmlSettings {
        val printerModelsBlock = Regex(
            """<select name="printermodel1".*?>(.*?)</select>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)?.groupValues?.get(1).orEmpty()
        val printerModels = Regex("""<option value="([^"]*)">\s*([^<]+)\s*</option>""", RegexOption.IGNORE_CASE)
            .findAll(printerModelsBlock)
            .map { match ->
                FujiNetPrinterModel(
                    value = match.groupValues[1],
                    label = match.groupValues[2].trim(),
                )
            }
            .toList()
        return ParsedHtmlSettings(
            printerEnabled = html.extractJsInt("current_printer_enabled")?.let { it != 0 },
            printerPort = html.extractJsInt("current_printerport"),
            printerModelValue = html.extractJsString("current_printer").orEmpty(),
            printerModels = printerModels,
            hsioIndex = html.extractJsInt("current_hsioindex"),
            configBootEnabled = html.extractJsInt("current_config_enabled")?.let { it != 0 },
            configNgEnabled = html.extractJsInt("current_config_ng")?.let { it != 0 },
            statusWaitEnabled = html.extractJsInt("current_status_wait_enabled")?.let { it != 0 },
            bootMode = html.extractJsInt("current_boot_mode")?.let { if (it == 0) FujiNetBootMode.CONFIG else FujiNetBootMode.MOUNT_ALL },
        )
    }

    private data class ParsedHtmlSettings(
        val printerEnabled: Boolean? = null,
        val printerPort: Int? = null,
        val printerModelValue: String? = null,
        val printerModels: List<FujiNetPrinterModel> = emptyList(),
        val hsioIndex: Int? = null,
        val configBootEnabled: Boolean? = null,
        val configNgEnabled: Boolean? = null,
        val statusWaitEnabled: Boolean? = null,
        val bootMode: FujiNetBootMode? = null,
    ) {
        val printerModelLabel: String?
            get() = printerModels.firstOrNull { it.value == printerModelValue }?.label
    }
}

private fun String.urlEncode(): String = URLEncoder.encode(this, UTF_8.name())

private fun String?.toBool(defaultValue: Boolean): Boolean = when (this?.trim()) {
    "1", "true", "yes", "on" -> true
    "0", "false", "no", "off" -> false
    else -> defaultValue
}

private fun String?.toBootMode(): FujiNetBootMode = if (this?.trim() == FujiNetBootMode.MOUNT_ALL.formValue) {
    FujiNetBootMode.MOUNT_ALL
} else {
    FujiNetBootMode.CONFIG
}

private fun String.extractJsInt(name: String): Int? =
    Regex("var\\s+$name\\s*=\\s*\"([^\"]*)\"").find(this)?.groupValues?.get(1)?.toIntOrNull()

private fun String.extractJsString(name: String): String? =
    Regex("var\\s+$name\\s*=\\s*\"([^\"]*)\"").find(this)?.groupValues?.get(1)
