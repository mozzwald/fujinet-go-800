#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
PROJECT_ROOT=$(cd -- "${SCRIPT_DIR}/../.." &>/dev/null && pwd)
SUPPORT_DIR="${SCRIPT_DIR}/support"
WORK_ROOT="${SCRIPT_DIR}/work"
CLONE_DIR="${WORK_ROOT}/fujinet-firmware"
GENERATED_ASSET_ROOT="${PROJECT_ROOT}/app/src/main/assets-generated/fujinet"
GENERATED_JNI_ROOT="${PROJECT_ROOT}/app/src/main/jniLibs-generated"

UPSTREAM_URL="https://github.com/mozzwald/fujinet-firmware"
UPSTREAM_BRANCH="android"
UPSTREAM_COMMIT="6d8d610f37e403dea8e8f88f8ab0fda0283a06eb"
ARCHIVE_PATH="${WORK_ROOT}/fujinet-firmware-${UPSTREAM_COMMIT}.tar.gz"

MBEDTLS_URL="https://github.com/Mbed-TLS/mbedtls.git"
MBEDTLS_TAG="mbedtls-3.6.5"
MBEDTLS_COMMIT="e185d7fd85499c8ce5ca2a54f5cf8fe7dbe3f8df"
MBEDTLS_SOURCE_DIR="${WORK_ROOT}/mbedtls-src"
MBEDTLS_BUILD_ROOT="${WORK_ROOT}/mbedtls-build"
MBEDTLS_INSTALL_ROOT="${WORK_ROOT}/mbedtls-install"
DEFAULT_ANDROID_ABIS=("armeabi-v7a" "arm64-v8a" "x86" "x86_64")
# DEFAULT_ANDROID_ABIS=("armeabi-v7a" "arm64-v8a")
# DEFAULT_ANDROID_ABIS=("arm64-v8a")

ANDROID_ABI_VALUE=""
REQUESTED_ABIS=()
BUILD_ALL_ABIS=0
REFRESH_CLONE=0

usage() {
    cat <<'EOF'
Usage: bash tools/fujinet/build-fujinet.sh (--all-abis | --abi <abi> [--abi <abi> ...]) [--refresh]

Builds a pinned FujiNet Android packaging proof from a fresh upstream clone.
EOF
}

fail() {
    echo "build-fujinet.sh: $*" >&2
    exit 1
}

abi_supported() {
    local abi="$1"
    local candidate
    for candidate in "${DEFAULT_ANDROID_ABIS[@]}"; do
        if [[ "${candidate}" == "${abi}" ]]; then
            return 0
        fi
    done
    return 1
}

resolve_requested_abis() {
    local -a resolved=()
    local abi

    if [[ "${BUILD_ALL_ABIS}" -eq 1 ]]; then
        printf '%s\n' "${DEFAULT_ANDROID_ABIS[@]}"
        return
    fi

    [[ "${#REQUESTED_ABIS[@]}" -gt 0 ]] || fail "Specify --all-abis or at least one --abi"

    for abi in "${REQUESTED_ABIS[@]}"; do
        abi_supported "${abi}" || fail "Unsupported ABI: ${abi}"
        if [[ " ${resolved[*]} " != *" ${abi} "* ]]; then
            resolved+=("${abi}")
        fi
    done

    printf '%s\n' "${resolved[@]}"
}

read_sdk_dir() {
    if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
        printf '%s\n' "${ANDROID_SDK_ROOT}"
        return
    fi

    local local_properties="${PROJECT_ROOT}/local.properties"
    if [[ ! -f "${local_properties}" ]]; then
        fail "ANDROID_SDK_ROOT is not set and local.properties is missing"
    fi

    local sdk_dir
    sdk_dir=$(sed -n 's/^sdk.dir=//p' "${local_properties}" | tail -n 1)
    [[ -n "${sdk_dir}" ]] || fail "Unable to resolve sdk.dir from local.properties"
    printf '%s\n' "${sdk_dir}"
}

find_latest_ndk() {
    local sdk_dir="$1"
    local ndk_root="${sdk_dir}/ndk"
    [[ -d "${ndk_root}" ]] || fail "NDK directory not found at ${ndk_root}"

    local candidate
    while IFS= read -r candidate; do
        if [[ -f "${ndk_root}/${candidate}/build/cmake/android.toolchain.cmake" ]]; then
            printf '%s\n' "${ndk_root}/${candidate}"
            return
        fi
    done < <(find "${ndk_root}" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -Vr)

    fail "No installed NDK with build/cmake/android.toolchain.cmake found under ${ndk_root}"
}

download_archive() {
    local archive_url="https://codeload.github.com/mozzwald/fujinet-firmware/tar.gz/${UPSTREAM_COMMIT}"

    if [[ "${REFRESH_CLONE}" -eq 1 ]]; then
        rm -f "${ARCHIVE_PATH}"
    fi

    if [[ -f "${ARCHIVE_PATH}" ]] && gzip -t "${ARCHIVE_PATH}" >/dev/null 2>&1; then
        return
    fi

    if command -v wget >/dev/null 2>&1; then
        wget -q -O "${ARCHIVE_PATH}" "${archive_url}"
    else
        curl -L --fail --silent --show-error "${archive_url}" -o "${ARCHIVE_PATH}"
    fi

    gzip -t "${ARCHIVE_PATH}" >/dev/null 2>&1 || fail "Downloaded archive is incomplete: ${ARCHIVE_PATH}"
}

prepare_fresh_clone() {
    local branch_ref
    branch_ref=$(git ls-remote --heads "${UPSTREAM_URL}" "${UPSTREAM_BRANCH}" | awk 'NR == 1 { print $1 }')
    [[ -n "${branch_ref}" ]] || fail "Unable to resolve upstream branch ${UPSTREAM_BRANCH}"

    rm -rf "${CLONE_DIR}"
    mkdir -p "${WORK_ROOT}"
    download_archive

    local archive_root
    archive_root=$(tar -tzf "${ARCHIVE_PATH}" | sed -n '1p' | cut -d/ -f1)
    [[ -n "${archive_root}" ]] || fail "Unable to determine extracted archive root"

    tar -xzf "${ARCHIVE_PATH}" -C "${WORK_ROOT}"
    mv "${WORK_ROOT}/${archive_root}" "${CLONE_DIR}"
}

prepare_mbedtls_source() {
    if [[ "${REFRESH_CLONE}" -eq 1 ]]; then
        rm -rf "${MBEDTLS_SOURCE_DIR}"
    fi

    if [[ ! -d "${MBEDTLS_SOURCE_DIR}/.git" ]]; then
        rm -rf "${MBEDTLS_SOURCE_DIR}"
        mkdir -p "${WORK_ROOT}"
        git clone --depth 1 --branch "${MBEDTLS_TAG}" "${MBEDTLS_URL}" "${MBEDTLS_SOURCE_DIR}"
    fi

    (
        cd "${MBEDTLS_SOURCE_DIR}"
        git fetch --depth 1 origin "${MBEDTLS_COMMIT}"
        git checkout --detach "${MBEDTLS_COMMIT}"
        git submodule update --init --recursive --depth 1
    )

    local resolved_commit
    resolved_commit=$(git -C "${MBEDTLS_SOURCE_DIR}" rev-parse HEAD)
    [[ "${resolved_commit}" == "${MBEDTLS_COMMIT}" ]] || fail "Mbed TLS checkout resolved to ${resolved_commit}, expected ${MBEDTLS_COMMIT}"
}

apply_android_patches() {
    python3 - "${CLONE_DIR}" "${SUPPORT_DIR}" <<'PY'
from pathlib import Path
import sys

clone_dir = Path(sys.argv[1])
support_dir = Path(sys.argv[2])

build_sh = clone_dir / "build.sh"
build_sh_text = build_sh.read_text()
build_sh_text = build_sh_text.replace(
    '  export PROJECT_CONFIG=$INI_FILE\n  GEN_CMD=""\n',
    '  export PROJECT_CONFIG=$INI_FILE\n'
    '  CMAKE_EXTRA_ARGS=()\n'
    '  if [ -n "${ANDROID_NDK_HOME}" ] ; then\n'
    '    if [ -z "${ANDROID_ABI}" ] ; then\n'
    '      echo "ANDROID_ABI must be set for Android PC builds"\n'
    '      exit 1\n'
    '    fi\n'
    '    ANDROID_PLATFORM=${ANDROID_PLATFORM:-android-26}\n'
    '    CMAKE_EXTRA_ARGS+=(\n'
    '      "-DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake"\n'
    '      "-DANDROID_ABI=${ANDROID_ABI}"\n'
    '      "-DANDROID_PLATFORM=${ANDROID_PLATFORM}"\n'
    '      "-DANDROID_STL=c++_static"\n'
    '      "-DFUJINET_ANDROID=ON"\n'
    '    )\n'
    '    if [ -n "${MBEDTLS_ROOT_DIR}" ] ; then\n'
    '      CMAKE_EXTRA_ARGS+=("-DMBEDTLS_ROOT_DIR=${MBEDTLS_ROOT_DIR}")\n'
    '    fi\n'
    '  fi\n'
    '  GEN_CMD=""\n',
)
build_sh_text = build_sh_text.replace(
    '    cmake .. -DCMAKE_EXPORT_COMPILE_COMMANDS=1 -DFUJINET_TARGET=$PC_TARGET "$@"\n',
    '    cmake .. "${CMAKE_EXTRA_ARGS[@]}" -DCMAKE_EXPORT_COMPILE_COMMANDS=1 -DFUJINET_TARGET=$PC_TARGET "$@"\n',
)
build_sh_text = build_sh_text.replace(
    '    cmake "$GEN_CMD" .. -DCMAKE_EXPORT_COMPILE_COMMANDS=1 -DFUJINET_TARGET=$PC_TARGET "$@"\n',
    '    cmake "$GEN_CMD" .. "${CMAKE_EXTRA_ARGS[@]}" -DCMAKE_EXPORT_COMPILE_COMMANDS=1 -DFUJINET_TARGET=$PC_TARGET "$@"\n',
)
build_sh_text = build_sh_text.replace(
    '    cmake .. -DFUJINET_TARGET=$PC_TARGET -DCMAKE_BUILD_TYPE=$BUILD_TYPE "$@"\n',
    '    cmake .. "${CMAKE_EXTRA_ARGS[@]}" -DFUJINET_TARGET=$PC_TARGET -DCMAKE_BUILD_TYPE=$BUILD_TYPE "$@"\n',
)
build_sh_text = build_sh_text.replace(
    '    cmake "$GEN_CMD" .. -DFUJINET_TARGET=$PC_TARGET -DCMAKE_BUILD_TYPE=$BUILD_TYPE "$@"\n',
    '    cmake "$GEN_CMD" .. "${CMAKE_EXTRA_ARGS[@]}" -DFUJINET_TARGET=$PC_TARGET -DCMAKE_BUILD_TYPE=$BUILD_TYPE "$@"\n',
)
build_sh_text = build_sh_text.replace(
    '        pip install platformio || exit 1\n',
    '        pip install platformio pyyaml jinja2 || exit 1\n',
)
build_sh.write_text(build_sh_text)

fujinet_cmake = clone_dir / "fujinet_pc.cmake"
fujinet_cmake_text = fujinet_cmake.read_text()
fujinet_cmake_text = fujinet_cmake_text.replace(
    'add_executable(fujinet ${SOURCES})\n',
    'if(FUJINET_ANDROID)\n'
    '    add_library(fujinet SHARED ${SOURCES} android/fujinet_android_entry.cpp)\n'
    '    set_target_properties(fujinet PROPERTIES OUTPUT_NAME "fujinet")\n'
    '    target_compile_definitions(fujinet PRIVATE FUJINET_ANDROID=1)\n'
    'else()\n'
    '    add_executable(fujinet ${SOURCES})\n'
    'endif()\n',
)
fujinet_cmake_text = fujinet_cmake_text.replace(
    'set(_MBEDTLS_ROOT_HINTS $ENV{MBEDTLS_ROOT_DIR} ${MBEDTLS_ROOT_DIR})\n'
    'set(_MBEDTLS_ROOT_PATHS "$ENV{PROGRAMFILES}/libmbedtls")\n'
    'set(_MBEDTLS_ROOT_HINTS_AND_PATHS HINTS ${_MBEDTLS_ROOT_HINTS} PATHS ${_MBEDTLS_ROOT_PATHS})\n'
    'find_library(MBEDTLS_STATIC_LIB libmbedtls.a HINTS ${_MBEDTLS_ROOT_HINTS_AND_PATHS})\n'
    'find_library(MBEDX509_STATIC_LIB libmbedx509.a HINTS ${_MBEDTLS_ROOT_HINTS_AND_PATHS})\n'
    'find_library(MBEDCRYPTO_STATIC_LIB libmbedcrypto.a HINTS ${_MBEDTLS_ROOT_HINTS_AND_PATHS})\n'
    'find_path(MBEDTLS_INCLUDE_DIR mbedtls/ssl.h HINTS ${_MBEDTLS_ROOT_HINTS_AND_PATHS} PATH_SUFFIXES include)\n',
    'if(FUJINET_ANDROID AND DEFINED MBEDTLS_ROOT_DIR)\n'
    '    set(MBEDTLS_STATIC_LIB "${MBEDTLS_ROOT_DIR}/lib/libmbedtls.a")\n'
    '    set(MBEDX509_STATIC_LIB "${MBEDTLS_ROOT_DIR}/lib/libmbedx509.a")\n'
    '    set(MBEDCRYPTO_STATIC_LIB "${MBEDTLS_ROOT_DIR}/lib/libmbedcrypto.a")\n'
    '    set(MBEDTLS_INCLUDE_DIR "${MBEDTLS_ROOT_DIR}/include")\n'
    'else()\n'
    '    set(_MBEDTLS_ROOT_HINTS $ENV{MBEDTLS_ROOT_DIR} ${MBEDTLS_ROOT_DIR})\n'
    '    set(_MBEDTLS_ROOT_PATHS "$ENV{PROGRAMFILES}/libmbedtls")\n'
    '    set(_MBEDTLS_ROOT_HINTS_AND_PATHS HINTS ${_MBEDTLS_ROOT_HINTS} PATHS ${_MBEDTLS_ROOT_PATHS})\n'
    '    find_library(MBEDTLS_STATIC_LIB libmbedtls.a HINTS ${_MBEDTLS_ROOT_HINTS_AND_PATHS})\n'
    '    find_library(MBEDX509_STATIC_LIB libmbedx509.a HINTS ${_MBEDTLS_ROOT_HINTS_AND_PATHS})\n'
    '    find_library(MBEDCRYPTO_STATIC_LIB libmbedcrypto.a HINTS ${_MBEDTLS_ROOT_HINTS_AND_PATHS})\n'
    '    find_path(MBEDTLS_INCLUDE_DIR mbedtls/ssl.h HINTS ${_MBEDTLS_ROOT_HINTS_AND_PATHS} PATH_SUFFIXES include)\n'
    'endif()\n',
)
fujinet_cmake_text = fujinet_cmake_text.replace(
    '        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:fujinet> dist\n',
    '        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:fujinet> dist/libfujinet.so\n',
)
fujinet_cmake_text = fujinet_cmake_text.replace(
    'add_subdirectory(components_pc/libssh)\n'
    '\n'
    'target_link_libraries(fujinet pthread expat cjson cjson_utils smb2 ssh)\n',
    'add_subdirectory(components_pc/libssh)\n'
    '\n'
    'if(FUJINET_ANDROID)\n'
    '    set(ENABLE_PROGRAMS OFF CACHE BOOL "" FORCE)\n'
    '    set(ENABLE_TESTING OFF CACHE BOOL "" FORCE)\n'
    '    add_subdirectory(components/expat/expat/expat)\n'
    '    find_library(ANDROID_LOG_LIB log)\n'
    '    target_link_libraries(fujinet expat cjson cjson_utils smb2 ssh ${ANDROID_LOG_LIB})\n'
    'else()\n'
    '    target_link_libraries(fujinet pthread expat cjson cjson_utils smb2 ssh)\n'
    'endif()\n',
)
fujinet_cmake.write_text(fujinet_cmake_text)

find_mbedtls = clone_dir / "components_pc/libssh/cmake/Modules/FindMbedTLS.cmake"
find_mbedtls_text = find_mbedtls.read_text()
find_mbedtls_text = find_mbedtls_text.replace(
    'find_path(MBEDTLS_INCLUDE_DIR\n'
    '    NAMES\n'
    '        mbedtls/ssl.h\n'
    '    HINTS\n'
    '        ${_MBEDTLS_ROOT_HINTS_AND_PATHS}\n'
    '    PATH_SUFFIXES\n'
    '       include\n'
    ')\n'
    '\n'
    'find_library(MBEDTLS_SSL_LIBRARY\n'
    '        NAMES\n'
    '            libmbedtls.a\n'
    '        HINTS\n'
    '            ${_MBEDTLS_ROOT_HINTS_AND_PATHS}\n'
    '        PATH_SUFFIXES\n'
    '            lib\n'
    '\n'
    ')\n'
    '\n'
    'find_library(MBEDTLS_CRYPTO_LIBRARY\n'
    '        NAMES\n'
    '            libmbedcrypto.a\n'
    '        HINTS\n'
    '            ${_MBEDTLS_ROOT_HINTS_AND_PATHS}\n'
    '        PATH_SUFFIXES\n'
    '            lib\n'
    ')\n'
    '\n'
    'find_library(MBEDTLS_X509_LIBRARY\n'
    '        NAMES\n'
    '            libmbedx509.a\n'
    '        HINTS\n'
    '            ${_MBEDTLS_ROOT_HINTS_AND_PATHS}\n'
    '        PATH_SUFFIXES\n'
    '            lib\n'
    ')\n',
    'if(DEFINED MBEDTLS_ROOT_DIR AND EXISTS "${MBEDTLS_ROOT_DIR}/include/mbedtls/ssl.h")\n'
    '    set(MBEDTLS_INCLUDE_DIR "${MBEDTLS_ROOT_DIR}/include")\n'
    '    set(MBEDTLS_SSL_LIBRARY "${MBEDTLS_ROOT_DIR}/lib/libmbedtls.a")\n'
    '    set(MBEDTLS_CRYPTO_LIBRARY "${MBEDTLS_ROOT_DIR}/lib/libmbedcrypto.a")\n'
    '    set(MBEDTLS_X509_LIBRARY "${MBEDTLS_ROOT_DIR}/lib/libmbedx509.a")\n'
    'else()\n'
    '    find_path(MBEDTLS_INCLUDE_DIR\n'
    '        NAMES\n'
    '            mbedtls/ssl.h\n'
    '        HINTS\n'
    '            ${_MBEDTLS_ROOT_HINTS_AND_PATHS}\n'
    '        PATH_SUFFIXES\n'
    '           include\n'
    '    )\n'
    '\n'
    '    find_library(MBEDTLS_SSL_LIBRARY\n'
    '            NAMES\n'
    '                libmbedtls.a\n'
    '            HINTS\n'
    '                ${_MBEDTLS_ROOT_HINTS_AND_PATHS}\n'
    '            PATH_SUFFIXES\n'
    '                lib\n'
    '\n'
    '    )\n'
    '\n'
    '    find_library(MBEDTLS_CRYPTO_LIBRARY\n'
    '            NAMES\n'
    '                libmbedcrypto.a\n'
    '            HINTS\n'
    '                ${_MBEDTLS_ROOT_HINTS_AND_PATHS}\n'
    '            PATH_SUFFIXES\n'
    '                lib\n'
    '    )\n'
    '\n'
    '    find_library(MBEDTLS_X509_LIBRARY\n'
    '            NAMES\n'
    '                libmbedx509.a\n'
    '            HINTS\n'
    '                ${_MBEDTLS_ROOT_HINTS_AND_PATHS}\n'
    '            PATH_SUFFIXES\n'
    '                lib\n'
    '    )\n'
    'endif()\n',
)
find_mbedtls.write_text(find_mbedtls_text)

libssh_misc = clone_dir / "components_pc/libssh/src/misc.c"
libssh_misc_text = libssh_misc.read_text()
libssh_misc_text = libssh_misc_text.replace(
    '#include <sys/stat.h>\n'
    '#include <sys/types.h>\n',
    '#include <sys/stat.h>\n'
    '#include <sys/types.h>\n'
    '#ifndef S_IWRITE\n'
    '#define S_IWRITE S_IWUSR\n'
    '#endif\n',
    1,
)
libssh_misc.write_text(libssh_misc_text)

linux_termios2 = clone_dir / "lib/compat/linux_termios2.h"
linux_termios2_text = linux_termios2.read_text()
linux_termios2_text = linux_termios2_text.replace(
    'struct termios2 {\n'
    '        tcflag_t c_iflag;               /* input mode flags */\n'
    '        tcflag_t c_oflag;               /* output mode flags */\n'
    '        tcflag_t c_cflag;               /* control mode flags */\n'
    '        tcflag_t c_lflag;               /* local mode flags */\n'
    '        cc_t c_line;                    /* line discipline */\n'
    '        cc_t c_cc[LINUX_NCCS];          /* control characters */\n'
    '        speed_t c_ispeed;               /* input speed */\n'
    '        speed_t c_ospeed;               /* output speed */\n'
    '};\n',
    '#ifndef __ANDROID__\n'
    'struct termios2 {\n'
    '        tcflag_t c_iflag;               /* input mode flags */\n'
    '        tcflag_t c_oflag;               /* output mode flags */\n'
    '        tcflag_t c_cflag;               /* control mode flags */\n'
    '        tcflag_t c_lflag;               /* local mode flags */\n'
    '        cc_t c_line;                    /* line discipline */\n'
    '        cc_t c_cc[LINUX_NCCS];          /* control characters */\n'
    '        speed_t c_ispeed;               /* input speed */\n'
    '        speed_t c_ospeed;               /* output speed */\n'
    '};\n'
    '#endif\n',
    1,
)
linux_termios2.write_text(linux_termios2_text)

fnsystem_h = clone_dir / "lib/hardware/fnSystem.h"
fnsystem_h_text = fnsystem_h.read_text()
fnsystem_h_text = fnsystem_h_text.replace(
    '    int request_for_shutdown();\n'
    '    int check_for_shutdown();\n',
    '    int request_for_shutdown();\n'
    '    int check_for_shutdown();\n'
    '    void clear_shutdown_request();\n',
)
fnsystem_h.write_text(fnsystem_h_text)

fnsystem_cpp = clone_dir / "lib/hardware/fnSystem.cpp"
fnsystem_cpp_text = fnsystem_cpp.read_text()
fnsystem_cpp_text = fnsystem_cpp_text.replace(
    'int SystemManager::check_for_shutdown()\n'
    '{\n'
    '    return _shutdown_requests;\n'
    '}\n',
    'int SystemManager::check_for_shutdown()\n'
    '{\n'
    '    return _shutdown_requests;\n'
    '}\n'
    'void SystemManager::clear_shutdown_request()\n'
    '{\n'
    '    _shutdown_requests = 0;\n'
    '}\n',
)
fnsystem_cpp.write_text(fnsystem_cpp_text)

netsio_cpp = clone_dir / "lib/bus/sio/siocom/netsio.cpp"
netsio_cpp_text = netsio_cpp.read_text()
netsio_cpp_text = netsio_cpp_text.replace(
    '            case NETSIO_COLD_RESET:\n'
    '                // emulator cold reset, do fujinet restart\n'
    '#ifndef DEBUG_NO_REBOOT\n'
    '                fnSystem.reboot();\n'
    '#endif\n'
    '                break;\n',
    '            case NETSIO_COLD_RESET:\n'
    '                // Android embeds FujiNet inside the app process, so app code owns restart.\n'
    '#if defined(FUJINET_ANDROID)\n'
    '                Debug_println("NetSIO cold reset ignored in Android runtime; app will restart FujiNet");\n'
    '#else\n'
    '                // emulator cold reset, do fujinet restart\n'
    '#ifndef DEBUG_NO_REBOOT\n'
    '                fnSystem.reboot();\n'
    '#endif\n'
    '#endif\n'
    '                break;\n',
)
netsio_cpp.write_text(netsio_cpp_text)

android_dir = clone_dir / "android"
android_dir.mkdir(exist_ok=True)
wrapper_src = support_dir / "fujinet_android_entry.cpp"
(android_dir / "fujinet_android_entry.cpp").write_text(wrapper_src.read_text())
PY
}

apply_local_patch_files() {
    local patch_dir="${SCRIPT_DIR}/patches"
    [[ -d "${patch_dir}" ]] || return

    local patch_file
    while IFS= read -r patch_file; do
        patch -d "${CLONE_DIR}" -p1 < "${patch_file}"
    done < <(find "${patch_dir}" -maxdepth 1 -type f -name '*.patch' | sort)
}

configure_mbedtls_for_android() {
    python3 - "${MBEDTLS_SOURCE_DIR}/include/mbedtls/mbedtls_config.h" <<'PY'
from pathlib import Path
import sys

config_h = Path(sys.argv[1])
text = config_h.read_text()
for old, new in (
    ('//#define MBEDTLS_THREADING_C\n', '#define MBEDTLS_THREADING_C\n'),
    ('//#define MBEDTLS_THREADING_PTHREAD\n', '#define MBEDTLS_THREADING_PTHREAD\n'),
):
    if old in text:
        text = text.replace(old, new)
config_h.write_text(text)
PY
}

build_mbedtls() {
    local mbedtls_build_dir="${MBEDTLS_BUILD_ROOT}/${ANDROID_ABI_VALUE}"
    local mbedtls_install_dir="${MBEDTLS_INSTALL_ROOT}/${ANDROID_ABI_VALUE}"

    if [[ "${REFRESH_CLONE}" -eq 0 ]] \
        && [[ -f "${mbedtls_install_dir}/lib/libmbedtls.a" ]] \
        && [[ -f "${mbedtls_install_dir}/lib/libmbedx509.a" ]] \
        && [[ -f "${mbedtls_install_dir}/lib/libmbedcrypto.a" ]]; then
        export MBEDTLS_ROOT_DIR="${mbedtls_install_dir}"
        return
    fi

    prepare_mbedtls_source
    configure_mbedtls_for_android

    rm -rf "${mbedtls_build_dir}" "${mbedtls_install_dir}"
    mkdir -p "${MBEDTLS_BUILD_ROOT}" "${MBEDTLS_INSTALL_ROOT}"

    cmake \
        -S "${MBEDTLS_SOURCE_DIR}" \
        -B "${mbedtls_build_dir}" \
        -DCMAKE_TOOLCHAIN_FILE="${TOOLCHAIN_FILE}" \
        -DANDROID_ABI="${ANDROID_ABI_VALUE}" \
        -DANDROID_PLATFORM="${ANDROID_PLATFORM:-android-26}" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_INSTALL_PREFIX="${mbedtls_install_dir}" \
        -DENABLE_PROGRAMS=OFF \
        -DENABLE_TESTING=OFF \
        -DMBEDTLS_FATAL_WARNINGS=OFF \
        -DUSE_STATIC_MBEDTLS_LIBRARY=ON \
        -DUSE_SHARED_MBEDTLS_LIBRARY=OFF \
        -DLINK_WITH_PTHREAD=ON \
        -DDISABLE_PACKAGE_CONFIG_AND_INSTALL=OFF

    cmake --build "${mbedtls_build_dir}" --target install --parallel
    [[ -f "${mbedtls_install_dir}/lib/libmbedtls.a" ]] || fail "Expected libmbedtls.a under ${mbedtls_install_dir}/lib"
    [[ -f "${mbedtls_install_dir}/lib/libmbedx509.a" ]] || fail "Expected libmbedx509.a under ${mbedtls_install_dir}/lib"
    [[ -f "${mbedtls_install_dir}/lib/libmbedcrypto.a" ]] || fail "Expected libmbedcrypto.a under ${mbedtls_install_dir}/lib"

    export MBEDTLS_ROOT_DIR="${mbedtls_install_dir}"
}

copy_shared_outputs() {
    local dist_dir="$1"

    [[ -d "${dist_dir}/data" ]] || fail "Expected FujiNet data directory at ${dist_dir}/data"
    [[ -d "${dist_dir}/SD" ]] || fail "Expected FujiNet SD directory at ${dist_dir}/SD"
    [[ -f "${dist_dir}/fnconfig.ini" ]] || fail "Expected FujiNet config at ${dist_dir}/fnconfig.ini"

    mkdir -p "${GENERATED_ASSET_ROOT}"
    cp -R "${dist_dir}/data" "${GENERATED_ASSET_ROOT}/data"
    cp -R "${dist_dir}/SD" "${GENERATED_ASSET_ROOT}/SD"
    cp "${dist_dir}/fnconfig.ini" "${GENERATED_ASSET_ROOT}/fnconfig.ini"
    printf '%s\n' "${UPSTREAM_COMMIT}" > "${GENERATED_ASSET_ROOT}/upstream-commit.txt"
}

copy_abi_output() {
    local abi="$1"
    local dist_dir="$2"
    local lib_output="${dist_dir}/libfujinet.so"

    [[ -f "${lib_output}" ]] || fail "Expected shared library at ${lib_output}"
    mkdir -p "${GENERATED_JNI_ROOT}/${abi}"
    cp "${lib_output}" "${GENERATED_JNI_ROOT}/${abi}/libfujinet.so"
}

build_fujinet_for_abi() {
    local abi="$1"

    rm -rf "${CLONE_DIR}/build"

    export ANDROID_NDK_HOME="${NDK_DIR}"
    export ANDROID_ABI="${abi}"
    export ANDROID_PLATFORM="${ANDROID_PLATFORM:-android-26}"
    ANDROID_ABI_VALUE="${abi}"

    build_mbedtls

    (
        cd "${CLONE_DIR}"
        bash ./build.sh -cp ATARI
    )
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --abi)
            [[ $# -ge 2 ]] || fail "--abi requires a value"
            REQUESTED_ABIS+=("$2")
            shift 2
            ;;
        --all-abis)
            BUILD_ALL_ABIS=1
            shift
            ;;
        --refresh)
            REFRESH_CLONE=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            fail "Unknown argument: $1"
            ;;
    esac
done

if [[ "${BUILD_ALL_ABIS}" -eq 1 && "${#REQUESTED_ABIS[@]}" -gt 0 ]]; then
    fail "Use --all-abis or --abi, not both"
fi

mapfile -t ANDROID_ABIS_TO_BUILD < <(resolve_requested_abis)

SDK_DIR=$(read_sdk_dir)
NDK_DIR="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-$(find_latest_ndk "${SDK_DIR}")}}"
TOOLCHAIN_FILE="${NDK_DIR}/build/cmake/android.toolchain.cmake"
[[ -f "${TOOLCHAIN_FILE}" ]] || fail "Android toolchain file not found at ${TOOLCHAIN_FILE}"

mkdir -p "${WORK_ROOT}"
prepare_fresh_clone

rm -rf "${CLONE_DIR}/build"
apply_android_patches
apply_local_patch_files

rm -rf "${GENERATED_ASSET_ROOT}" "${GENERATED_JNI_ROOT}"
mkdir -p "${GENERATED_JNI_ROOT}"

for index in "${!ANDROID_ABIS_TO_BUILD[@]}"; do
    abi="${ANDROID_ABIS_TO_BUILD[${index}]}"
    build_fujinet_for_abi "${abi}"

    DIST_DIR="${CLONE_DIR}/build/dist"
    if [[ "${index}" == "0" ]]; then
        copy_shared_outputs "${DIST_DIR}"
    fi
    copy_abi_output "${abi}" "${DIST_DIR}"
done

printf '%s\n' "${ANDROID_ABIS_TO_BUILD[@]}" > "${GENERATED_ASSET_ROOT}/android-abis.txt"

echo "FujiNet Android contract outputs updated:"
echo "  assets: ${GENERATED_ASSET_ROOT}"
for abi in "${ANDROID_ABIS_TO_BUILD[@]}"; do
    echo "  jniLibs: ${GENERATED_JNI_ROOT}/${abi}/libfujinet.so"
done
