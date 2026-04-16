#include "session_runtime.h"

#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <limits>
#include <vector>
extern "C" {
#include "atari800/akey.h"
#include "atari800/afile.h"
#include "atari800/artifact.h"
#include "atari800/atari.h"
#include "atari800/cartridge.h"
#include "atari800/colours.h"
#include "atari800/devices.h"
#include "atari800/esc.h"
#include "atari800/filter_ntsc.h"
#include "atari800/input.h"
#include "atari800/memory.h"
#include "atari800/netsio.h"
#include "atari800/pokeysnd.h"
#include "atari800/screen.h"
#include "atari800/sio.h"
#include "atari800/sound.h"
#include "atari800/sysrom.h"
}

#include "atari_log_sink.h"

#define LOG_TAG "AtariCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" void PLATFORM_SetJoystick(int port, uint8_t stick_code, uint8_t trig_pressed);
extern "C" void FujiNetAndroid_MixAudio(int16_t* output, int sampleCount, int outputSampleRate);
extern "C" void FujiNetAndroid_ClearAudio();

namespace {
int resolved_ram_size_for_config(int machine_type, int memory_size_kb);

const char* machine_label_for_request(int machine_type) {
    switch (machine_type) {
        case 0: return "400/800";
        case 1: return "1200XL";
        case 2: return "800XL";
        case 3: return "130XE";
        case 4: return "320XE Compy";
        case 5: return "320XE Rambo";
        case 6: return "576XE";
        case 7: return "1088XE";
        case 8: return "XEGS";
        case 9: return "5200";
        default: return "Unknown";
    }
}

const char* machine_label_for_core(int machine_type) {
    switch (machine_type) {
        case Atari800_MACHINE_800: return "800";
        case Atari800_MACHINE_XLXE: return "XL/XE";
        case Atari800_MACHINE_5200: return "5200";
        default: return "Unknown";
    }
}

uint8_t stick_from_axes(float x, float y) {
    const float dead = 0.3f;
    const bool left = x <= -dead;
    const bool right = x >= dead;
    const bool up = y <= -dead;
    const bool down = y >= dead;

    if (left && up) return INPUT_STICK_UL;
    if (right && up) return INPUT_STICK_UR;
    if (left && down) return INPUT_STICK_LL;
    if (right && down) return INPUT_STICK_LR;
    if (left) return INPUT_STICK_LEFT;
    if (right) return INPUT_STICK_RIGHT;
    if (up) return INPUT_STICK_FORWARD;
    if (down) return INPUT_STICK_BACK;
    return INPUT_STICK_CENTRE;
}

void ReloadMachineRomsLocked(int machine_type, int memory_size_kb, bool basic_enabled) {
    const bool is_1200xl = machine_type == 1;
    const bool is_xegs = machine_type == 8;
    const bool is_5200 = machine_type == 9;
    MEMORY_ram_size = resolved_ram_size_for_config(machine_type, memory_size_kb);
    Atari800_disable_basic = (basic_enabled && !is_5200) ? FALSE : TRUE;
    Atari800_builtin_game = is_xegs ? TRUE : FALSE;
    Atari800_keyboard_detached = FALSE;
    Atari800_keyboard_leds = is_1200xl ? TRUE : FALSE;
    Atari800_f_keys = is_1200xl ? TRUE : FALSE;
    if (machine_type != 0 && !is_xegs && !is_5200 && memory_size_kb == 64) {
        Atari800_f_keys = TRUE;
    }
    Atari800_InitialiseMachine();
    Atari800_Coldstart();
}

ARTIFACT_t artifact_mode_from_setting(int mode) {
    switch (mode) {
        case 1:
            return ARTIFACT_NTSC_OLD;
        case 2:
            return ARTIFACT_NTSC_NEW;
        case 3:
            return ARTIFACT_NTSC_FULL;
        case 4:
#ifndef NO_SIMPLE_PAL_BLENDING
            return ARTIFACT_PAL_SIMPLE;
#else
            return ARTIFACT_NONE;
#endif
        case 5:
#ifdef PAL_BLENDING
            return ARTIFACT_PAL_BLEND;
#else
            return ARTIFACT_NONE;
#endif
        default:
            return ARTIFACT_NONE;
    }
}

float clamp_ntsc_filter_value(float value) {
    return std::clamp(value, -1.0f, 1.0f);
}

void apply_ntsc_filter_preset_locked(int preset) {
    switch (preset) {
        case FILTER_NTSC_PRESET_COMPOSITE:
            FILTER_NTSC_SetPreset(FILTER_NTSC_PRESET_COMPOSITE);
            break;
        case FILTER_NTSC_PRESET_SVIDEO:
            FILTER_NTSC_SetPreset(FILTER_NTSC_PRESET_SVIDEO);
            break;
        case FILTER_NTSC_PRESET_RGB:
            FILTER_NTSC_SetPreset(FILTER_NTSC_PRESET_RGB);
            break;
        case FILTER_NTSC_PRESET_MONOCHROME:
            FILTER_NTSC_SetPreset(FILTER_NTSC_PRESET_MONOCHROME);
            break;
        default:
            break;
    }
}

int resolved_ram_size_for_config(int machine_type, int memory_size_kb) {
    switch (machine_type) {
        case 0:
            return memory_size_kb == 52 ? 52 : 48;
        case 1:
        case 2:
        case 8:
            return 64;
        case 3:
            return 128;
        case 4:
            return MEMORY_RAM_320_COMPY_SHOP;
        case 5:
            return MEMORY_RAM_320_RAMBO;
        case 6:
            return 576;
        case 7:
            return 1088;
        case 9:
            return 16;
        default:
            return 64;
    }
}
}  // namespace

SessionRuntime& SessionRuntime::Get() {
    static SessionRuntime runtime;
    return runtime;
}

void SessionRuntime::StartSession(
        int width,
        int height,
        int sample_rate,
        bool fuji_net_enabled,
        const char* runtime_root_path,
        int machine_type,
        int memory_size_kb,
        bool basic_enabled
) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    frame_width_ = width;
    frame_height_ = height;
    sample_rate_ = sample_rate;
    fuji_net_enabled_ = fuji_net_enabled;
    runtime_root_path_ = runtime_root_path != nullptr ? runtime_root_path : "";
    AtariLogSink_StartCapture(runtime_root_path_.c_str());
    machine_type_ = machine_type;
    memory_size_kb_ = memory_size_kb;
    basic_enabled_ = basic_enabled;
    {
        std::lock_guard<std::mutex> command_lock(command_mutex_);
        pending_reset_requested_ = false;
        pending_reset_notify_fuji_net_ = true;
    }
    ResetQueuedAudioLocked();
    if (core_ready_) {
        ReconfigureCoreLocked();
    } else {
        InitializeCore();
    }
    UpdateNetSioConfiguration();
    initialized_ = true;
    paused_ = false;
    session_token_ += 1;
    LOGI("Session started token=%llu size=%dx%d sampleRate=%d fujiNet=%d requestedMachine=%s(%d) requestedRam=%d basic=%d",
         static_cast<unsigned long long>(session_token_),
         frame_width_,
         frame_height_,
         sample_rate_,
         fuji_net_enabled_ ? 1 : 0,
         machine_label_for_request(machine_type_),
         machine_type_,
         memory_size_kb_,
         basic_enabled_ ? 1 : 0);
}

void SessionRuntime::PauseSession(bool paused) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    paused_ = paused;
    if (paused) {
        ResetQueuedAudioLocked();
    }
    if (audio_ready_) {
        if (paused) {
            Sound_Pause();
        } else {
            Sound_Continue();
        }
    }
    LOGI("Session paused=%d token=%llu", paused_ ? 1 : 0, static_cast<unsigned long long>(session_token_));
}

void SessionRuntime::AttachSurface(JNIEnv* env, jobject surface, int width, int height) {
    std::lock_guard<std::mutex> lock(surface_mutex_);
    ClearSurfaceRef(env);
    if (surface != nullptr) {
        attached_surface_ = env->NewGlobalRef(surface);
        surface_attached_ = true;
        surface_width_ = width;
        surface_height_ = height;
    } else {
        surface_attached_ = false;
        surface_width_ = 0;
        surface_height_ = 0;
    }
}

void SessionRuntime::DetachSurface(JNIEnv* env) {
    std::lock_guard<std::mutex> lock(surface_mutex_);
    ClearSurfaceRef(env);
    surface_attached_ = false;
    surface_width_ = 0;
    surface_height_ = 0;
}

jlong SessionRuntime::GetSessionToken() const {
    return static_cast<jlong>(session_token_);
}

jboolean SessionRuntime::IsSessionAlive(jlong session_token) const {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const auto requested_token = static_cast<uint64_t>(session_token);
    const bool alive = initialized_ && core_ready_ && requested_token != 0 && session_token_ == requested_token;
    return alive ? JNI_TRUE : JNI_FALSE;
}

jboolean SessionRuntime::MountDisk(JNIEnv* env, jstring jpath, jint drive_number) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    InitializeCore();
    const bool mounted = SIO_Mount(drive_number, path, FALSE) != 0;
    if (mounted && core_ready_) {
        Atari800_Coldstart();
    }
    env->ReleaseStringUTFChars(jpath, path);
    return mounted ? JNI_TRUE : JNI_FALSE;
}

jboolean SessionRuntime::EjectDisk(jint drive_number) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    InitializeCore();
    SIO_Dismount(drive_number);
    if (core_ready_) {
        Atari800_Coldstart();
    }
    return JNI_TRUE;
}

jboolean SessionRuntime::InsertCartridge(JNIEnv* env, jstring jpath) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    InitializeCore();
    const int result = CARTRIDGE_InsertAutoReboot(path);
    env->ReleaseStringUTFChars(jpath, path);
    return (result >= 0 || result == CARTRIDGE_BAD_CHECKSUM) ? JNI_TRUE : JNI_FALSE;
}

jboolean SessionRuntime::RemoveCartridge() {
    std::lock_guard<std::mutex> lock(core_mutex_);
    InitializeCore();
    CARTRIDGE_RemoveAutoReboot();
    return JNI_TRUE;
}

jboolean SessionRuntime::LoadExecutable(JNIEnv* env, jstring jpath) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    InitializeCore();
    const int type = AFILE_OpenFile(path, TRUE, 1, FALSE);
    env->ReleaseStringUTFChars(jpath, path);
    return type != AFILE_ERROR ? JNI_TRUE : JNI_FALSE;
}

jboolean SessionRuntime::SetCustomRomPath(JNIEnv* env, jstring jpath) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    const int result = SYSROM_SetPath(path, 1, SYSROM_XL_CUSTOM);
    if (result == SYSROM_OK) {
        SYSROM_os_versions[Atari800_MACHINE_XLXE] = SYSROM_XL_CUSTOM;
        if (core_ready_) {
            ReloadMachineRomsLocked(machine_type_, memory_size_kb_, basic_enabled_);
        }
    }
    env->ReleaseStringUTFChars(jpath, path);
    return result == SYSROM_OK ? JNI_TRUE : JNI_FALSE;
}

jboolean SessionRuntime::ClearCustomRomPath() {
    std::lock_guard<std::mutex> lock(core_mutex_);
    SYSROM_os_versions[Atari800_MACHINE_XLXE] = SYSROM_ALTIRRA_XL;
    if (core_ready_) {
        ReloadMachineRomsLocked(machine_type_, memory_size_kb_, basic_enabled_);
    }
    return JNI_TRUE;
}

jboolean SessionRuntime::SetBasicRomPath(JNIEnv* env, jstring jpath) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    const int result = SYSROM_SetPath(path, 1, SYSROM_BASIC_CUSTOM);
    if (result == SYSROM_OK) {
        SYSROM_basic_version = SYSROM_BASIC_CUSTOM;
        if (core_ready_) {
            ReloadMachineRomsLocked(machine_type_, memory_size_kb_, basic_enabled_);
        }
    }
    env->ReleaseStringUTFChars(jpath, path);
    return result == SYSROM_OK ? JNI_TRUE : JNI_FALSE;
}

jboolean SessionRuntime::ClearBasicRomPath() {
    std::lock_guard<std::mutex> lock(core_mutex_);
    SYSROM_basic_version = SYSROM_ALTIRRA_BASIC;
    if (core_ready_) {
        ReloadMachineRomsLocked(machine_type_, memory_size_kb_, basic_enabled_);
    }
    return JNI_TRUE;
}

jboolean SessionRuntime::SetAtari400800RomPath(JNIEnv* env, jstring jpath) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    const int result = SYSROM_SetPath(path, 1, SYSROM_800_CUSTOM);
    if (result == SYSROM_OK) {
        SYSROM_os_versions[Atari800_MACHINE_800] = SYSROM_800_CUSTOM;
        if (core_ready_) {
            ReloadMachineRomsLocked(machine_type_, memory_size_kb_, basic_enabled_);
        }
    }
    env->ReleaseStringUTFChars(jpath, path);
    return result == SYSROM_OK ? JNI_TRUE : JNI_FALSE;
}

jboolean SessionRuntime::ClearAtari400800RomPath() {
    std::lock_guard<std::mutex> lock(core_mutex_);
    SYSROM_os_versions[Atari800_MACHINE_800] = SYSROM_ALTIRRA_800;
    if (core_ready_) {
        ReloadMachineRomsLocked(machine_type_, memory_size_kb_, basic_enabled_);
    }
    return JNI_TRUE;
}

void SessionRuntime::SetTurboEnabled(bool enabled) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    Atari800_turbo = enabled ? TRUE : FALSE;
}

void SessionRuntime::SetVideoStandard(bool pal) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    use_pal_ = pal;
    if (core_ready_) {
        UpdateNtscFilterLocked();
        Atari800_SetTVMode(use_pal_ ? Atari800_TV_PAL : Atari800_TV_NTSC);
        Atari800_Coldstart();
    }
}

void SessionRuntime::SetSioPatchMode(int mode) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    sio_patch_mode_ = mode;
    if (core_ready_) {
        ApplyPatchSettingsLocked();
    }
}

void SessionRuntime::SetArtifactingMode(int mode) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    artifacting_mode_ = mode;
    if (core_ready_) {
        ApplyArtifactingLocked();
    }
}

void SessionRuntime::SetNtscFilterConfig(
        int preset,
        float sharpness,
        float resolution,
        float artifacts,
        float fringing,
        float bleed,
        float burst_phase
) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    ntsc_filter_preset_ = preset;
    ntsc_filter_sharpness_ = clamp_ntsc_filter_value(sharpness);
    ntsc_filter_resolution_ = clamp_ntsc_filter_value(resolution);
    ntsc_filter_artifacts_ = clamp_ntsc_filter_value(artifacts);
    ntsc_filter_fringing_ = clamp_ntsc_filter_value(fringing);
    ntsc_filter_bleed_ = clamp_ntsc_filter_value(bleed);
    ntsc_filter_burst_phase_ = clamp_ntsc_filter_value(burst_phase);
    if (core_ready_) {
        UpdateNtscFilterLocked();
    }
}

void SessionRuntime::SetStereoPokeyEnabled(bool enabled) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    stereo_pokey_enabled_ = enabled;
}

void SessionRuntime::SetHDevicePath(JNIEnv* env, jint slot, jstring path) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    if (slot < 1 || slot > 4) {
        return;
    }
    const int index = static_cast<int>(slot) - 1;
    if (path == nullptr) {
        h_device_paths_[index].clear();
    } else {
        const char* chars = env->GetStringUTFChars(path, nullptr);
        if (chars == nullptr) {
            return;
        }
        h_device_paths_[index] = chars;
        env->ReleaseStringUTFChars(path, chars);
    }
    if (core_ready_) {
        ApplyPatchSettingsLocked();
    }
}

jboolean SessionRuntime::LoadRom(JNIEnv* env, jbyteArray rom_data) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const jsize length = env->GetArrayLength(rom_data);
    LOGI("loadRom called with %d bytes (stub)", static_cast<int>(length));
    return JNI_TRUE;
}

jboolean SessionRuntime::LoadCartridge(JNIEnv* env, jbyteArray data) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    (void)env;
    (void)data;
    return JNI_FALSE;
}

jboolean SessionRuntime::LoadFile(JNIEnv* env, jstring jpath) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }
    InitializeCore();
    const int type = AFILE_OpenFile(path, TRUE, 1, FALSE);
    if (type != AFILE_ERROR && core_ready_) {
        Atari800_Coldstart();
    }
    env->ReleaseStringUTFChars(jpath, path);
    return type != AFILE_ERROR;
}

jboolean SessionRuntime::LoadXex(JNIEnv* env, jbyteArray data) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    (void)env;
    (void)data;
    return JNI_FALSE;
}

jboolean SessionRuntime::LoadAtr(JNIEnv* env, jbyteArray data) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    (void)env;
    (void)data;
    return JNI_FALSE;
}

void SessionRuntime::ResetSystem(bool notify_fuji_net) {
    std::lock_guard<std::mutex> lock(command_mutex_);
    pending_reset_requested_ = true;
    pending_reset_notify_fuji_net_ = notify_fuji_net;
}

void SessionRuntime::WarmResetSystem() {
    std::lock_guard<std::mutex> lock(command_mutex_);
    pending_warm_reset_requested_ = true;
}

void SessionRuntime::SetKeyState(jint akey_code, jboolean pressed) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    if (pressed) {
        INPUT_key_code = akey_code;
    } else if (INPUT_key_code == akey_code) {
        INPUT_key_code = AKEY_NONE;
    }
}

void SessionRuntime::SetConsoleKeys(jboolean start, jboolean select, jboolean option) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    int consol = INPUT_CONSOL_NONE;
    if (start) consol &= ~INPUT_CONSOL_START;
    if (select) consol &= ~INPUT_CONSOL_SELECT;
    if (option) consol &= ~INPUT_CONSOL_OPTION;
    INPUT_key_consol = consol;
}

void SessionRuntime::SetJoystickState(jint port, jfloat x, jfloat y, jboolean fire) {
    if (port < 0 || port >= static_cast<jint>(pending_joystick_x_.size())) {
        return;
    }
    std::lock_guard<std::mutex> lock(input_mutex_);
    pending_joystick_x_[port] = x;
    pending_joystick_y_[port] = y;
    pending_joystick_fire_[port] = fire == JNI_TRUE;
    pending_joystick_dirty_[port] = true;
}

void SessionRuntime::RenderFrame(JNIEnv* env, jobject buffer) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    ApplyPendingControlStateLocked();
    if (!initialized_ || paused_) {
        return;
    }

    if (!core_ready_) {
        InitializeCore();
        LOGI("renderFrame: coreReady after retry = %d", core_ready_ ? 1 : 0);
    }

    uint8_t* dst = static_cast<uint8_t*>(env->GetDirectBufferAddress(buffer));
    if (dst == nullptr) {
        LOGE("renderFrame: buffer is null");
        return;
    }

    const jlong capacity = env->GetDirectBufferCapacity(buffer);
    const int stride = frame_width_ * 4;
    const jlong required = static_cast<jlong>(frame_width_) * frame_height_ * 4;
    if (capacity < required) {
        LOGE("renderFrame: buffer too small (have %ld, need %ld)",
             static_cast<long>(capacity),
             static_cast<long>(required));
        return;
    }

    RunFrame(dst, stride);
}

void SessionRuntime::FillAudioBuffer(JNIEnv* env, jshortArray audio_buffer) {
    const jsize length = env->GetArrayLength(audio_buffer);
    jshort* out = env->GetShortArrayElements(audio_buffer, nullptr);
    if (out == nullptr) {
        return;
    }
    std::memset(out, 0, sizeof(jshort) * length);
    {
        std::lock_guard<std::mutex> lock(audio_mutex_);
        if (audio_buffered_samples_ > 0 && !audio_ring_buffer_.empty()) {
            const size_t samples_to_copy = std::min(static_cast<size_t>(length), audio_buffered_samples_);
            const size_t first_chunk = std::min(samples_to_copy, audio_ring_buffer_.size() - audio_read_index_);
            std::memcpy(out, audio_ring_buffer_.data() + audio_read_index_, first_chunk * sizeof(int16_t));
            if (samples_to_copy > first_chunk) {
                std::memcpy(out + first_chunk, audio_ring_buffer_.data(), (samples_to_copy - first_chunk) * sizeof(int16_t));
            }
            audio_read_index_ = (audio_read_index_ + samples_to_copy) % audio_ring_buffer_.size();
            audio_buffered_samples_ -= samples_to_copy;
        }
    }
    env->ReleaseShortArrayElements(audio_buffer, out, 0);
}

void SessionRuntime::SetGamepadState(jint player_index, jint buttons_mask, jfloat axis_x, jfloat axis_y) {
    std::lock_guard<std::mutex> lock(core_mutex_);
    (void)player_index;
    (void)buttons_mask;
    (void)axis_x;
    (void)axis_y;
}

void SessionRuntime::InitializeCore() {
    if (core_ready_) {
        return;
    }

    Sound_enabled = TRUE;
    Sound_desired.freq = static_cast<unsigned int>(sample_rate_);
    Sound_desired.sample_size = 2;
    Sound_desired.channels = 1;
    Sound_desired.buffer_ms = 25;
    POKEYSND_bienias_fix = 0;
    POKEYSND_enable_new_pokey = 0;
    POKEYSND_stereo_enabled = stereo_pokey_enabled_ ? 1 : 0;
    POKEYSND_num_pokeys = stereo_pokey_enabled_ ? 2 : 1;
    POKEYSND_snd_flags = 0;

    const bool is_400_800 = machine_type_ == 0;
    const bool is_1200xl = machine_type_ == 1;
    const bool is_800xl = machine_type_ == 2;
    const bool is_130xe = machine_type_ == 3;
    const bool is_320xe_compy = machine_type_ == 4;
    const bool is_320xe_rambo = machine_type_ == 5;
    const bool is_576xe = machine_type_ == 6;
    const bool is_1088xe = machine_type_ == 7;
    const bool is_xegs = machine_type_ == 8;
    const bool is_5200 = machine_type_ == 9;
    const int resolved_ram_size = resolved_ram_size_for_config(machine_type_, memory_size_kb_);
    const int atari_machine_type = is_400_800
        ? Atari800_MACHINE_800
        : (is_5200 ? Atari800_MACHINE_5200 : Atari800_MACHINE_XLXE);

    Atari800_SetMachineType(atari_machine_type);
    MEMORY_ram_size = resolved_ram_size;
    Atari800_disable_basic = (basic_enabled_ && !is_5200) ? FALSE : TRUE;
    Atari800_builtin_game = is_xegs ? TRUE : FALSE;
    Atari800_keyboard_detached = FALSE;
    Atari800_keyboard_leds = is_1200xl ? TRUE : FALSE;
    Atari800_f_keys = is_1200xl ? TRUE : FALSE;

    if (!is_400_800 && !is_xegs && !is_5200 && memory_size_kb_ == 64) {
        Atari800_f_keys = TRUE;
    }
    if (!is_400_800 && !is_5200 && memory_size_kb_ >= 64) {
        Atari800_keyboard_detached = FALSE;
    }

    SYSROM_SetDefaults();
    SYSROM_os_versions[Atari800_MACHINE_800] = SYSROM_ALTIRRA_800;
    SYSROM_os_versions[Atari800_MACHINE_XLXE] = SYSROM_ALTIRRA_XL;
    SYSROM_os_versions[Atari800_MACHINE_5200] = SYSROM_ALTIRRA_5200;
    SYSROM_basic_version = SYSROM_ALTIRRA_BASIC;

    Screen_show_atari_speed = FALSE;
    Screen_show_disk_led = FALSE;
    Screen_show_sector_counter = FALSE;
    Screen_show_multimedia_stats = FALSE;

    char arg0[] = "android";
    char arg1[] = "-xl";
    char arg1b[] = "-atari";
    char arg1c[] = "-xe";
    char arg1d[] = "-xegs";
    char arg1e[] = "-1200";
    char arg1f[] = "-320xe";
    char arg1g[] = "-rambo";
    char arg1h[] = "-576xe";
    char arg1i[] = "-1088xe";
    char arg1j[] = "-5200";
    char arg1k[] = "-c";
    char arg2[] = "-netsio";
    char arg3[] = "9997";
    char* machine_arg = arg1;
    char* machine_extra_arg = nullptr;
    if (is_400_800) {
        machine_arg = arg1b;
        if (resolved_ram_size == 52) {
            machine_extra_arg = arg1k;
        }
    } else if (is_1200xl) {
        machine_arg = arg1e;
    } else if (is_xegs) {
        machine_arg = arg1d;
    } else if (is_320xe_compy) {
        machine_arg = arg1f;
    } else if (is_320xe_rambo) {
        machine_arg = arg1g;
    } else if (is_576xe) {
        machine_arg = arg1h;
    } else if (is_1088xe) {
        machine_arg = arg1i;
    } else if (is_5200) {
        machine_arg = arg1j;
    } else if (is_130xe) {
        machine_arg = arg1c;
    }
    char* argv[6];
    int argc = 0;
    argv[argc++] = arg0;
    argv[argc++] = machine_arg;
    if (machine_extra_arg != nullptr) {
        argv[argc++] = machine_extra_arg;
    }
    if (fuji_net_enabled_) {
        argv[argc++] = arg2;
        argv[argc++] = arg3;
    }
    argv[argc] = nullptr;
    bool ok = Atari800_Initialise(&argc, argv);
    if (ok) {
        MEMORY_ram_size = resolved_ram_size;
    }
    if (ok && Sound_out.freq == 0) {
        Sound_out.freq = static_cast<unsigned int>(sample_rate_);
        Sound_out.sample_size = 2;
        Sound_out.channels = 1;
    }
    if (ok) {
        Atari800_SetTVMode(use_pal_ ? Atari800_TV_PAL : Atari800_TV_NTSC);
        ok = Atari800_InitialiseMachine() != 0;
    }
    core_ready_ = ok;
    if (!core_ready_) {
        LOGE("Atari800_Initialise failed, sticking with test pattern fallback");
    } else {
        // `-netsio 9997` during cold init already binds the emulator NetSIO socket.
        netsio_configured_ = fuji_net_enabled_;
        audio_ready_ = Sound_enabled != FALSE;
        ResetQueuedAudioLocked();
        ApplyPatchSettingsLocked();
        ApplyArtifactingLocked();
        FujiNetAndroid_ClearAudio();
        Atari800_Coldstart();
        LOGI("Atari config requestedMachine=%s(%d) requestedRam=%d effectiveMachine=%s(%d) effectiveRam=%d basicDisabled=%d builtinGame=%d keyboardLeds=%d fKeys=%d",
             machine_label_for_request(machine_type_),
             machine_type_,
             memory_size_kb_,
             machine_label_for_core(Atari800_machine_type),
             Atari800_machine_type,
             MEMORY_ram_size,
             Atari800_disable_basic ? 1 : 0,
             Atari800_builtin_game ? 1 : 0,
             Atari800_keyboard_leds ? 1 : 0,
             Atari800_f_keys ? 1 : 0);
    }

    Screen_visible_x1 = 0;
    Screen_visible_y1 = 0;
    Screen_visible_x2 = frame_width_;
    Screen_visible_y2 = frame_height_;
}

void SessionRuntime::ReconfigureCoreLocked() {
    const bool is_400_800 = machine_type_ == 0;
    const bool is_1200xl = machine_type_ == 1;
    const bool is_800xl = machine_type_ == 2;
    const bool is_130xe = machine_type_ == 3;
    const bool is_320xe_compy = machine_type_ == 4;
    const bool is_320xe_rambo = machine_type_ == 5;
    const bool is_576xe = machine_type_ == 6;
    const bool is_1088xe = machine_type_ == 7;
    const bool is_xegs = machine_type_ == 8;
    const bool is_5200 = machine_type_ == 9;
    const int resolved_ram_size = resolved_ram_size_for_config(machine_type_, memory_size_kb_);
    const int atari_machine_type = is_400_800
        ? Atari800_MACHINE_800
        : (is_5200 ? Atari800_MACHINE_5200 : Atari800_MACHINE_XLXE);

    Atari800_SetMachineType(atari_machine_type);
    MEMORY_ram_size = resolved_ram_size;
    Atari800_disable_basic = (basic_enabled_ && !is_5200) ? FALSE : TRUE;
    Atari800_builtin_game = is_xegs ? TRUE : FALSE;
    Atari800_keyboard_detached = FALSE;
    Atari800_keyboard_leds = is_1200xl ? TRUE : FALSE;
    Atari800_f_keys = is_1200xl ? TRUE : FALSE;
    if (!is_400_800 && !is_xegs && !is_5200 && memory_size_kb_ == 64) {
        Atari800_f_keys = TRUE;
    }
    if (!is_400_800 && !is_5200 && memory_size_kb_ >= 64) {
        Atari800_keyboard_detached = FALSE;
    }

    Atari800_InitialiseMachine();
    Atari800_SetTVMode(use_pal_ ? Atari800_TV_PAL : Atari800_TV_NTSC);
    ResetQueuedAudioLocked();
    ApplyPatchSettingsLocked();
    ApplyArtifactingLocked();
    FujiNetAndroid_ClearAudio();
    Atari800_Coldstart();
    LOGI("Atari reconfig requestedMachine=%s(%d) requestedRam=%d effectiveMachine=%s(%d) effectiveRam=%d basicDisabled=%d builtinGame=%d keyboardLeds=%d fKeys=%d",
         machine_label_for_request(machine_type_),
         machine_type_,
         memory_size_kb_,
         machine_label_for_core(Atari800_machine_type),
         Atari800_machine_type,
         MEMORY_ram_size,
         Atari800_disable_basic ? 1 : 0,
         Atari800_builtin_game ? 1 : 0,
         Atari800_keyboard_leds ? 1 : 0,
         Atari800_f_keys ? 1 : 0);
}

void SessionRuntime::UpdateNetSioConfiguration() {
    if (!fuji_net_enabled_) {
        netsio_enabled = 0;
        return;
    }

    if (!netsio_configured_ && netsio_init(static_cast<uint16_t>(kFujiNetNetSioPort)) < 0) {
        LOGE("netsio init failed for FujiNet launch on port %d", kFujiNetNetSioPort);
        return;
    }

    netsio_configured_ = true;
    netsio_enabled = 1;
    LOGI("Configured FujiNet NetSIO on UDP %d", kFujiNetNetSioPort);
}

void SessionRuntime::ApplyPatchSettingsLocked() {
    if (fuji_net_enabled_) {
        ESC_enable_sio_patch = FALSE;
        Devices_enable_h_patch = FALSE;
        Devices_enable_p_patch = FALSE;
        Devices_enable_r_patch = FALSE;
        if (core_ready_) {
            ESC_UpdatePatches();
        }
        return;
    }

    const bool has_h_device = h_device_paths_[0].empty() == false ||
        h_device_paths_[1].empty() == false ||
        h_device_paths_[2].empty() == false ||
        h_device_paths_[3].empty() == false;
    for (int index = 0; index < 4; ++index) {
        const std::string& path = h_device_paths_[index];
        std::strncpy(Devices_atari_h_dir[index], path.c_str(), FILENAME_MAX - 1);
        Devices_atari_h_dir[index][FILENAME_MAX - 1] = '\0';
    }

    switch (sio_patch_mode_) {
        case 1: // NO_SIO_PATCH
            ESC_enable_sio_patch = FALSE;
            Devices_enable_h_patch = has_h_device ? TRUE : FALSE;
            Devices_enable_p_patch = TRUE;
            Devices_enable_r_patch = FALSE;
            break;
        case 2: // NO_PATCH_ALL
            ESC_enable_sio_patch = FALSE;
            Devices_enable_h_patch = FALSE;
            Devices_enable_p_patch = FALSE;
            Devices_enable_r_patch = FALSE;
            break;
        default: // ENHANCED
            ESC_enable_sio_patch = TRUE;
            Devices_enable_h_patch = has_h_device ? TRUE : FALSE;
            Devices_enable_p_patch = TRUE;
            Devices_enable_r_patch = FALSE;
            break;
    }

    if (core_ready_) {
        ESC_UpdatePatches();
    }
}

void SessionRuntime::ApplyArtifactingLocked() {
    if (!core_ready_) {
        return;
    }
    UpdateNtscFilterLocked();
    ARTIFACT_Set(artifact_mode_from_setting(artifacting_mode_));
}

void SessionRuntime::UpdateNtscFilterLocked() {
    const bool needs_ntsc_filter = !use_pal_ && artifacting_mode_ == 3;
    if (!needs_ntsc_filter) {
        if (FILTER_NTSC_emu != nullptr) {
            FILTER_NTSC_Delete(FILTER_NTSC_emu);
            FILTER_NTSC_emu = nullptr;
        }
        ntsc_filter_argb_buffer_.clear();
        return;
    }

    if (FILTER_NTSC_emu == nullptr) {
        FILTER_NTSC_emu = FILTER_NTSC_New();
    }
    if (FILTER_NTSC_emu != nullptr) {
        apply_ntsc_filter_preset_locked(ntsc_filter_preset_);
        FILTER_NTSC_setup.sharpness = ntsc_filter_sharpness_;
        FILTER_NTSC_setup.resolution = ntsc_filter_resolution_;
        FILTER_NTSC_setup.artifacts = ntsc_filter_artifacts_;
        FILTER_NTSC_setup.fringing = ntsc_filter_fringing_;
        FILTER_NTSC_setup.bleed = ntsc_filter_bleed_;
        FILTER_NTSC_setup.burst_phase = ntsc_filter_burst_phase_;
        FILTER_NTSC_Update(FILTER_NTSC_emu);
    }
}

void SessionRuntime::ApplyPendingControlStateLocked() {
    bool apply_reset = false;
    bool apply_warm_reset = false;
    bool notify_fuji_net = true;
    {
        std::lock_guard<std::mutex> command_lock(command_mutex_);
        if (pending_reset_requested_) {
            apply_reset = true;
            notify_fuji_net = pending_reset_notify_fuji_net_;
            pending_reset_requested_ = false;
        }
        if (pending_warm_reset_requested_) {
            apply_warm_reset = true;
            pending_warm_reset_requested_ = false;
        }
    }

    if (apply_reset && core_ready_) {
        const int previous_netsio_enabled = netsio_enabled;
        if (!notify_fuji_net && fuji_net_enabled_) {
            netsio_enabled = 0;
        }
        ResetQueuedAudioLocked();
        FujiNetAndroid_ClearAudio();
        Atari800_Coldstart();
        if (!notify_fuji_net && fuji_net_enabled_) {
            netsio_enabled = previous_netsio_enabled;
        }
        LOGI("Session reset notifyFujiNet=%d token=%llu", notify_fuji_net ? 1 : 0, static_cast<unsigned long long>(session_token_));
    }

    if (apply_warm_reset && core_ready_) {
        ResetQueuedAudioLocked();
        FujiNetAndroid_ClearAudio();
        Atari800_Warmstart();
        LOGI("Session warm reset token=%llu", static_cast<unsigned long long>(session_token_));
    }
}

void SessionRuntime::ApplyPendingJoystickStateLocked() {
    std::lock_guard<std::mutex> input_lock(input_mutex_);
    for (size_t port = 0; port < pending_joystick_dirty_.size(); ++port) {
        if (!pending_joystick_dirty_[port]) {
            continue;
        }
        pending_joystick_dirty_[port] = false;
        const uint8_t stick = stick_from_axes(pending_joystick_x_[port], pending_joystick_y_[port]);
        const uint8_t trig = pending_joystick_fire_[port] ? 1 : 0;
        PLATFORM_SetJoystick(static_cast<int>(port), stick, trig);
    }
}

void SessionRuntime::ResetQueuedAudioLocked() {
    std::lock_guard<std::mutex> audio_lock(audio_mutex_);
    const size_t desired_capacity = std::max<size_t>(static_cast<size_t>(sample_rate_ / 3), 4096);
    if (audio_ring_buffer_.size() != desired_capacity) {
        audio_ring_buffer_.assign(desired_capacity, 0);
    } else {
        std::fill(audio_ring_buffer_.begin(), audio_ring_buffer_.end(), 0);
    }
    audio_read_index_ = 0;
    audio_write_index_ = 0;
    audio_buffered_samples_ = 0;
    audio_frame_accumulator_ = 0.0;
}

void SessionRuntime::EnqueueAudioLocked(const int16_t* samples, size_t sample_count) {
    std::lock_guard<std::mutex> audio_lock(audio_mutex_);
    if (audio_ring_buffer_.empty()) {
        return;
    }
    if (sample_count >= audio_ring_buffer_.size()) {
        samples += (sample_count - audio_ring_buffer_.size());
        sample_count = audio_ring_buffer_.size();
    }
    const size_t available_space = audio_ring_buffer_.size() - audio_buffered_samples_;
    if (sample_count > available_space) {
        const size_t drop_count = sample_count - available_space;
        audio_read_index_ = (audio_read_index_ + drop_count) % audio_ring_buffer_.size();
        audio_buffered_samples_ -= drop_count;
    }
    const size_t first_chunk = std::min(sample_count, audio_ring_buffer_.size() - audio_write_index_);
    std::memcpy(audio_ring_buffer_.data() + audio_write_index_, samples, first_chunk * sizeof(int16_t));
    if (sample_count > first_chunk) {
        std::memcpy(audio_ring_buffer_.data(), samples + first_chunk, (sample_count - first_chunk) * sizeof(int16_t));
    }
    audio_write_index_ = (audio_write_index_ + sample_count) % audio_ring_buffer_.size();
    audio_buffered_samples_ = std::min(audio_buffered_samples_ + sample_count, audio_ring_buffer_.size());
}

void SessionRuntime::ProduceAudioFrameLocked() {
    if (!initialized_ || paused_ || !audio_ready_) {
        return;
    }

    const double frame_rate = use_pal_ ? 50.0 : 60.0;
    audio_frame_accumulator_ += static_cast<double>(sample_rate_) / frame_rate;
    size_t sample_count = static_cast<size_t>(audio_frame_accumulator_);
    size_t buffered_samples = 0;
    {
        std::lock_guard<std::mutex> audio_lock(audio_mutex_);
        buffered_samples = audio_buffered_samples_;
    }

    const size_t frame_samples = std::max<size_t>(
        1,
        static_cast<size_t>(std::ceil(static_cast<double>(sample_rate_) / frame_rate))
    );
    const size_t target_buffered_samples = std::max<size_t>(
        frame_samples * 2,
        1024
    );
    if (buffered_samples + sample_count < target_buffered_samples) {
        sample_count = target_buffered_samples - buffered_samples;
    }

    if (sample_count == 0) {
        return;
    }
    const size_t frame_sized_samples = static_cast<size_t>(audio_frame_accumulator_);
    if (frame_sized_samples > 0) {
        audio_frame_accumulator_ -= static_cast<double>(frame_sized_samples);
    }
    if (sample_count > static_cast<size_t>(std::numeric_limits<unsigned int>::max() / sizeof(int16_t))) {
        sample_count = std::numeric_limits<unsigned int>::max() / sizeof(int16_t);
    }

    std::vector<int16_t> mixed_buffer(sample_count, 0);
    Sound_Callback(
        reinterpret_cast<UBYTE*>(mixed_buffer.data()),
        static_cast<unsigned int>(sample_count * sizeof(int16_t))
    );
    FujiNetAndroid_MixAudio(mixed_buffer.data(), static_cast<int>(sample_count), sample_rate_);
    EnqueueAudioLocked(mixed_buffer.data(), sample_count);
}

void SessionRuntime::RunFrame(uint8_t* dst, int stride) {
    if (dst == nullptr) {
        return;
    }

    static uint32_t fallback_counter = 0;
    if (core_ready_ && Screen_atari != nullptr) {
        ApplyPendingJoystickStateLocked();
        Atari800_Frame();
        ProduceAudioFrameLocked();

        const int src_width = Screen_WIDTH;
        const int src_height = Screen_HEIGHT;
        int copy_width = frame_width_;
        int copy_height = frame_height_;
        if (copy_width > src_width) copy_width = src_width;
        if (copy_height > src_height) copy_height = src_height;
        const int x_offset = (src_width - copy_width) / 2;
        const UBYTE* src = reinterpret_cast<const UBYTE*>(Screen_atari);

        const bool use_ntsc_filter = !use_pal_ && artifacting_mode_ == 3 && FILTER_NTSC_emu != nullptr;
        if (use_ntsc_filter) {
            const int filtered_width = ATARI_NTSC_OUT_WIDTH(copy_width);
            const size_t filtered_pixels = static_cast<size_t>(filtered_width) * copy_height;
            if (ntsc_filter_argb_buffer_.size() < filtered_pixels) {
                ntsc_filter_argb_buffer_.resize(filtered_pixels);
            }

            atari_ntsc_blit_argb32(
                FILTER_NTSC_emu,
                reinterpret_cast<const ATARI_NTSC_IN_T*>(src + x_offset),
                src_width,
                copy_width,
                copy_height,
                ntsc_filter_argb_buffer_.data(),
                filtered_width * static_cast<long>(sizeof(uint32_t))
            );

            for (int y = 0; y < copy_height; ++y) {
                const uint32_t* filtered_row = ntsc_filter_argb_buffer_.data() + static_cast<size_t>(y) * filtered_width;
                uint8_t* dst_row = dst + y * stride;
                for (int x = 0; x < copy_width; ++x) {
                    const int filtered_x = (x * filtered_width) / copy_width;
                    const uint32_t argb = filtered_row[filtered_x];
                    const int pixel_offset = x * 4;
                    dst_row[pixel_offset + 0] = static_cast<uint8_t>((argb >> 16) & 0xFF);
                    dst_row[pixel_offset + 1] = static_cast<uint8_t>((argb >> 8) & 0xFF);
                    dst_row[pixel_offset + 2] = static_cast<uint8_t>(argb & 0xFF);
                    dst_row[pixel_offset + 3] = static_cast<uint8_t>((argb >> 24) & 0xFF);
                }
            }
        } else {
            for (int y = 0; y < copy_height; ++y) {
                const UBYTE* src_row = src + y * src_width + x_offset;
                uint8_t* dst_row = dst + y * stride;
                for (int x = 0; x < copy_width; ++x) {
                    const UBYTE idx = src_row[x];
                    const int pixel_offset = x * 4;
                    dst_row[pixel_offset + 0] = Colours_GetR(idx);
                    dst_row[pixel_offset + 1] = Colours_GetG(idx);
                    dst_row[pixel_offset + 2] = Colours_GetB(idx);
                    dst_row[pixel_offset + 3] = 0xFF;
                }
            }
        }
        return;
    }

    if ((fallback_counter % 60) == 0) {
        LOGE("Using fallback test pattern (coreReady=%d Screen_atari=%p)",
             core_ready_ ? 1 : 0,
             Screen_atari);
    }
    fallback_counter++;
    for (int y = 0; y < frame_height_; ++y) {
        uint8_t* row = dst + y * stride;
        for (int x = 0; x < frame_width_; ++x) {
            const uint8_t r = static_cast<uint8_t>((x + fallback_counter) & 0xFF);
            const uint8_t g = static_cast<uint8_t>((y + fallback_counter) & 0xFF);
            const uint8_t b = static_cast<uint8_t>(((x ^ y) + fallback_counter) & 0xFF);
            const int pixel_offset = x * 4;
            row[pixel_offset + 0] = r;
            row[pixel_offset + 1] = g;
            row[pixel_offset + 2] = b;
            row[pixel_offset + 3] = 0xFF;
        }
    }
}

void SessionRuntime::ClearSurfaceRef(JNIEnv* env) {
    if (attached_surface_ != nullptr) {
        env->DeleteGlobalRef(attached_surface_);
        attached_surface_ = nullptr;
    }
}
