package com.mantismoonlabs.fujinetgo800.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.mantismoonlabs.fujinetgo800.BuildConfig
import com.mantismoonlabs.fujinetgo800.MainActivity
import com.mantismoonlabs.fujinetgo800.R
import com.mantismoonlabs.fujinetgo800.core.EmulatorNative
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetDebugFailureMode
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetServiceBridge
import com.mantismoonlabs.fujinetgo800.fujinet.FujiNetStartupException
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettings
import com.mantismoonlabs.fujinetgo800.settings.EmulatorSettingsRepository
import com.mantismoonlabs.fujinetgo800.settings.LaunchMode
import com.mantismoonlabs.fujinetgo800.settings.VideoStandard
import com.mantismoonlabs.fujinetgo800.storage.FujiNetRuntimeAssetInstaller
import com.mantismoonlabs.fujinetgo800.storage.MediaApplyUseCase
import com.mantismoonlabs.fujinetgo800.storage.MediaDocumentStore
import com.mantismoonlabs.fujinetgo800.storage.RuntimePaths
import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EmulatorSessionService : LifecycleService() {
    private val localBinder = LocalBinder()
    private val mutableState = MutableStateFlow<SessionState>(
        SessionState.ReadyToLaunch(launchMode = LaunchMode.FUJINET_ENABLED),
    )
    private val runtimePaths by lazy { RuntimePaths.fromContext(this) }
    private val mediaDocumentStore by lazy { MediaDocumentStore(runtimePaths) }
    private val mediaApplyUseCase by lazy {
        MediaApplyUseCase(
            importer = MediaApplyUseCase.ContentResolverImporter(contentResolver),
        )
    }
    private val settingsRepository by lazy { EmulatorSettingsRepository(this) }
    private val fujiNetRuntimeAssetInstaller by lazy {
        FujiNetRuntimeAssetInstaller(
            runtimePaths = runtimePaths,
            version = BuildConfig.FUJINET_RUNTIME_VERSION,
            bundledAssets = loadFujiNetBundledAssets(),
        )
    }
    private val fujiNetServiceBridge by lazy {
        FujiNetServiceBridge.forContext(
            context = this,
            runtimePaths = runtimePaths,
        )
    }
    private val frameProducer = EmulationFrameProducer()
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }
    private val audioFocusPolicy = ServiceAudioPolicy()
    private val controller by lazy {
        EmulatorSessionController(
            runtime = EmulatorSessionRuntime(
                ensureDirectories = { runtimePaths.ensureDirectories() },
                startSession = { config ->
                    EmulatorNative.startSession(
                        width = config.width,
                        height = config.height,
                        sampleRate = config.sampleRate,
                        enableFujiNet = config.settings.launchMode == LaunchMode.FUJINET_ENABLED,
                        runtimeRootPath = runtimePaths.fujiNetRuntimeDirectory.absolutePath,
                        machineType = config.settings.machineType,
                        memoryProfile = config.settings.memoryProfile,
                        basicEnabled = config.settings.basicEnabled,
                    )
                },
                getSessionToken = EmulatorNative::getSessionToken,
                isSessionAlive = EmulatorNative::isSessionAlive,
                pauseSession = EmulatorNative::pauseSession,
                resetSystem = EmulatorNative::resetSystem,
                warmResetSystem = EmulatorNative::warmResetSystem,
                attachSurface = EmulatorNative::attachSurface,
                detachSurface = EmulatorNative::detachSurface,
                mountDisk = EmulatorNative::mountDisk,
                ejectDisk = { EmulatorNative.ejectDisk(1) },
                insertCartridge = EmulatorNative::insertCartridge,
                removeCartridge = EmulatorNative::removeCartridge,
                loadExecutable = EmulatorNative::loadExecutable,
                applyCustomRom = EmulatorNative::setCustomRomPath,
                clearCustomRom = EmulatorNative::clearCustomRomPath,
                applyBasicRom = EmulatorNative::setBasicRomPath,
                clearBasicRom = EmulatorNative::clearBasicRomPath,
                applyAtari400800Rom = EmulatorNative::setAtari400800RomPath,
                clearAtari400800Rom = EmulatorNative::clearAtari400800RomPath,
                setTurboEnabled = EmulatorNative::setTurboEnabled,
                setVideoStandard = { videoStandard ->
                    EmulatorNative.setVideoStandard(videoStandard == VideoStandard.PAL)
                },
                setSioPatchMode = EmulatorNative::setSioPatchMode,
                setArtifactingMode = EmulatorNative::setArtifactingMode,
                setNtscFilterConfig = EmulatorNative::setNtscFilterConfig,
                setStereoPokeyEnabled = EmulatorNative::setStereoPokeyEnabled,
                setHDevicePath = EmulatorNative::setHDevicePath,
                setKeyState = EmulatorNative::setKeyState,
                setConsoleKeys = EmulatorNative::setConsoleKeys,
                setJoystickState = EmulatorNative::setJoystickState,
                startAudio = { sampleRate ->
                    Log.i(LogTag, "runtime.startAudio sampleRate=$sampleRate")
                    ensureAudioPlayer(sampleRate).resume()
                    applyServiceAudioPolicy(audioFocusPolicy.onPlaybackRequestedChanged(true))
                },
                resumeAudio = {
                    Log.i(LogTag, "runtime.resumeAudio")
                    audioPlayer?.resume()
                    applyServiceAudioPolicy(audioFocusPolicy.onPlaybackRequestedChanged(true))
                },
                pauseAudio = {
                    Log.i(LogTag, "runtime.pauseAudio")
                    audioPlayer?.pause()
                    applyServiceAudioPolicy(audioFocusPolicy.onPlaybackRequestedChanged(false))
                },
                setHostAudioMuted = { muted ->
                    Log.i(LogTag, "runtime.setHostAudioMuted muted=$muted")
                    audioPlayer?.setMuted(muted)
                    applyServiceAudioPolicy(audioFocusPolicy.onHostMutedChanged(muted))
                },
                setAudioVolume = { volume ->
                    audioPlayer?.setUserVolume(volume)
                },
                ensureFujiNetReady = { fujiNetServiceBridge.start() },
                isFujiNetHealthy = { fujiNetServiceBridge.isHealthy() },
            ),
        )
    }
    private var audioPlayer: EmulatorAudioPlayer? = null
    private var startJob: Job? = null
    private var shutdownInProgress = false
    private val controllerCommandMutex = Mutex()

    var attachedSurface: Surface? = null
        private set
    var surfaceWidth: Int = 0
        private set
    var surfaceHeight: Int = 0
        private set

    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    inner class LocalBinder : Binder() {
        fun service(): EmulatorSessionService = this@EmulatorSessionService
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        audioFocusPolicy.onForegroundInteraction()
        val persistedLaunchMode = runBlocking {
            settingsRepository.currentSettings().launchMode
        }
        val initialState = controller.onColdStart(persistedLaunchMode = persistedLaunchMode)
        syncSurfaceStateFromController()
        updateState(initialState)
        ServiceCompat.startForeground(
            this,
            NotificationId,
            buildNotification(initialState),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return localBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionTogglePause -> {
                val running = state.value as? SessionState.Running
                if (running != null) {
                    launchControllerCommand(SessionCommand.TogglePause)
                }
            }

            ActionStopService -> {
                lifecycleScope.launch {
                    stopEmulationService()
                }
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        startJob?.cancel()
        frameProducer.stop()
        runBlocking {
            fujiNetServiceBridge.stop()
        }
        audioPlayer?.stop()
        audioPlayer = null
        super.onDestroy()
    }

    fun dispatch(command: SessionCommand) {
        when (command) {
            is SessionCommand.StartSession -> {
                Log.i(
                    LogTag,
                    "dispatch StartSession launchMode=${command.config.settings.launchMode} startJobActive=${startJob?.isActive == true} state=${state.value::class.simpleName}",
                )
                if (startJob?.isActive == true) {
                    Log.i(LogTag, "dispatch StartSession ignored because startup job is already active")
                    return
                }
                if (command.config.settings.launchMode == LaunchMode.FUJINET_ENABLED) {
                    startJob = lifecycleScope.launch {
                        Log.i(LogTag, "dispatch StartSession entering FujiNet startup coroutine")
                        runCatching {
                            Log.i(LogTag, "FujiNet startup about to ensure runtime is installed")
                            ensureFujiNetRuntimeInstalled()
                            Log.i(LogTag, "FujiNet startup runtime install complete; invoking controller.startSession")
                            controller.startSession(command) { state ->
                                Log.i(LogTag, "controller.startSession emitted intermediate state=${state::class.simpleName}")
                                updateState(state)
                            }
                        }.onSuccess { state ->
                            Log.i(LogTag, "dispatch StartSession completed successfully with state=${state::class.simpleName}")
                            syncSurfaceStateFromController()
                            if (state is SessionState.Running) {
                                frameProducer.start()
                            } else if (state is SessionState.Failed) {
                                frameProducer.stop()
                                fujiNetServiceBridge.stop()
                            }
                            updateState(state)
                        }.onFailure { error ->
                            Log.e(LogTag, "dispatch StartSession failed during FujiNet startup", error)
                            frameProducer.stop()
                            updateState(
                                SessionState.Failed(
                                    launchMode = LaunchMode.FUJINET_ENABLED,
                                    reason = error.toFujiNetFailureReason(),
                                    canRecoverLocally = false,
                                    message = error.message ?: error.toFujiNetFailureReason().defaultMessage,
                                ),
                            )
                            fujiNetServiceBridge.stop()
                        }
                    }.also { job ->
                        job.invokeOnCompletion {
                            Log.i(LogTag, "dispatch StartSession startup coroutine completed")
                            startJob = null
                        }
                    }
                    return
                }

                lifecycleScope.launch {
                    fujiNetServiceBridge.stop()
                }
                updateState(controller.dispatch(command))
                syncSurfaceStateFromController()
                if (state.value is SessionState.Running) {
                    frameProducer.start()
                }
            }

            SessionCommand.RecoverLocalOnly -> {
                lifecycleScope.launch {
                    fujiNetServiceBridge.stop()
                }
                updateState(controller.dispatch(command))
                syncSurfaceStateFromController()
                if (state.value is SessionState.Running) {
                    frameProducer.start()
                }
            }

            is SessionCommand.SetAudioVolume,
            is SessionCommand.ApplyRuntimeSettings,
            is SessionCommand.MountDisk,
            SessionCommand.EjectDisk,
            is SessionCommand.InsertCartridge,
            SessionCommand.RemoveCartridge,
            is SessionCommand.LoadExecutable,
            is SessionCommand.ApplyCustomRom,
            SessionCommand.ClearCustomRom,
            is SessionCommand.ApplyBasicRom,
            SessionCommand.ClearBasicRom,
            is SessionCommand.ApplyAtari400800Rom,
            SessionCommand.ClearAtari400800Rom,
            is SessionCommand.SetKeyState,
            is SessionCommand.SetConsoleKeys,
            is SessionCommand.SetJoystickState -> {
                updateState(controller.dispatch(command))
                syncSurfaceStateFromController()
            }

            SessionCommand.TogglePause,
            is SessionCommand.AttachSurface,
            SessionCommand.DetachSurface -> {
                launchControllerCommand(command)
            }

            SessionCommand.HostStarted,
            SessionCommand.HostStopped -> {
                if (command == SessionCommand.HostStarted) {
                    applyServiceAudioPolicy(audioFocusPolicy.onForegroundInteraction())
                }
                updateState(controller.dispatch(command))
                syncSurfaceStateFromController()
            }

            SessionCommand.ResetSystem -> {
                if (state.value.usesFujiNet()) {
                    lifecycleScope.launch {
                        runCatching {
                            fujiNetServiceBridge.stop()
                            ensureFujiNetRuntimeInstalled()
                            fujiNetServiceBridge.start()
                            controllerCommandMutex.withLock {
                                updateState(controller.resetSystem(notifyFujiNet = false))
                                syncSurfaceStateFromController()
                            }
                        }.onFailure { error ->
                            frameProducer.stop()
                            updateState(
                                SessionState.Failed(
                                    launchMode = LaunchMode.FUJINET_ENABLED,
                                    reason = error.toFujiNetFailureReason(),
                                    canRecoverLocally = false,
                                    message = error.message ?: error.toFujiNetFailureReason().defaultMessage,
                                ),
                            )
                        }
                    }
                    return
                }
                launchControllerReset()
            }

            SessionCommand.WarmResetSystem -> {
                launchControllerWarmReset()
            }

            SessionCommand.ReturnToLaunch -> {
                val previousState = state.value
                startJob?.cancel()
                frameProducer.stop()
                updateState(controller.dispatch(command))
                syncSurfaceStateFromController()
                if (previousState.usesFujiNet()) {
                    lifecycleScope.launch {
                        fujiNetServiceBridge.stop()
                    }
                }
            }

            is SessionCommand.ApplyStoredMedia -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val selection = mediaDocumentStore.loadSelection(command.role) ?: return@launch
                    val prepared = mediaApplyUseCase.prepareApply(selection)
                    mediaDocumentStore.saveSelection(command.role, prepared.importedSelection)
                    dispatch(prepared.command)
                }
            }

            is SessionCommand.ClearStoredMedia -> {
                mediaDocumentStore.clearSelection(command.role)
                mediaApplyUseCase.clearCommand(command.role)?.let(::dispatch)
            }
        }
    }

    fun copyLatestFrame(target: ByteBuffer): Boolean = frameProducer.copyLatestFrame(target)

    fun refreshNotification() {
        notificationManager.notify(NotificationId, buildNotification(state.value))
    }

    fun startSession(config: SessionLaunchConfig) {
        dispatch(SessionCommand.StartSession(config))
    }

    fun pauseSession(): SessionState {
        val running = state.value as? SessionState.Running ?: return state.value
        if (!running.paused) {
            updateState(controller.dispatch(SessionCommand.TogglePause))
        }
        return state.value
    }

    fun resumeSession(): SessionState {
        val running = state.value as? SessionState.Running ?: return state.value
        if (running.paused) {
            updateState(controller.dispatch(SessionCommand.TogglePause))
        }
        return state.value
    }

    suspend fun stopEmulationService() {
        shutdownInProgress = true
        dispatch(SessionCommand.ReturnToLaunch)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NotificationId)
        stopSelf()
    }

    private fun syncSurfaceStateFromController() {
        attachedSurface = controller.attachedSurface
        surfaceWidth = controller.surfaceWidth
        surfaceHeight = controller.surfaceHeight
    }

    private fun updateState(newState: SessionState) {
        mutableState.value = newState
        if (!shutdownInProgress) {
            refreshNotification()
        }
    }

    private fun ensureAudioPlayer(sampleRate: Int): EmulatorAudioPlayer {
        val existing = audioPlayer
        if (existing != null) {
            return existing
        }
        return EmulatorAudioPlayer(sampleRate).also {
            it.start()
            audioPlayer = it
        }
    }

    private fun ensureFujiNetRuntimeInstalled() {
        Log.i(LogTag, "ensureFujiNetRuntimeInstalled entered")
        if (FujiNetServiceBridge.debugFailureModeForTesting() == FujiNetDebugFailureMode.ASSET_INIT_FAILURE) {
            throw FujiNetStartupException(
                failureMode = FujiNetDebugFailureMode.ASSET_INIT_FAILURE,
                message = "Debug FujiNet asset initialization failure",
            )
        }
        val assets = loadFujiNetBundledAssets()
        Log.i(LogTag, "ensureFujiNetRuntimeInstalled loaded assets count=${assets.size}")
        check(assets.isNotEmpty()) { "Bundled FujiNet runtime assets are missing" }
        fujiNetRuntimeAssetInstaller.ensureInstalled()
        Log.i(LogTag, "ensureFujiNetRuntimeInstalled ensureInstalled finished")
    }

    private fun loadFujiNetBundledAssets(): List<FujiNetRuntimeAssetInstaller.BundledAsset> {
        val assetManager = assets
        return runCatching {
            FujiNetBundledAssetLoader(
                listChildren = { assetPath: String ->
                    assetManager.list(assetPath)?.filter { childName -> childName.isNotBlank() }
                },
                openAsset = { assetPath: String ->
                    assetManager.open(assetPath).use { input -> input.readBytes() }
                },
            ).load()
        }.getOrElse { error: Throwable ->
            throw IOException("Unable to read bundled FujiNet assets", error)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelId,
                "Emulation",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Foreground emulation runtime"
            },
        )
    }

    private fun buildNotification(state: SessionState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pauseResumeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, EmulatorSessionService::class.java).setAction(ActionTogglePause),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, EmulatorSessionService::class.java).setAction(ActionStopService),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(this, NotificationChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(state.notificationLabel())
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        if (state is SessionState.Running) {
            builder.addAction(
                if (state.paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (state.paused) "Resume" else "Pause",
                pauseResumeIntent,
            )
        }

        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            stopIntent,
        )
        return builder.build()
    }

    private fun SessionState.notificationLabel(): String = when (this) {
        SessionState.Idle -> "Idle"
        is SessionState.ReadyToLaunch -> "Ready to launch ${launchMode.label()}"
        is SessionState.StartingFujiNet -> "Starting FujiNet"
        is SessionState.Starting -> "Starting emulator"
        is SessionState.Running -> {
            val mode = launchMode.label()
            if (paused) "$mode paused" else "$mode running"
        }
        is SessionState.Recovering -> "Recovering ${launchMode.label()}"
        is SessionState.Failed -> message
    }

    private fun LaunchMode.label(): String = when (this) {
        LaunchMode.FUJINET_ENABLED -> "FujiNet"
        LaunchMode.LOCAL_ONLY -> "Local only"
    }

    private fun SessionState.usesFujiNet(): Boolean = when (this) {
        is SessionState.ReadyToLaunch -> false
        is SessionState.Running -> launchMode == LaunchMode.FUJINET_ENABLED
        is SessionState.StartingFujiNet -> true
        is SessionState.Starting -> launchMode == LaunchMode.FUJINET_ENABLED
        is SessionState.Recovering -> launchMode == LaunchMode.FUJINET_ENABLED
        SessionState.Idle -> false
        is SessionState.Failed -> launchMode == LaunchMode.FUJINET_ENABLED
    }

    companion object {
        private const val LogTag = "EmulationService"
        private const val NotificationChannelId = "emulation_runtime"
        private const val NotificationId = 4001
        private val ActionTogglePause = "${BuildConfig.APPLICATION_ID}.action.TOGGLE_PAUSE"
        private val ActionStopService = "${BuildConfig.APPLICATION_ID}.action.STOP_EMULATION"
    }

    private fun applyServiceAudioPolicy(policy: ServiceAudioPolicy.Decision) {
        Log.i(
            LogTag,
            "applyServiceAudioPolicy playbackRequested=${policy.playbackRequested} focusMuted=${policy.focusMuted} ducked=${policy.ducked}",
        )
        audioPlayer?.setFocusMuted(policy.focusMuted)
        audioPlayer?.setDucked(policy.ducked)
    }

    private fun launchControllerCommand(command: SessionCommand) {
        lifecycleScope.launch(Dispatchers.Default) {
            controllerCommandMutex.withLock {
                updateState(controller.dispatch(command))
                syncSurfaceStateFromController()
            }
        }
    }

    private fun launchControllerReset() {
        lifecycleScope.launch(Dispatchers.Default) {
            controllerCommandMutex.withLock {
                updateState(controller.resetSystem())
                syncSurfaceStateFromController()
            }
        }
    }

    private fun launchControllerWarmReset() {
        lifecycleScope.launch(Dispatchers.Default) {
            controllerCommandMutex.withLock {
                updateState(controller.warmResetSystem())
                syncSurfaceStateFromController()
            }
        }
    }
}

internal class EmulatorSessionController(
    private val runtime: EmulatorSessionRuntime,
) {
    companion object {
        private const val LogTag = "EmulationService"
    }

    var state: SessionState = SessionState.ReadyToLaunch(LaunchMode.FUJINET_ENABLED)
        private set
    private var runtimeSettings = EmulatorSettings()
    private var launchModeForReturn = LaunchMode.FUJINET_ENABLED
    private var hostVisible = true
    private var autoPausedForHost = false

    var attachedSurface: Surface? = null
        private set
    var surfaceWidth: Int = 0
        private set
    var surfaceHeight: Int = 0
        private set

    fun onColdStart(
        persistedLaunchMode: LaunchMode,
    ): SessionState {
        launchModeForReturn = persistedLaunchMode
        runtimeSettings = runtimeSettings.copy(launchMode = persistedLaunchMode)
        hostVisible = true
        autoPausedForHost = false
        state = SessionState.ReadyToLaunch(launchMode = persistedLaunchMode)
        return state
    }

    suspend fun startSession(
        command: SessionCommand.StartSession,
        onStateChanged: (SessionState) -> Unit = {},
    ): SessionState {
        if (state is SessionState.Running || state is SessionState.Starting || state is SessionState.StartingFujiNet) {
            return state
        }

        runtimeSettings = command.config.settings
        logInfo(
            "Starting session launchMode=${command.config.settings.launchMode} machine=${command.config.settings.machineType} ram=${command.config.settings.memoryProfile} basic=${command.config.settings.basicEnabled}",
        )
        runtime.ensureDirectories()

        if (command.config.settings.launchMode == LaunchMode.FUJINET_ENABLED) {
            state = SessionState.StartingFujiNet(launchMode = command.config.settings.launchMode)
            logInfo("Controller entering StartingFujiNet and calling runtime.ensureFujiNetReady()")
            onStateChanged(state)
            runCatching {
                runtime.ensureFujiNetReady()
            }.onFailure { error ->
                Log.e(LogTag, "Controller ensureFujiNetReady failed", error)
                state = SessionState.Failed(
                    launchMode = LaunchMode.FUJINET_ENABLED,
                    reason = error.toFujiNetFailureReason(),
                    canRecoverLocally = false,
                    message = error.message ?: error.toFujiNetFailureReason().defaultMessage,
                )
                onStateChanged(state)
                return state
            }
            logInfo("Controller ensureFujiNetReady completed successfully")
        }

        state = SessionState.Starting(launchMode = command.config.settings.launchMode)
        logInfo("Controller entering Starting and about to configure runtime/start native session")
        onStateChanged(state)
        configureRuntimeBeforeStart()
        runtime.startSession(command.config)
        logInfo("Controller native runtime.startSession returned; publishing running state")
        state = publishRunningState(command.config, notifyFujiNet = true)
        onStateChanged(state)
        return state
    }

    fun dispatch(command: SessionCommand): SessionState {
        when (command) {
            is SessionCommand.StartSession -> {
                if (command.config.settings.launchMode == LaunchMode.FUJINET_ENABLED) {
                    return state
                }
                if (state is SessionState.Running) {
                    return state
                }
                runtimeSettings = command.config.settings
                runtime.ensureDirectories()
                state = SessionState.Starting(launchMode = command.config.settings.launchMode)
                configureRuntimeBeforeStart()
                runtime.startSession(command.config)
                state = publishRunningState(command.config, notifyFujiNet = true)
            }

            SessionCommand.ReturnToLaunch -> {
                val readyState = SessionState.ReadyToLaunch(launchMode = launchModeForReturn)
                if (state !is SessionState.Running && state !is SessionState.Starting && state !is SessionState.StartingFujiNet) {
                    state = readyState
                    return state
                }
                runtime.pauseSession(true)
                if (attachedSurface != null) {
                    runtime.detachSurface()
                }
                attachedSurface = null
                surfaceWidth = 0
                surfaceHeight = 0
                autoPausedForHost = false
                state = readyState
                syncAudioState()
            }

            SessionCommand.RecoverLocalOnly -> {
                val failedState = state as? SessionState.Failed ?: return state
                if (!failedState.canRecoverLocally) {
                    return state
                }
                val recoveryConfig = SessionLaunchConfig(
                    settings = runtimeSettings.copy(launchMode = LaunchMode.LOCAL_ONLY),
                )
                runtimeSettings = recoveryConfig.settings
                state = SessionState.Starting(launchMode = LaunchMode.LOCAL_ONLY)
                configureRuntimeBeforeStart()
                runtime.startSession(recoveryConfig)
                state = publishRunningState(recoveryConfig, notifyFujiNet = true)
                launchModeForReturn = failedState.launchMode
            }

            is SessionCommand.ApplyRuntimeSettings -> {
                val previousSettings = runtimeSettings
                runtimeSettings = command.settings
                if (state is SessionState.Running) {
                    if (previousSettings.requiresRomReload(command.settings)) {
                        applyConfiguredRoms()
                    }
                    applyRuntimeConfiguration()
                    syncAudioState()
                }
            }

            is SessionCommand.SetAudioVolume -> {
                val normalizedVolume = command.volumePercent.coerceIn(0, 100)
                runtimeSettings = runtimeSettings.copy(emulatorVolumePercent = normalizedVolume)
                runtime.setAudioVolume(normalizedVolume / 100f)
            }

            SessionCommand.TogglePause -> {
                val running = state as? SessionState.Running ?: return state
                val paused = !running.paused
                runtime.pauseSession(paused)
                state = running.copy(paused = paused)
                syncAudioState()
            }

            SessionCommand.ResetSystem -> {
                resetSystem()
            }

            SessionCommand.WarmResetSystem -> {
                warmResetSystem()
            }

            is SessionCommand.MountDisk -> {
                runtime.mountDisk(command.importedPath, command.driveNumber)
            }

            SessionCommand.EjectDisk -> {
                runtime.ejectDisk()
            }

            is SessionCommand.InsertCartridge -> {
                runtime.insertCartridge(command.importedPath)
            }

            SessionCommand.RemoveCartridge -> {
                runtime.removeCartridge()
            }

            is SessionCommand.LoadExecutable -> {
                runtime.loadExecutable(command.importedPath)
            }

            is SessionCommand.ApplyCustomRom -> {
                runtime.applyCustomRom(command.importedPath)
            }

            SessionCommand.ClearCustomRom -> {
                runtime.clearCustomRom()
            }

            is SessionCommand.ApplyBasicRom -> {
                runtime.applyBasicRom(command.importedPath)
            }

            SessionCommand.ClearBasicRom -> {
                runtime.clearBasicRom()
            }

            is SessionCommand.ApplyAtari400800Rom -> {
                runtime.applyAtari400800Rom(command.importedPath)
            }

            SessionCommand.ClearAtari400800Rom -> {
                runtime.clearAtari400800Rom()
            }

            is SessionCommand.SetKeyState -> {
                if (state is SessionState.Running) {
                    runtime.setKeyState(command.aKeyCode, command.pressed)
                }
            }

            is SessionCommand.SetConsoleKeys -> {
                if (state is SessionState.Running) {
                    runtime.setConsoleKeys(command.start, command.select, command.option)
                }
            }

            is SessionCommand.SetJoystickState -> {
                if (state is SessionState.Running) {
                    runtime.setJoystickState(command.port, command.x, command.y, command.fire)
                }
            }

            SessionCommand.HostStarted -> {
                hostVisible = true
                val running = state as? SessionState.Running
                if (autoPausedForHost && running != null && running.paused) {
                    runtime.pauseSession(false)
                    autoPausedForHost = false
                    state = running.copy(paused = false)
                }
                syncAudioState()
                return state
            }

            SessionCommand.HostStopped -> {
                hostVisible = false
                val running = state as? SessionState.Running
                if (runtimeSettings.pauseOnAppSwitch && running != null && !running.paused) {
                    runtime.pauseSession(true)
                    autoPausedForHost = true
                    state = running.copy(paused = true)
                }
                syncAudioState()
                return state
            }

            is SessionCommand.AttachSurface -> {
                attachedSurface = command.surface
                surfaceWidth = command.width
                surfaceHeight = command.height
                val running = state as? SessionState.Running
                if (running != null) {
                    runtime.attachSurface(command.surface, command.width, command.height)
                    state = running.copy(surfaceAttached = true)
                }
            }

            SessionCommand.DetachSurface -> {
                attachedSurface = null
                surfaceWidth = 0
                surfaceHeight = 0
                val running = state as? SessionState.Running
                if (running != null) {
                    runtime.detachSurface()
                    state = running.copy(surfaceAttached = false)
                }
            }

            is SessionCommand.ApplyStoredMedia,
            is SessionCommand.ClearStoredMedia -> error("Async media commands are handled by EmulatorSessionService")
        }

        return state
    }

    suspend fun handleHostStarted(): SessionState {
        return state
    }

    fun resetSystem(notifyFujiNet: Boolean = true): SessionState {
        runtime.resetSystem(notifyFujiNet)
        return state
    }

    fun warmResetSystem(): SessionState {
        runtime.warmResetSystem()
        return state
    }

    private fun publishRunningState(
        config: SessionLaunchConfig,
        notifyFujiNet: Boolean,
    ): SessionState {
        applyConfiguredRoms()
        applyRuntimeConfiguration()
        runtime.resetSystem(notifyFujiNet)
        attachedSurface?.let { surface ->
            runtime.attachSurface(surface, surfaceWidth, surfaceHeight)
        }
        runtime.startAudio(config.sampleRate)
        autoPausedForHost = false
        val runningState = SessionState.Running(
            sessionToken = runtime.getSessionToken(),
            paused = false,
            surfaceAttached = attachedSurface != null,
            launchMode = config.settings.launchMode,
        )
        state = runningState
        launchModeForReturn = runningState.launchMode
        syncAudioState()
        return runningState
    }

    private fun syncAudioState() {
        val running = state as? SessionState.Running
        val shouldMuteForHost = running != null && !runtimeSettings.backgroundAudioEnabled && !hostVisible
        runtime.setAudioVolume(runtimeSettings.emulatorVolumePercent / 100f)
        runtime.setHostAudioMuted(shouldMuteForHost)
        if (running == null || running.paused) {
            runtime.pauseAudio()
            return
        }
        runtime.resumeAudio()
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(LogTag, message) }
    }

    private fun configureRuntimeBeforeStart() {
        runtime.setVideoStandard(runtimeSettings.videoStandard)
        runtime.setNtscFilterConfig(runtimeSettings.ntscFilter)
        runtime.setArtifactingMode(runtimeSettings.artifactingMode)
        runtime.setStereoPokeyEnabled(runtimeSettings.stereoPokeyEnabled)
        applyLocalOnlyRuntimeConfiguration()
    }

    private fun applyRuntimeConfiguration() {
        runtime.setVideoStandard(runtimeSettings.videoStandard)
        runtime.setTurboEnabled(runtimeSettings.launchMode == LaunchMode.LOCAL_ONLY && runtimeSettings.turboEnabled)
        runtime.setNtscFilterConfig(runtimeSettings.ntscFilter)
        runtime.setArtifactingMode(runtimeSettings.artifactingMode)
        runtime.setStereoPokeyEnabled(runtimeSettings.stereoPokeyEnabled)
        applyLocalOnlyRuntimeConfiguration()
    }

    private fun applyLocalOnlyRuntimeConfiguration() {
        val localOnly = runtimeSettings.launchMode == LaunchMode.LOCAL_ONLY
        runtime.setSioPatchMode(
            if (localOnly) runtimeSettings.sioPatchMode else com.mantismoonlabs.fujinetgo800.settings.SioPatchMode.ENHANCED,
        )
        runtime.setHDevicePath(1, if (localOnly) runtimeSettings.hDevice1Path else null)
        runtime.setHDevicePath(2, if (localOnly) runtimeSettings.hDevice2Path else null)
        runtime.setHDevicePath(3, if (localOnly) runtimeSettings.hDevice3Path else null)
        runtime.setHDevicePath(4, if (localOnly) runtimeSettings.hDevice4Path else null)
    }

    private fun applyConfiguredRoms() {
        runtimeSettings.xlxeRomPath?.let(runtime.applyCustomRom) ?: runtime.clearCustomRom()
        runtimeSettings.basicRomPath?.let(runtime.applyBasicRom) ?: runtime.clearBasicRom()
        runtimeSettings.atari400800RomPath?.let(runtime.applyAtari400800Rom) ?: runtime.clearAtari400800Rom()
    }

    private fun EmulatorSettings.requiresRomReload(other: EmulatorSettings): Boolean {
        return xlxeRomPath != other.xlxeRomPath ||
            basicRomPath != other.basicRomPath ||
            atari400800RomPath != other.atari400800RomPath
    }
}

internal data class EmulatorSessionRuntime(
    val ensureDirectories: () -> Unit,
    val startSession: (SessionLaunchConfig) -> Unit,
    val getSessionToken: () -> Long,
    val isSessionAlive: (Long) -> Boolean = { token -> token != 0L },
    val pauseSession: (Boolean) -> Unit,
    val resetSystem: (Boolean) -> Unit,
    val warmResetSystem: () -> Unit,
    val attachSurface: (Surface, Int, Int) -> Unit,
    val detachSurface: () -> Unit,
    val mountDisk: (String, Int) -> Unit,
    val ejectDisk: () -> Unit,
    val insertCartridge: (String) -> Unit,
    val removeCartridge: () -> Unit,
    val loadExecutable: (String) -> Unit,
    val applyCustomRom: (String) -> Unit,
    val clearCustomRom: () -> Unit,
    val applyBasicRom: (String) -> Unit,
    val clearBasicRom: () -> Unit,
    val applyAtari400800Rom: (String) -> Unit,
    val clearAtari400800Rom: () -> Unit,
    val setTurboEnabled: (Boolean) -> Unit,
    val setVideoStandard: (VideoStandard) -> Unit,
    val setSioPatchMode: (com.mantismoonlabs.fujinetgo800.settings.SioPatchMode) -> Unit = {},
    val setArtifactingMode: (com.mantismoonlabs.fujinetgo800.settings.ArtifactingMode) -> Unit = {},
    val setNtscFilterConfig: (com.mantismoonlabs.fujinetgo800.settings.NtscFilterSettings) -> Unit = {},
    val setStereoPokeyEnabled: (Boolean) -> Unit = {},
    val setHDevicePath: (Int, String?) -> Unit = { _, _ -> },
    val setKeyState: (Int, Boolean) -> Unit,
    val setConsoleKeys: (Boolean, Boolean, Boolean) -> Unit,
    val setJoystickState: (Int, Float, Float, Boolean) -> Unit,
    val startAudio: (Int) -> Unit,
    val resumeAudio: () -> Unit,
    val pauseAudio: () -> Unit,
    val setHostAudioMuted: (Boolean) -> Unit = {},
    val setAudioVolume: (Float) -> Unit = {},
    val ensureFujiNetReady: suspend () -> Unit = {},
    val isFujiNetHealthy: suspend () -> Boolean = { true },
)

private fun Throwable.toFujiNetFailureReason(): FujiNetFailureReason = when (this) {
    is FujiNetStartupException -> when (failureMode) {
        FujiNetDebugFailureMode.ASSET_INIT_FAILURE -> FujiNetFailureReason.AssetInitializationFailed
        FujiNetDebugFailureMode.SERVICE_START_FAILURE -> FujiNetFailureReason.ServiceStartFailed
        FujiNetDebugFailureMode.READINESS_TIMEOUT -> FujiNetFailureReason.ReadinessTimeout
    }

    else -> {
        val detail = message.orEmpty()
        when {
            detail.contains("timeout", ignoreCase = true) ||
                detail.contains("timed out", ignoreCase = true) ||
                detail.contains("readiness", ignoreCase = true) -> FujiNetFailureReason.ReadinessTimeout

            detail.contains("asset", ignoreCase = true) ||
                detail.contains("bundled", ignoreCase = true) ||
                detail.contains("staging", ignoreCase = true) -> FujiNetFailureReason.AssetInitializationFailed

            else -> FujiNetFailureReason.ServiceStartFailed
        }
    }
}

internal class ServiceAudioPolicy {
    data class Decision(
        val playbackRequested: Boolean,
        val focusMuted: Boolean,
        val ducked: Boolean,
    )

    private var playbackRequested = false

    fun onPlaybackRequestedChanged(requested: Boolean): Decision {
        playbackRequested = requested
        return decision()
    }

    fun onHostMutedChanged(@Suppress("UNUSED_PARAMETER") muted: Boolean): Decision {
        return decision()
    }

    fun onForegroundInteraction(): Decision = decision()

    private fun decision(): Decision {
        if (!playbackRequested) {
            return Decision(
                playbackRequested = false,
                focusMuted = false,
                ducked = false,
            )
        }
        return Decision(
            playbackRequested = true,
            focusMuted = false,
            ducked = false,
        )
    }
}
