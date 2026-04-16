# Compliance Notes

This repository distributes source code for an Android application that incorporates and modifies GPL-covered upstream projects.

This file is a practical map of the corresponding-source story for this repo. It is not legal advice.

## Overview

The app is built from:

- original Android/Kotlin/C++ integration code in this repository
- a pinned Atari800 source tree fetched during build setup
- a pinned FujiNet firmware source tree fetched during build setup
- local patches and Android-specific support code applied on top of those upstreams

The generated source and runtime artifacts are build outputs, not the preferred form for modification. The preferred form lives in this repository as:

- the app source
- the patch files
- the Android support files
- the scripts that fetch, patch, and stage the upstream source trees

## Atari800 Corresponding Source

Atari800 is fetched by:

- [`tools/atari800/build-atari800-source.sh`](./tools/atari800/build-atari800-source.sh)

Pinned upstream:

- upstream repo: `https://github.com/mozzwald/atari800`
- branch: `netsio-feb2026`
- commit: `a240a3e02ea7110426b59569f7d87633e9082edd`

Local modifications are applied from:

- [`tools/atari800/patches/`](./tools/atari800/patches/)

The staged generated copy used by the Android native build is written to:

- `app/src/main/cpp-generated/atari800`

That generated directory should be treated as a build artifact. The source of truth is the pinned upstream plus the local patch set and build script above.

## FujiNet Corresponding Source

FujiNet firmware is fetched and built by:

- [`tools/fujinet/build-fujinet.sh`](./tools/fujinet/build-fujinet.sh)

Pinned upstream:

- upstream repo: `https://github.com/mozzwald/fujinet-firmware`
- branch: `android`
- commit: `6d8d610f37e403dea8e8f88f8ab0fda0283a06eb`

Local modifications are represented by:

- explicit patch files in [`tools/fujinet/patches/`](./tools/fujinet/patches/)
- Android support code in [`tools/fujinet/support/`](./tools/fujinet/support/)
- scripted source edits performed directly by [`tools/fujinet/build-fujinet.sh`](./tools/fujinet/build-fujinet.sh)

The script also pulls a pinned Mbed TLS revision for the Android FujiNet runtime build:

- upstream repo: `https://github.com/Mbed-TLS/mbedtls.git`
- tag: `mbedtls-3.6.5`
- commit: `952b8e597b4e239a7cd592d19a2579e7fc67d21e`

The generated runtime artifacts are written to:

- `app/src/main/assets-generated/fujinet`
- `app/src/main/jniLibs-generated`

Those generated directories are build outputs. The corresponding source is the pinned upstream plus the patch/support/scripted modifications in this repository.

## App-Side Integration Code

This repository also contains original Android-side integration code, including:

- Kotlin application code under `app/src/main/java/`
- native bridge and runtime host code under `app/src/main/cpp/`
- Android resources and tests under `app/src/main/res/`, `app/src/test/`, and `app/src/androidTest/`

That code is part of the distributed source for this repository.

## Other Upstream Notices

Some third-party code and notices may be present inside the fetched upstream trees rather than re-copied into this repository.

Examples may include:

- Atari800 subcomponents and bundled notices
- ROM replacement code or assets referenced by Atari800 upstream, such as Altirra-derived components when included by upstream

Those notices should be read from the pinned upstream source trees themselves. This repository intentionally keeps the source-of-truth relationship aligned with those upstream projects instead of copying all of their internal notice files into the top level here.

## Release Practice

When distributing binaries built from this repository, the recommended practice is:

- publish the source repository revision used for the release
- keep the Atari800 and FujiNet patch/support/build scripts in the same revision
- keep the pinned upstream commit ids visible in the build scripts
- avoid treating generated source/runtime directories as the canonical editable source

## Related Files

- [`LICENSE`](./LICENSE)
- [`THIRD_PARTY_NOTICES.md`](./THIRD_PARTY_NOTICES.md)
- [`tools/atari800/build-atari800-source.sh`](./tools/atari800/build-atari800-source.sh)
- [`tools/fujinet/build-fujinet.sh`](./tools/fujinet/build-fujinet.sh)
