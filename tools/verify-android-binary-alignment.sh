#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
NDK_DIR="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
BUNDLETOOL_JAR="${BUNDLETOOL_JAR:-}"
APKS=()
BUNDLES=()
TEMP_ROOT=""

fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'EOF'
Usage: tools/verify-android-binary-alignment.sh [options]

Options:
  --apk PATH          Verify a release APK. May be repeated.
  --bundle PATH       Generate and verify a universal APK from an AAB. May be repeated.
  --bundletool PATH   Path to a bundletool-all JAR; required with --bundle.
  --sdk-dir PATH      Android SDK directory (defaults to environment/local.properties).
  --ndk-dir PATH      Android NDK directory (defaults to environment/Gradle's pinned NDK).
  -h, --help          Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --apk)
            [[ $# -ge 2 ]] || fail "--apk requires a path"
            APKS+=("$2")
            shift 2
            ;;
        --bundle)
            [[ $# -ge 2 ]] || fail "--bundle requires a path"
            BUNDLES+=("$2")
            shift 2
            ;;
        --bundletool)
            [[ $# -ge 2 ]] || fail "--bundletool requires a path"
            BUNDLETOOL_JAR="$2"
            shift 2
            ;;
        --sdk-dir)
            [[ $# -ge 2 ]] || fail "--sdk-dir requires a path"
            SDK_DIR="$2"
            shift 2
            ;;
        --ndk-dir)
            [[ $# -ge 2 ]] || fail "--ndk-dir requires a path"
            NDK_DIR="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            fail "unknown argument: $1"
            ;;
    esac
done

[[ ${#APKS[@]} -gt 0 || ${#BUNDLES[@]} -gt 0 ]] || fail "provide at least one --apk or --bundle"

if [[ -z "${SDK_DIR}" ]]; then
    SDK_DIR=$(sed -n 's/^sdk.dir=//p' "${PROJECT_ROOT}/local.properties" | tail -n 1)
fi
[[ -d "${SDK_DIR}" ]] || fail "Android SDK directory not found: ${SDK_DIR}"

if [[ -z "${NDK_DIR}" ]]; then
    PINNED_NDK_VERSION=$(sed -n 's/^val pinnedAndroidNdkVersion = "\([^"]*\)"/\1/p' \
        "${PROJECT_ROOT}/app/build.gradle.kts" | head -n 1)
    [[ -n "${PINNED_NDK_VERSION}" ]] || fail "unable to read pinnedAndroidNdkVersion"
    NDK_DIR="${SDK_DIR}/ndk/${PINNED_NDK_VERSION}"
fi

READELF="${NDK_DIR}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
[[ -x "${READELF}" ]] || fail "llvm-readelf not found in NDK: ${NDK_DIR}"

ZIPALIGN=$(find "${SDK_DIR}/build-tools" -mindepth 2 -maxdepth 2 -type f -name zipalign \
    -printf '%h\n' | sort -V | tail -n 1)/zipalign
[[ -x "${ZIPALIGN}" ]] || fail "zipalign not found under ${SDK_DIR}/build-tools"

TEMP_ROOT=$(mktemp -d)
trap 'rm -rf "${TEMP_ROOT}"' EXIT

verify_apk() {
    local apk="$1"
    local label="$2"
    local extraction_dir="${TEMP_ROOT}/$(printf '%s' "${label}" | tr '/ ' '__')"
    local load_count

    [[ -f "${apk}" ]] || fail "APK not found: ${apk}"
    "${ZIPALIGN}" -c -P 16 4 "${apk}" >/dev/null || fail "16 KB ZIP alignment failed: ${label}"

    mkdir -p "${extraction_dir}"
    unzip -q "${apk}" 'lib/*/*.so' -d "${extraction_dir}"
    mapfile -t native_libraries < <(
        find "${extraction_dir}/lib" -type f -name '*.so' \
            \( -path '*/arm64-v8a/*' -o -path '*/x86_64/*' \) | sort
    )
    [[ ${#native_libraries[@]} -gt 0 ]] || fail "no packaged 64-bit native libraries found: ${label}"

    for library in "${native_libraries[@]}"; do
        load_count=0
        while IFS= read -r alignment; do
            [[ "${alignment}" =~ ^0x[0-9a-fA-F]+$ ]] || fail "invalid ELF alignment '${alignment}' in ${library}"
            ((load_count += 1))
            if (( alignment < 0x4000 )); then
                fail "ELF LOAD alignment below 16 KB in ${library}: ${alignment}"
            fi
        done < <("${READELF}" -lW "${library}" | awk '$1 == "LOAD" { print $NF }')
        [[ ${load_count} -gt 0 ]] || fail "no ELF LOAD segments found in ${library}"
    done

    printf 'verified: %s (%d 64-bit native libraries)\n' "${label}" "${#native_libraries[@]}"
}

for apk in "${APKS[@]}"; do
    verify_apk "${apk}" "${apk}"
done

if [[ ${#BUNDLES[@]} -gt 0 ]]; then
    [[ -f "${BUNDLETOOL_JAR}" ]] || fail "--bundle requires a bundletool-all JAR"
    for bundle in "${BUNDLES[@]}"; do
        [[ -f "${bundle}" ]] || fail "AAB not found: ${bundle}"
        bundle_name=$(basename "${bundle}" .aab)
        apk_set="${TEMP_ROOT}/${bundle_name}.apks"
        universal_dir="${TEMP_ROOT}/${bundle_name}-universal"
        java -jar "${BUNDLETOOL_JAR}" build-apks \
            --bundle="${bundle}" \
            --output="${apk_set}" \
            --mode=universal \
            --overwrite >/dev/null
        mkdir -p "${universal_dir}"
        unzip -q "${apk_set}" universal.apk -d "${universal_dir}"
        verify_apk "${universal_dir}/universal.apk" "${bundle} (bundletool universal APK)"
    done
fi
