#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
PROJECT_ROOT=$(cd -- "${SCRIPT_DIR}/../.." &>/dev/null && pwd)
WORK_ROOT="${SCRIPT_DIR}/work"
PATCH_DIR_RELATIVE="tools/atari800/patches"
PATCH_DIR="${PROJECT_ROOT}/${PATCH_DIR_RELATIVE}"
CLONE_DIR="${WORK_ROOT}/atari800"
GENERATED_SOURCE_ROOT="${PROJECT_ROOT}/app/src/main/cpp-generated/atari800"
REQUIRED_PATCHES=(
    "0001-increase-netsio-startup-timeout.patch"
    "0002-add-android-atari-log-file-hook.patch"
)

UPSTREAM_URL="https://github.com/mozzwald/atari800"
UPSTREAM_BRANCH="netsio-feb2026"
UPSTREAM_COMMIT="a240a3e02ea7110426b59569f7d87633e9082edd"
ARCHIVE_PATH="${WORK_ROOT}/atari800-${UPSTREAM_COMMIT}.tar.gz"
STAMP_PATH="${GENERATED_SOURCE_ROOT}/.source-info"

REFRESH_CLONE=0

usage() {
    cat <<'EOF'
Usage: bash tools/atari800/build-atari800-source.sh [--refresh]

Fetches the pinned Atari800 source tree from GitHub and stages upstream src/
into app/src/main/cpp-generated/atari800 for the Android native build.
EOF
}

fail() {
    echo "build-atari800-source.sh: $*" >&2
    exit 1
}

patch_fingerprint() {
    local patches=("${PATCH_DIR}"/*.patch)
    if [[ ! -d "${PATCH_DIR}" ]] || [[ ${#patches[@]} -eq 0 ]]; then
        echo "none"
        return
    fi

    sha256sum "${patches[@]}" | sha256sum | awk '{ print $1 }'
}

download_archive() {
    local archive_url="https://codeload.github.com/mozzwald/atari800/tar.gz/${UPSTREAM_COMMIT}"

    if [[ "${REFRESH_CLONE}" -eq 1 ]]; then
        rm -f "${ARCHIVE_PATH}"
    fi

    if [[ -f "${ARCHIVE_PATH}" ]] && gzip -t "${ARCHIVE_PATH}" >/dev/null 2>&1; then
        return
    fi

    mkdir -p "${WORK_ROOT}"
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
    download_archive

    local archive_root
    archive_root=$(tar -tzf "${ARCHIVE_PATH}" | sed -n '1p' | cut -d/ -f1)
    [[ -n "${archive_root}" ]] || fail "Unable to determine extracted archive root"

    tar -xzf "${ARCHIVE_PATH}" -C "${WORK_ROOT}"
    mv "${WORK_ROOT}/${archive_root}" "${CLONE_DIR}"
}

apply_patches() {
    if [[ ! -d "${PATCH_DIR}" ]]; then
        return
    fi

    local required_patch
    for required_patch in "${REQUIRED_PATCHES[@]}"; do
        [[ -f "${PATCH_DIR}/${required_patch}" ]] || fail "Missing required patch: ${required_patch}"
    done

    shopt -s nullglob
    local patch
    for patch in "${PATCH_DIR}"/*.patch; do
        patch -d "${CLONE_DIR}" -p1 < "${patch}"
    done
    shopt -u nullglob
}

stage_source_tree() {
    local patch_state
    patch_state=$(patch_fingerprint)

    rm -rf "${GENERATED_SOURCE_ROOT}"
    mkdir -p "${GENERATED_SOURCE_ROOT}"
    cp -R "${CLONE_DIR}/src/." "${GENERATED_SOURCE_ROOT}/"

    cat > "${STAMP_PATH}" <<EOF
upstream_url=${UPSTREAM_URL}
upstream_branch=${UPSTREAM_BRANCH}
upstream_commit=${UPSTREAM_COMMIT}
patch_fingerprint=${patch_state}
EOF
}

source_is_current() {
    local patch_state
    patch_state=$(patch_fingerprint)

    [[ "${REFRESH_CLONE}" -eq 0 ]] &&
    [[ -f "${STAMP_PATH}" ]] &&
    grep -q "^upstream_commit=${UPSTREAM_COMMIT}$" "${STAMP_PATH}" &&
    grep -q "^patch_fingerprint=${patch_state}$" "${STAMP_PATH}" &&
    [[ -f "${GENERATED_SOURCE_ROOT}/atari.c" ]]
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --refresh)
            REFRESH_CLONE=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            usage
            fail "Unknown argument: $1"
            ;;
    esac
done

if source_is_current; then
    exit 0
fi

prepare_fresh_clone
apply_patches
stage_source_tree
