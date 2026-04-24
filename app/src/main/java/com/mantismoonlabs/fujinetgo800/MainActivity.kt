package com.mantismoonlabs.fujinetgo800

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.ViewCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mantismoonlabs.fujinetgo800.notifications.NotificationStartupGateState
import com.mantismoonlabs.fujinetgo800.notifications.determineNotificationStartupGateState
import com.mantismoonlabs.fujinetgo800.input.GameControllerMapper
import com.mantismoonlabs.fujinetgo800.input.HardwareKeyboardRouter
import com.mantismoonlabs.fujinetgo800.input.AndroidAtariKeyMapper
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetSettingsBridge
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetWebViewActivity
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.SystemRomKind
import com.mantismoonlabs.fujinetgo800.settings.requestedOrientationFor
import com.mantismoonlabs.fujinetgo800.session.EmulatorSessionService
import com.mantismoonlabs.fujinetgo800.session.LaunchSettingsViewModel
import com.mantismoonlabs.fujinetgo800.session.ServiceBackedSessionRepository
import com.mantismoonlabs.fujinetgo800.session.SessionCommand
import com.mantismoonlabs.fujinetgo800.session.SessionRepository
import com.mantismoonlabs.fujinetgo800.storage.LocalMediaViewModel
import com.mantismoonlabs.fujinetgo800.storage.MediaRole
import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import com.mantismoonlabs.fujinetgo800.storage.SystemRomDocumentStore
import com.mantismoonlabs.fujinetgo800.storage.SystemRomSelection
import com.mantismoonlabs.fujinetgo800.ui.EmulatorScreen
import com.mantismoonlabs.fujinetgo800.ui.NotificationPermissionGateScreen
import com.mantismoonlabs.fujinetgo800.ui.theme.Fuji800ATheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private var sessionRepository: SessionRepository? by mutableStateOf(null)
    private var emulationService: EmulatorSessionService? = null
    private var keyboardResetTrigger by mutableStateOf(0)
    private var isBound = false
    private var serviceStartRequested = false
    private var shutdownInProgress = false
    private var notificationGateState by mutableStateOf<NotificationStartupGateState>(
        NotificationStartupGateState.Loading,
    )
    private var pendingSystemRomKind: SystemRomKind? = null
    private var pendingHDeviceSlot: Int? = null
    private lateinit var localMediaViewModel: LocalMediaViewModel
    private lateinit var emulatorSettingsRepository: EmulatorSettingsRepository
    private val hardwareKeyboardRouter = HardwareKeyboardRouter(AndroidAtariKeyMapper())
    private val gameControllerMapper = GameControllerMapper()
    private val runtimePaths by lazy {
        RuntimePaths.fromContext(this)
    }
    private val fujiNetSettingsBridge by lazy {
        FujiNetSettingsBridge(runtimePaths)
    }
    private val systemRomDocumentStore by lazy {
        SystemRomDocumentStore(runtimePaths)
    }

    private val diskPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onDocumentPicked(MediaRole.DISK, uri)
    }
    private val cartridgePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onDocumentPicked(MediaRole.CARTRIDGE, uri)
    }
    private val executablePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onDocumentPicked(MediaRole.EXECUTABLE, uri)
    }
    private val romPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onDocumentPicked(MediaRole.ROM, uri)
    }
    private val systemRomPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val kind = pendingSystemRomKind
        pendingSystemRomKind = null
        onSystemRomPicked(kind, uri)
    }
    private val hDevicePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val slot = pendingHDeviceSlot
        pendingHDeviceSlot = null
        onHDeviceTreePicked(slot, uri)
    }
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            lifecycleScope.launch {
                emulatorSettingsRepository.updateNotificationPermissionEducationSeen(true)
                refreshNotificationStartupState()
                if (granted && notificationsReadyForForegroundService()) {
                    sessionRepository?.refreshNotification()
                }
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? EmulatorSessionService.LocalBinder ?: return
            val boundService = binder.service()
            emulationService = boundService
            val repository = ServiceBackedSessionRepository(boundService)
            sessionRepository = repository
            isBound = true
            repository.dispatch(SessionCommand.HostStarted)
            if (notificationsReadyForForegroundService()) {
                repository.refreshNotification()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            serviceStartRequested = false
            emulationService = null
            sessionRepository = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        allowContentInDisplayCutout()
        emulatorSettingsRepository = EmulatorSettingsRepository(this)
        localMediaViewModel = ViewModelProvider(
            this,
            LocalMediaViewModel.provideFactory(this),
        )[LocalMediaViewModel::class.java]
        observePickerRequests()
        observeHostDisplaySettings()
        setContent {
            Fuji800ATheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val repository = sessionRepository
                    when {
                        notificationGateState == NotificationStartupGateState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        notificationGateState != NotificationStartupGateState.Ready -> {
                            NotificationPermissionGateScreen(
                                gateState = notificationGateState,
                                onPrimaryAction = ::handleNotificationGatePrimaryAction,
                                onOpenSettings = ::openAppNotificationSettings,
                            )
                        }

                        repository == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        else -> {
                            EmulatorScreen(
                                settingsRepository = emulatorSettingsRepository,
                                sessionRepository = repository,
                                localMediaViewModel = localMediaViewModel,
                                keyboardResetTrigger = keyboardResetTrigger,
                                onClearMediaSelection = ::clearMediaSelection,
                                onPickSystemRom = ::pickSystemRom,
                                onClearSystemRom = ::clearSystemRom,
                                onOpenFujiNetWebUi = ::openFujiNetWebUi,
                                onSwapFujiNetDisks = ::swapFujiNetDisks,
                                onOpenFujiNetLogFile = ::openFujiNetLogFile,
                                onPickHDevice = ::pickHDeviceDirectory,
                                onClearHDevice = ::clearHDeviceDirectory,
                                onShutdownRequested = ::shutdownEmulatorAndExit,
                            )
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            refreshNotificationStartupState()
        }
    }

    private fun observeHostDisplaySettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                emulatorSettingsRepository.settings.collectLatest { settings ->
                    requestedOrientation = requestedOrientationFor(settings.orientationMode)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (notificationGateState == NotificationStartupGateState.Ready) {
            ensureServiceStartedAndBound()
        }
    }

    override fun onResume() {
        super.onResume()
        keyboardResetTrigger++
        enterImmersiveMode()
        lifecycleScope.launch {
            refreshNotificationStartupState()
        }
    }

    override fun onStop() {
        if (!shutdownInProgress) {
            sessionRepository?.dispatch(SessionCommand.HostStopped)
        }
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
            emulationService = null
        }
        super.onStop()
    }

    private fun shutdownEmulatorAndExit() {
        lifecycleScope.launch {
            shutdownInProgress = true
            val service = emulationService
            sessionRepository = null
            emulationService = null
            runCatching {
                service?.stopEmulationService()
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            keyboardResetTrigger++
            enterImmersiveMode()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val repository = sessionRepository
        val state = repository?.state?.value
        if (repository != null && state != null) {
            if (gameControllerMapper.handleButtonEvent(event, state, repository)) {
                return true
            }
            if (hardwareKeyboardRouter.route(event, state, repository)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val repository = sessionRepository
        val state = repository?.state?.value
        if (repository != null && state != null && gameControllerMapper.handleMotionEvent(event, state, repository)) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun enterImmersiveMode() {
        val decorView = window.decorView
        val controller = WindowInsetsControllerCompat(window, decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

        val rootInsets = ViewCompat.getRootWindowInsets(decorView)
        val imeVisible = rootInsets?.isVisible(WindowInsetsCompat.Type.ime()) == true
        val statusBarsVisible = rootInsets?.isVisible(WindowInsetsCompat.Type.statusBars()) != false
        val navigationBarsVisible = rootInsets?.isVisible(WindowInsetsCompat.Type.navigationBars()) != false

        if (imeVisible) {
            if (statusBarsVisible) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
            return
        }

        if (isSamsung) {
            if (statusBarsVisible) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
            return
        }

        if (statusBarsVisible || navigationBarsVisible) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun allowContentInDisplayCutout() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun handleNotificationGatePrimaryAction() {
        when (notificationGateState) {
            NotificationStartupGateState.EducationRequired,
            NotificationStartupGateState.PermissionRequired -> {
                lifecycleScope.launch {
                    emulatorSettingsRepository.updateNotificationPermissionEducationSeen(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        refreshNotificationStartupState()
                    }
                }
            }

            NotificationStartupGateState.AppNotificationsDisabled -> openAppNotificationSettings()
            NotificationStartupGateState.Loading,
            NotificationStartupGateState.Ready -> Unit
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun areAppNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    private fun notificationsReadyForForegroundService(): Boolean {
        return canPostNotifications() && areAppNotificationsEnabled()
    }

    private suspend fun refreshNotificationStartupState() {
        val nextState = determineNotificationStartupGateState(
            sdkInt = Build.VERSION.SDK_INT,
            runtimePermissionGranted = canPostNotifications(),
            appNotificationsEnabled = areAppNotificationsEnabled(),
            educationSeen = emulatorSettingsRepository.notificationPermissionEducationSeen(),
        )
        notificationGateState = nextState
        if (nextState == NotificationStartupGateState.Ready) {
            ensureServiceStartedAndBound()
        }
    }

    private fun ensureServiceStartedAndBound() {
        if (!serviceStartRequested) {
            ContextCompat.startForegroundService(this, Intent(this, EmulatorSessionService::class.java))
            serviceStartRequested = true
        }
        if (!isBound) {
            bindService(
                Intent(this, EmulatorSessionService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
        }
    }

    private fun openAppNotificationSettings() {
        val notificationSettingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra("android.provider.extra.APP_PACKAGE", packageName)
        }
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        runCatching {
            startActivity(notificationSettingsIntent)
        }.getOrElse {
            startActivity(appDetailsIntent)
        }
    }

    private fun observePickerRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                localMediaViewModel.pickerRequests.collect { role ->
                    when (role) {
                        MediaRole.DISK -> diskPickerLauncher.launch(arrayOf("*/*"))
                        MediaRole.CARTRIDGE -> cartridgePickerLauncher.launch(arrayOf("*/*"))
                        MediaRole.EXECUTABLE -> executablePickerLauncher.launch(arrayOf("*/*"))
                        MediaRole.ROM -> romPickerLauncher.launch(arrayOf("*/*"))
                    }
                }
            }
        }
    }

    private fun openFujiNetWebUi() {
        runCatching {
            startActivity(Intent(this, FujiNetWebViewActivity::class.java))
        }.getOrElse {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8000/")))
        }
    }

    private fun openFujiNetLogFile(path: String) {
        val file = File(path)
        if (!file.isFile) {
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            startActivity(Intent.createChooser(viewIntent, "Open FujiNet log"))
        }.getOrElse {
            startActivity(Intent.createChooser(shareIntent, "Share FujiNet log"))
        }
    }

    private fun swapFujiNetDisks() {
        lifecycleScope.launch {
            fujiNetSettingsBridge.swapDisks()
        }
    }

    private fun onDocumentPicked(role: MediaRole, uri: Uri?) {
        if (uri == null) {
            return
        }
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Some providers grant temporary access only; persistence is best-effort.
        }
        localMediaViewModel.onDocumentPicked(
            role = role,
            uriString = uri.toString(),
            displayName = resolveDisplayName(uri) ?: defaultDisplayName(role),
        )
        sessionRepository?.dispatch(SessionCommand.ApplyStoredMedia(role))
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex == -1 || !cursor.moveToFirst()) {
                    return@use null
                }
                cursor.getString(nameIndex)
            }
    }

    private fun defaultDisplayName(role: MediaRole): String = when (role) {
        MediaRole.DISK -> "disk.atr"
        MediaRole.CARTRIDGE -> "cartridge.car"
        MediaRole.EXECUTABLE -> "program.xex"
        MediaRole.ROM -> "custom-os.rom"
    }

    private fun clearMediaSelection(role: MediaRole) {
        localMediaViewModel.clearSelection(role)
        sessionRepository?.dispatch(SessionCommand.ClearStoredMedia(role))
    }

    private fun pickSystemRom(kind: SystemRomKind) {
        pendingSystemRomKind = kind
        systemRomPickerLauncher.launch(arrayOf("*/*"))
    }

    fun pickHDeviceDirectory(slot: Int) {
        pendingHDeviceSlot = slot
        hDevicePickerLauncher.launch(null)
    }

    fun clearHDeviceDirectory(slot: Int) {
        lifecycleScope.launch {
            emulatorSettingsRepository.updateHDevicePath(slot, null)
            sessionRepository?.dispatch(
                SessionCommand.ApplyRuntimeSettings(emulatorSettingsRepository.currentSettings()),
            )
        }
    }

    private fun clearSystemRom(kind: SystemRomKind) {
        lifecycleScope.launch {
            launchSettingsViewModel()?.onSystemRomImported(kind, null) ?: run {
                systemRomDocumentStore.clearSelection(kind)
                when (kind) {
                    SystemRomKind.XL_XE -> emulatorSettingsRepository.updateXlxeRomPath(null)
                    SystemRomKind.BASIC -> emulatorSettingsRepository.updateBasicRomPath(null)
                    SystemRomKind.ATARI_400_800 -> emulatorSettingsRepository.updateAtari400800RomPath(null)
                }
                sessionRepository?.dispatch(
                    SessionCommand.ApplyRuntimeSettings(emulatorSettingsRepository.currentSettings()),
                )
            }
        }
    }

    private fun onSystemRomPicked(kind: SystemRomKind?, uri: Uri?) {
        if (kind == null || uri == null) {
            return
        }
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Some providers grant temporary access only; persistence is best-effort.
        }
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val displayName = resolveDisplayName(uri) ?: defaultSystemRomDisplayName(kind)
                    val importedPath = importSystemRom(kind, uri, displayName)
                    SystemRomSelection(
                        uriString = uri.toString(),
                        importedPath = importedPath,
                        displayName = displayName,
                        lastUpdatedEpochMillis = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { selection ->
                Log.i(TAG, "Imported ${kind.name} ROM from $uri to ${selection.importedPath}")
                launchSettingsViewModel()?.onSystemRomImported(kind, selection) ?: run {
                    Log.w(TAG, "LaunchSettingsViewModel unavailable; falling back to repository update for ${kind.name} ROM")
                    systemRomDocumentStore.saveSelection(kind, selection)
                    when (kind) {
                        SystemRomKind.XL_XE -> emulatorSettingsRepository.updateXlxeRomPath(selection.importedPath)
                        SystemRomKind.BASIC -> emulatorSettingsRepository.updateBasicRomPath(selection.importedPath)
                        SystemRomKind.ATARI_400_800 -> emulatorSettingsRepository.updateAtari400800RomPath(selection.importedPath)
                    }
                    sessionRepository?.dispatch(
                        SessionCommand.ApplyRuntimeSettings(emulatorSettingsRepository.currentSettings()),
                    )
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to import ${kind.name} ROM from $uri", throwable)
            }
        }
    }

    private fun launchSettingsViewModel(): LaunchSettingsViewModel? {
        val repository = sessionRepository ?: return null
        return ViewModelProvider(
            this,
            LaunchSettingsViewModel.provideFactory(
                settingsRepository = emulatorSettingsRepository,
                sessionRepository = repository,
                runtimePaths = runtimePaths,
            ),
        )["launch", LaunchSettingsViewModel::class.java]
    }

    private fun importSystemRom(kind: SystemRomKind, uri: Uri, displayName: String): String {
        val runtimePaths = this.runtimePaths.also { it.ensureDirectories() }
        val destination = runtimePaths.romsDirectory
            .resolve("${kind.storagePrefix()}-${displayName.sanitizeForImport()}")
        destination.parentFile?.mkdirs()
        contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to open ROM URI: $uri")
        return destination.absolutePath
    }

    private fun defaultSystemRomDisplayName(kind: SystemRomKind): String = when (kind) {
        SystemRomKind.XL_XE -> "xlxe-os.rom"
        SystemRomKind.BASIC -> "basic.rom"
        SystemRomKind.ATARI_400_800 -> "atari800-os.rom"
    }

    private fun SystemRomKind.storagePrefix(): String = when (this) {
        SystemRomKind.XL_XE -> "xlxe"
        SystemRomKind.BASIC -> "basic"
        SystemRomKind.ATARI_400_800 -> "a800"
    }

    private fun String.sanitizeForImport(): String {
        return replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .ifBlank { "imported.bin" }
    }

    private fun onHDeviceTreePicked(slot: Int?, uri: Uri?) {
        if (slot == null || uri == null) {
            return
        }
        lifecycleScope.launch {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // Temporary access only; continue while permission is still valid.
            }

            val importedPath = importHDeviceDirectory(slot, uri)
            emulatorSettingsRepository.updateHDevicePath(slot, importedPath)
            sessionRepository?.dispatch(
                SessionCommand.ApplyRuntimeSettings(emulatorSettingsRepository.currentSettings()),
            )
        }
    }

    private fun importHDeviceDirectory(slot: Int, uri: Uri): String {
        val root = DocumentFile.fromTreeUri(this, uri)
            ?: error("Unable to open H$slot tree URI: $uri")
        val destination = runtimePaths.also { it.ensureDirectories() }.hDeviceDirectory(slot)
        destination.deleteRecursively()
        destination.mkdirs()
        copyDocumentTree(root, destination)
        return destination.absolutePath
    }

    private fun copyDocumentTree(source: DocumentFile, destination: File) {
        source.listFiles().forEach { child ->
            val safeName = (child.name ?: "item").sanitizeForImport()
            val target = destination.resolve(safeName)
            if (child.isDirectory) {
                target.mkdirs()
                copyDocumentTree(child, target)
            } else if (child.isFile) {
                target.parentFile?.mkdirs()
                contentResolver.openInputStream(child.uri)?.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
