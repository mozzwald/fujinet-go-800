#include <jni.h>

#include <cstdio>
#include <string>
#include <vector>

#include "session_runtime.h"

extern "C" bool FujiNetAndroid_StartRuntime(
        const char* runtimeRootPath,
        const char* configPath,
        const char* sdPath,
        const char* dataPath,
        int listenPort
);
extern "C" void FujiNetAndroid_StopRuntime();
extern "C" const char* FujiNetAndroid_LastErrorMessage();
extern "C" bool FujiNetAndroid_IsRuntimeRunning();
extern "C" int FujiNetAndroid_CopyRecentLog(char* output, int maxBytes);

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeStartSession(
        JNIEnv* env,
        jobject /*thiz*/,
        jint width,
        jint height,
        jint sampleRate,
        jboolean enableFujiNet,
        jstring runtimeRootPath,
        jint machineType,
        jint memorySizeKb,
        jboolean basicEnabled
) {
    const char* runtime_root_path = env->GetStringUTFChars(runtimeRootPath, nullptr);
    SessionRuntime::Get().StartSession(
            width,
            height,
            sampleRate,
            enableFujiNet == JNI_TRUE,
            runtime_root_path,
            machineType,
            memorySizeKb,
            basicEnabled == JNI_TRUE
    );
    env->ReleaseStringUTFChars(runtimeRootPath, runtime_root_path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativePauseSession(
        JNIEnv* env,
        jobject /*thiz*/,
        jboolean paused
) {
    (void)env;
    SessionRuntime::Get().PauseSession(paused);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeAttachSurface(
        JNIEnv* env,
        jobject /*thiz*/,
        jobject surface,
        jint width,
        jint height
) {
    SessionRuntime::Get().AttachSurface(env, surface, width, height);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeDetachSurface(
        JNIEnv* env,
        jobject /*thiz*/
) {
    SessionRuntime::Get().DetachSurface(env);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeGetSessionToken(
        JNIEnv* env,
        jobject /*thiz*/
) {
    (void)env;
    return SessionRuntime::Get().GetSessionToken();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeIsSessionAlive(
        JNIEnv* env,
        jobject /*thiz*/,
        jlong sessionToken
) {
    (void)env;
    return SessionRuntime::Get().IsSessionAlive(sessionToken);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeMountDisk(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring path,
        jint driveNumber
) {
    return SessionRuntime::Get().MountDisk(env, path, driveNumber);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeEjectDisk(
        JNIEnv* env,
        jobject /*thiz*/,
        jint driveNumber
) {
    (void)env;
    return SessionRuntime::Get().EjectDisk(driveNumber);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeInsertCartridge(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring path
) {
    return SessionRuntime::Get().InsertCartridge(env, path);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeRemoveCartridge(
        JNIEnv* env,
        jobject /*thiz*/
) {
    (void)env;
    return SessionRuntime::Get().RemoveCartridge();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeLoadExecutable(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring path
) {
    return SessionRuntime::Get().LoadExecutable(env, path);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetCustomRomPath(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring path
) {
    return SessionRuntime::Get().SetCustomRomPath(env, path);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeClearCustomRomPath(
        JNIEnv* env,
        jobject /*thiz*/
) {
    (void)env;
    return SessionRuntime::Get().ClearCustomRomPath();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetBasicRomPath(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring path
) {
    return SessionRuntime::Get().SetBasicRomPath(env, path);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeClearBasicRomPath(
        JNIEnv* env,
        jobject /*thiz*/
) {
    (void)env;
    return SessionRuntime::Get().ClearBasicRomPath();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetAtari400800RomPath(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring path
) {
    return SessionRuntime::Get().SetAtari400800RomPath(env, path);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeClearAtari400800RomPath(
        JNIEnv* env,
        jobject /*thiz*/
) {
    (void)env;
    return SessionRuntime::Get().ClearAtari400800RomPath();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetTurboEnabled(
        JNIEnv* env,
        jobject /*thiz*/,
        jboolean enabled
) {
    (void)env;
    SessionRuntime::Get().SetTurboEnabled(enabled == JNI_TRUE);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetVideoStandard(
        JNIEnv* env,
        jobject /*thiz*/,
        jboolean isPal
) {
    (void)env;
    SessionRuntime::Get().SetVideoStandard(isPal == JNI_TRUE);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetSioPatchMode(
        JNIEnv* env,
        jobject /*thiz*/,
        jint mode
) {
    (void)env;
    SessionRuntime::Get().SetSioPatchMode(mode);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetArtifactingMode(
        JNIEnv* env,
        jobject /*thiz*/,
        jint mode
) {
    (void)env;
    SessionRuntime::Get().SetArtifactingMode(mode);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetNtscFilterConfig(
        JNIEnv* env,
        jobject /*thiz*/,
        jint preset,
        jfloat sharpness,
        jfloat resolution,
        jfloat artifacts,
        jfloat fringing,
        jfloat bleed,
        jfloat burstPhase
) {
    (void)env;
    SessionRuntime::Get().SetNtscFilterConfig(
        preset,
        sharpness,
        resolution,
        artifacts,
        fringing,
        bleed,
        burstPhase
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetStereoPokeyEnabled(
        JNIEnv* env,
        jobject /*thiz*/,
        jboolean enabled
) {
    (void)env;
    SessionRuntime::Get().SetStereoPokeyEnabled(enabled == JNI_TRUE);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeSetHDevicePath(
        JNIEnv* env,
        jobject /*thiz*/,
        jint slot,
        jstring path
) {
    SessionRuntime::Get().SetHDevicePath(env, slot, path);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_loadRom(
        JNIEnv* env,
        jobject /*thiz*/,
        jbyteArray romData
) {
    return SessionRuntime::Get().LoadRom(env, romData);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_loadCartridge(
        JNIEnv* env,
        jobject /*thiz*/,
        jbyteArray data
) {
    return SessionRuntime::Get().LoadCartridge(env, data);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_loadFile(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring path
) {
    return SessionRuntime::Get().LoadFile(env, path);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_loadXex(
        JNIEnv* env,
        jobject /*thiz*/,
        jbyteArray data
) {
    return SessionRuntime::Get().LoadXex(env, data);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_loadAtr(
        JNIEnv* env,
        jobject /*thiz*/,
        jbyteArray data
) {
    return SessionRuntime::Get().LoadAtr(env, data);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeResetSystem(
        JNIEnv* env,
        jobject /*thiz*/,
        jboolean notifyFujiNet
) {
    (void)env;
    SessionRuntime::Get().ResetSystem(notifyFujiNet == JNI_TRUE);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_nativeWarmResetSystem(
        JNIEnv* env,
        jobject /*thiz*/
) {
    (void)env;
    SessionRuntime::Get().WarmResetSystem();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_setKeyState(
        JNIEnv* env,
        jobject /*thiz*/,
        jint akeyCode,
        jboolean pressed
) {
    (void)env;
    SessionRuntime::Get().SetKeyState(akeyCode, pressed);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_setConsoleKeys(
        JNIEnv* env,
        jobject /*thiz*/,
        jboolean start,
        jboolean select,
        jboolean option
) {
    (void)env;
    SessionRuntime::Get().SetConsoleKeys(start, select, option);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_setJoystickState(
        JNIEnv* env,
        jobject /*thiz*/,
        jint port,
        jfloat x,
        jfloat y,
        jboolean fire
) {
    (void)env;
    SessionRuntime::Get().SetJoystickState(port, x, y, fire);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_FujiNetNative_nativeStartRuntime(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring runtimeRootPath,
        jstring configPath,
        jstring sdPath,
        jstring dataPath,
        jint listenPort
) {
    const char* runtime_root = env->GetStringUTFChars(runtimeRootPath, nullptr);
    const char* config = env->GetStringUTFChars(configPath, nullptr);
    const char* sd = env->GetStringUTFChars(sdPath, nullptr);
    const char* data = env->GetStringUTFChars(dataPath, nullptr);

    if (runtime_root == nullptr || config == nullptr || sd == nullptr || data == nullptr) {
        if (runtime_root != nullptr) env->ReleaseStringUTFChars(runtimeRootPath, runtime_root);
        if (config != nullptr) env->ReleaseStringUTFChars(configPath, config);
        if (sd != nullptr) env->ReleaseStringUTFChars(sdPath, sd);
        if (data != nullptr) env->ReleaseStringUTFChars(dataPath, data);
        return JNI_FALSE;
    }

    const bool started = FujiNetAndroid_StartRuntime(
            runtime_root,
            config,
            sd,
            data,
            listenPort
    );

    env->ReleaseStringUTFChars(runtimeRootPath, runtime_root);
    env->ReleaseStringUTFChars(configPath, config);
    env->ReleaseStringUTFChars(sdPath, sd);
    env->ReleaseStringUTFChars(dataPath, data);
    return started ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_FujiNetNative_nativeStopRuntime(
        JNIEnv* env,
        jobject /*thiz*/
) {
    (void)env;
    FujiNetAndroid_StopRuntime();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_FujiNetNative_nativeLastErrorMessage(
        JNIEnv* env,
        jobject /*thiz*/
) {
    const char* error = FujiNetAndroid_LastErrorMessage();
    if (error == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(error);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_FujiNetNative_nativeIsRuntimeRunning(
        JNIEnv* env,
        jobject /*thiz*/
) {
    (void)env;
    return FujiNetAndroid_IsRuntimeRunning() ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_FujiNetNative_nativeRecentLog(
        JNIEnv* env,
        jobject /*thiz*/,
        jint maxBytes
) {
    const int resolvedMaxBytes = maxBytes > 0 ? maxBytes : 16384;
    std::vector<char> buffer(static_cast<size_t>(resolvedMaxBytes));
    const int copied = FujiNetAndroid_CopyRecentLog(buffer.data(), resolvedMaxBytes);
    if (copied <= 0) {
        return env->NewStringUTF("");
    }

    std::string sanitized;
    sanitized.reserve(static_cast<size_t>(copied));

    char escaped[5];
    for (int index = 0; index < copied; ++index) {
        const unsigned char byte = static_cast<unsigned char>(buffer[static_cast<size_t>(index)]);
        switch (byte) {
            case '\n':
            case '\r':
            case '\t':
                sanitized.push_back(static_cast<char>(byte));
                break;
            default:
                if (byte >= 0x20 && byte <= 0x7e) {
                    sanitized.push_back(static_cast<char>(byte));
                } else {
                    std::snprintf(escaped, sizeof(escaped), "\\x%02X", byte);
                    sanitized.append(escaped);
                }
                break;
        }
    }

    return env->NewStringUTF(sanitized.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_renderFrame(
        JNIEnv* env,
        jobject /*thiz*/,
        jobject buffer
) {
    SessionRuntime::Get().RenderFrame(env, buffer);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_fillAudioBuffer(
        JNIEnv* env,
        jobject /*thiz*/,
        jshortArray audioBuffer
) {
    SessionRuntime::Get().FillAudioBuffer(env, audioBuffer);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mantismoonlabs_fujinetgo800_core_EmulatorNative_setGamepadState(
        JNIEnv* env,
        jobject /*thiz*/,
        jint playerIndex,
        jint buttonsMask,
        jfloat axisX,
        jfloat axisY
) {
    (void)env;
    SessionRuntime::Get().SetGamepadState(playerIndex, buttonsMask, axisX, axisY);
}
