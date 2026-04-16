#include <android/log.h>
#include <arpa/inet.h>
#include <dlfcn.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>

#include <atomic>
#include <cerrno>
#include <cstdint>
#include <cstring>
#include <filesystem>
#include <limits>
#include <mutex>
#include <string>
#include <vector>

namespace {
constexpr const char* LOG_TAG = "FujiNetAndroid";
std::mutex g_mutex;
std::string g_last_error;
void* g_library_handle = nullptr;
bool g_running = false;

using StartRuntimeFn = bool (*)(
        const char* runtimeRootPath,
        const char* configPath,
        const char* sdPath,
        const char* dataPath,
        int listenPort
);
using StopRuntimeFn = void (*)();
using LastErrorMessageFn = const char* (*)();
using ReadAudioFn = int (*)(int16_t* output, int maxSamples, int outputSampleRate);
using ClearAudioFn = void (*)();
using CopyRecentLogFn = int (*)(char* output, int maxBytes);

StartRuntimeFn g_start_runtime = nullptr;
StopRuntimeFn g_stop_runtime = nullptr;
LastErrorMessageFn g_last_error_message = nullptr;
ReadAudioFn g_read_audio = nullptr;
ClearAudioFn g_clear_audio = nullptr;
CopyRecentLogFn g_copy_recent_log = nullptr;

void set_last_error_locked(const std::string& message) {
    g_last_error = message;
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s", g_last_error.c_str());
}

bool load_fujinet_library_locked() {
    if (g_library_handle != nullptr) {
        return true;
    }

    g_library_handle = dlopen("libfujinet.so", RTLD_NOW | RTLD_LOCAL);
    if (g_library_handle == nullptr) {
        const char* dl_error = dlerror();
        set_last_error_locked(
                std::string("FujiNet shared library could not be loaded: ")
                + (dl_error != nullptr ? dl_error : "dlopen returned null")
        );
        return false;
    }

    g_start_runtime = reinterpret_cast<StartRuntimeFn>(
            dlsym(g_library_handle, "fujinet_android_start_runtime")
    );
    g_stop_runtime = reinterpret_cast<StopRuntimeFn>(
            dlsym(g_library_handle, "fujinet_android_stop_runtime")
    );
    g_last_error_message = reinterpret_cast<LastErrorMessageFn>(
            dlsym(g_library_handle, "fujinet_android_last_error_message")
    );
    g_read_audio = reinterpret_cast<ReadAudioFn>(
            dlsym(g_library_handle, "fujinet_android_read_audio")
    );
    g_clear_audio = reinterpret_cast<ClearAudioFn>(
            dlsym(g_library_handle, "fujinet_android_clear_audio")
    );
    g_copy_recent_log = reinterpret_cast<CopyRecentLogFn>(
            dlsym(g_library_handle, "fujinet_android_copy_recent_log")
    );

    if (g_start_runtime == nullptr || g_stop_runtime == nullptr || g_last_error_message == nullptr) {
        set_last_error_locked("FujiNet shared library is missing the Android runtime contract");
        dlclose(g_library_handle);
        g_library_handle = nullptr;
        g_start_runtime = nullptr;
        g_stop_runtime = nullptr;
        g_last_error_message = nullptr;
        g_read_audio = nullptr;
        g_clear_audio = nullptr;
        g_copy_recent_log = nullptr;
        return false;
    }

    return true;
}

bool validate_runtime_layout_locked(
        const char* runtime_root_path,
        const char* config_path,
        const char* sd_path,
        const char* data_path
) {
    namespace fs = std::filesystem;

    if (runtime_root_path == nullptr || config_path == nullptr || sd_path == nullptr || data_path == nullptr) {
        set_last_error_locked("FujiNet runtime paths were missing");
        return false;
    }

    std::error_code ec;
    const fs::path runtime_root(runtime_root_path);
    const fs::path config_file(config_path);
    const fs::path sd_directory(sd_path);
    const fs::path data_directory(data_path);

    if (!fs::exists(runtime_root, ec) || !fs::is_directory(runtime_root, ec)) {
        set_last_error_locked("FujiNet runtime root is unavailable");
        return false;
    }
    if (!fs::exists(config_file, ec) || !fs::is_regular_file(config_file, ec)) {
        set_last_error_locked("FujiNet config file is unavailable");
        return false;
    }
    if (!fs::exists(sd_directory, ec) || !fs::is_directory(sd_directory, ec)) {
        set_last_error_locked("FujiNet SD directory is unavailable");
        return false;
    }
    if (!fs::exists(data_directory, ec) || !fs::is_directory(data_directory, ec)) {
        set_last_error_locked("FujiNet data directory is unavailable");
        return false;
    }

    return true;
}
}  // namespace

extern "C" bool FujiNetAndroid_StartRuntime(
        const char* runtimeRootPath,
        const char* configPath,
        const char* sdPath,
        const char* dataPath,
        int listenPort
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last_error.clear();

    if (g_running) {
        return true;
    }

    if (!validate_runtime_layout_locked(runtimeRootPath, configPath, sdPath, dataPath)) {
        return false;
    }

    if (!load_fujinet_library_locked()) {
        return false;
    }

    const bool started = g_start_runtime(
            runtimeRootPath,
            configPath,
            sdPath,
            dataPath,
            listenPort
    );
    if (!started) {
        const char* runtime_error = g_last_error_message != nullptr ? g_last_error_message() : nullptr;
        if (runtime_error != nullptr && runtime_error[0] != '\0') {
            set_last_error_locked(runtime_error);
        } else {
            set_last_error_locked("FujiNet shared runtime failed to start");
        }
        return false;
    }

    g_running = true;
    __android_log_print(
            ANDROID_LOG_INFO,
            LOG_TAG,
            "FujiNet runtime started at %s on UDP %d",
            runtimeRootPath,
            listenPort
    );
    return true;
}

extern "C" void FujiNetAndroid_StopRuntime() {
    void* library_handle = nullptr;
    StopRuntimeFn stop_runtime = nullptr;
    ClearAudioFn clear_audio = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        if (!g_running && g_library_handle == nullptr) {
            return;
        }
        stop_runtime = g_stop_runtime;
        clear_audio = g_clear_audio;
        library_handle = g_library_handle;
        g_library_handle = nullptr;
        g_start_runtime = nullptr;
        g_stop_runtime = nullptr;
        g_last_error_message = nullptr;
        g_read_audio = nullptr;
        g_clear_audio = nullptr;
        g_copy_recent_log = nullptr;
        g_running = false;
    }

    if (stop_runtime != nullptr) {
        stop_runtime();
    }
    if (clear_audio != nullptr) {
        clear_audio();
    }
    if (library_handle != nullptr) {
        dlclose(library_handle);
    }
}

extern "C" const char* FujiNetAndroid_LastErrorMessage() {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_last_error.empty()) {
        if (g_last_error_message != nullptr) {
            return g_last_error_message();
        }
        return nullptr;
    }
    return g_last_error.c_str();
}

extern "C" bool FujiNetAndroid_IsRuntimeRunning() {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_running;
}

extern "C" int FujiNetAndroid_CopyRecentLog(char* output, int maxBytes) {
    if (output == nullptr || maxBytes <= 0) {
        return 0;
    }

    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_copy_recent_log == nullptr) {
        output[0] = '\0';
        return 0;
    }
    return g_copy_recent_log(output, maxBytes);
}

extern "C" void FujiNetAndroid_MixAudio(
        int16_t* output,
        int sampleCount,
        int outputSampleRate
) {
    if (output == nullptr || sampleCount <= 0) {
        return;
    }

    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_running || g_read_audio == nullptr) {
        return;
    }

    std::vector<int16_t> overlay(static_cast<size_t>(sampleCount));
    const int produced = g_read_audio(overlay.data(), sampleCount, outputSampleRate);
    for (int index = 0; index < produced; ++index) {
        const int mixed = static_cast<int>(output[index]) + static_cast<int>(overlay[index]);
        if (mixed > std::numeric_limits<int16_t>::max()) {
            output[index] = std::numeric_limits<int16_t>::max();
        } else if (mixed < std::numeric_limits<int16_t>::min()) {
            output[index] = std::numeric_limits<int16_t>::min();
        } else {
            output[index] = static_cast<int16_t>(mixed);
        }
    }
}

extern "C" void FujiNetAndroid_ClearAudio() {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_clear_audio != nullptr) {
        g_clear_audio();
    }
}
