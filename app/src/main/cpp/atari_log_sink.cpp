#include "atari_log_sink.h"

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <deque>
#include <filesystem>
#include <mutex>
#include <string>

namespace {
constexpr size_t kMaxLogFileBytes = 1024 * 1024;

std::mutex g_log_mutex;
std::string g_log_file_path;
size_t g_log_file_size = 0;
std::deque<char> g_log_tail;

void rewrite_log_file_locked() {
    if (g_log_file_path.empty()) {
        return;
    }

    FILE* file = std::fopen(g_log_file_path.c_str(), "wb");
    if (file == nullptr) {
        return;
    }

    if (!g_log_tail.empty()) {
        std::string snapshot(g_log_tail.begin(), g_log_tail.end());
        std::fwrite(snapshot.data(), 1, snapshot.size(), file);
        g_log_file_size = snapshot.size();
    } else {
        g_log_file_size = 0;
    }
    std::fclose(file);
}

void append_log_chunk_locked(const char* text, size_t size) {
    if (text == nullptr || size == 0 || g_log_file_path.empty()) {
        return;
    }

    const size_t keep_bytes = std::min(size, kMaxLogFileBytes);
    const char* chunk = text + (size - keep_bytes);

    for (size_t index = 0; index < keep_bytes; ++index) {
        g_log_tail.push_back(chunk[index]);
    }
    while (g_log_tail.size() > kMaxLogFileBytes) {
        g_log_tail.pop_front();
    }

    FILE* file = std::fopen(g_log_file_path.c_str(), "ab");
    if (file != nullptr) {
        std::fwrite(chunk, 1, keep_bytes, file);
        std::fclose(file);
        g_log_file_size += keep_bytes;
    }

    if (g_log_file_size > kMaxLogFileBytes) {
        rewrite_log_file_locked();
    }
}
}  // namespace

extern "C" void AtariLogSink_StartCapture(const char* runtime_root_path) {
    std::lock_guard<std::mutex> lock(g_log_mutex);

    g_log_tail.clear();
    g_log_file_size = 0;
    g_log_file_path.clear();

    if (runtime_root_path == nullptr || runtime_root_path[0] == '\0') {
        return;
    }

    std::error_code ec;
    std::filesystem::path runtime_root(runtime_root_path);
    std::filesystem::create_directories(runtime_root, ec);
    g_log_file_path = (runtime_root / "a800-console.log").string();
    rewrite_log_file_locked();
}

extern "C" void AtariLogSink_StopCapture(void) {
    std::lock_guard<std::mutex> lock(g_log_mutex);
    g_log_tail.clear();
    g_log_file_size = 0;
    g_log_file_path.clear();
}

extern "C" void AtariLogSink_Write(const char* text) {
    if (text == nullptr) {
        return;
    }
    std::lock_guard<std::mutex> lock(g_log_mutex);
    append_log_chunk_locked(text, std::strlen(text));
}
