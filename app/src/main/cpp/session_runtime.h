#pragma once

#include <jni.h>
#include <array>
#include <cstdint>
#include <mutex>
#include <string>
#include <vector>

class SessionRuntime {
public:
    static SessionRuntime& Get();

    void StartSession(
        int width,
        int height,
        int sample_rate,
        bool fuji_net_enabled,
        const char* runtime_root_path,
        int machine_type,
        int memory_size_kb,
        bool basic_enabled
    );
    void PauseSession(bool paused);
    void AttachSurface(JNIEnv* env, jobject surface, int width, int height);
    void DetachSurface(JNIEnv* env);
    jlong GetSessionToken() const;
    jboolean IsSessionAlive(jlong session_token) const;
    jboolean MountDisk(JNIEnv* env, jstring path, jint drive_number);
    jboolean EjectDisk(jint drive_number);
    jboolean InsertCartridge(JNIEnv* env, jstring path);
    jboolean RemoveCartridge();
    jboolean LoadExecutable(JNIEnv* env, jstring path);
    jboolean SetCustomRomPath(JNIEnv* env, jstring path);
    jboolean ClearCustomRomPath();
    jboolean SetBasicRomPath(JNIEnv* env, jstring path);
    jboolean ClearBasicRomPath();
    jboolean SetAtari400800RomPath(JNIEnv* env, jstring path);
    jboolean ClearAtari400800RomPath();
    void SetTurboEnabled(bool enabled);
    void SetVideoStandard(bool pal);
    void SetSioPatchMode(int mode);
    void SetArtifactingMode(int mode);
    void SetNtscFilterConfig(
        int preset,
        float sharpness,
        float resolution,
        float artifacts,
        float fringing,
        float bleed,
        float burst_phase
    );
    void SetStereoPokeyEnabled(bool enabled);
    void SetHDevicePath(JNIEnv* env, jint slot, jstring path);

    jboolean LoadRom(JNIEnv* env, jbyteArray rom_data);
    jboolean LoadCartridge(JNIEnv* env, jbyteArray data);
    jboolean LoadFile(JNIEnv* env, jstring path);
    jboolean LoadXex(JNIEnv* env, jbyteArray data);
    jboolean LoadAtr(JNIEnv* env, jbyteArray data);
    void ResetSystem(bool notify_fuji_net = true);
    void WarmResetSystem();
    void SetKeyState(jint akey_code, jboolean pressed);
    void SetConsoleKeys(jboolean start, jboolean select, jboolean option);
    void SetJoystickState(jint port, jfloat x, jfloat y, jboolean fire);
    void RenderFrame(JNIEnv* env, jobject buffer);
    void FillAudioBuffer(JNIEnv* env, jshortArray audio_buffer);
    void SetGamepadState(jint player_index, jint buttons_mask, jfloat axis_x, jfloat axis_y);

private:
    static constexpr int kFujiNetNetSioPort = 9997;

    SessionRuntime() = default;
    SessionRuntime(const SessionRuntime&) = delete;
    SessionRuntime& operator=(const SessionRuntime&) = delete;

    void InitializeCore();
    void ReconfigureCoreLocked();
    void UpdateNetSioConfiguration();
    void ApplyPatchSettingsLocked();
    void ApplyArtifactingLocked();
    void UpdateNtscFilterLocked();
    void ApplyPendingControlStateLocked();
    void ApplyPendingJoystickStateLocked();
    void ResetQueuedAudioLocked();
    void ProduceAudioFrameLocked();
    void EnqueueAudioLocked(const int16_t* samples, size_t sample_count);
    void RunFrame(uint8_t* dst, int stride);
    void ClearSurfaceRef(JNIEnv* env);

    mutable std::mutex core_mutex_;
    mutable std::mutex surface_mutex_;
    mutable std::mutex command_mutex_;
    mutable std::mutex input_mutex_;
    mutable std::mutex audio_mutex_;
    int frame_width_ = 320;
    int frame_height_ = 240;
    int sample_rate_ = 44100;
    bool initialized_ = false;
    bool paused_ = false;
    bool core_ready_ = false;
    bool audio_ready_ = false;
    bool use_pal_ = false;
    bool fuji_net_enabled_ = false;
    std::string runtime_root_path_;
    int machine_type_ = 1;
    int memory_size_kb_ = 64;
    bool basic_enabled_ = false;
    int sio_patch_mode_ = 0;
    int artifacting_mode_ = 0;
    int ntsc_filter_preset_ = 0;
    float ntsc_filter_sharpness_ = -0.5f;
    float ntsc_filter_resolution_ = -0.1f;
    float ntsc_filter_artifacts_ = 0.0f;
    float ntsc_filter_fringing_ = 0.0f;
    float ntsc_filter_bleed_ = 0.0f;
    float ntsc_filter_burst_phase_ = 0.0f;
    bool stereo_pokey_enabled_ = false;
    std::array<std::string, 4> h_device_paths_{};
    bool netsio_configured_ = false;
    bool surface_attached_ = false;
    uint64_t session_token_ = 0;
    jobject attached_surface_ = nullptr;
    int surface_width_ = 0;
    int surface_height_ = 0;
    std::array<float, 4> pending_joystick_x_{};
    std::array<float, 4> pending_joystick_y_{};
    std::array<bool, 4> pending_joystick_fire_{};
    std::array<bool, 4> pending_joystick_dirty_{};
    bool pending_reset_requested_ = false;
    bool pending_reset_notify_fuji_net_ = true;
    bool pending_warm_reset_requested_ = false;
    std::vector<uint32_t> ntsc_filter_argb_buffer_{};
    std::vector<int16_t> audio_ring_buffer_{};
    size_t audio_read_index_ = 0;
    size_t audio_write_index_ = 0;
    size_t audio_buffered_samples_ = 0;
    double audio_frame_accumulator_ = 0.0;
};
