# FujiNet Go 800

Android Atari 8-bit emulation with FujiNet integration.

This repository contains:

- an Android UI and app shell
- native Android glue for the emulator/runtime integration
- build scripts that fetch, patch, and stage pinned upstream source trees for `atari800` and `fujinet-firmware`

The shipped app is built from a mix of original project code plus modified upstream GPL-covered components. See [COMPLIANCE.md](./COMPLIANCE.md) and [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md).

## Current Upstreams

- Atari800
  - upstream repo: `https://github.com/mozzwald/atari800`
  - pinned branch: `netsio-feb2026`
  - pinned commit: `a240a3e02ea7110426b59569f7d87633e9082edd`
- FujiNet firmware
  - upstream repo: `https://github.com/mozzwald/fujinet-firmware`
  - pinned branch: `android`
  - pinned commit: `6d8d610f37e403dea8e8f88f8ab0fda0283a06eb`

## Build Requirements

You need a normal Android native build environment:

- JDK 11
- Android SDK
- Android NDK installed under the SDK
- `bash`
- `git`
- `python3`
- `patch`
- `wget` or `curl`

The FujiNet runtime build also pulls and builds Mbed TLS during setup.

## Android SDK Setup

The build scripts resolve the Android SDK from either:

- `ANDROID_SDK_ROOT`, or
- `local.properties` with `sdk.dir=...`

## Build

The Gradle build wires the source/runtime preparation steps into the app build, but the underlying scripts are:

- `bash tools/atari800/build-atari800-source.sh`
- `bash tools/fujinet/build-fujinet.sh --all-abis`

Typical debug build:

```bash
./gradlew assembleDebug
```

Typical release build:

```bash
./gradlew assembleRelease
```

If you sign release builds locally, create a `keystore.properties` file outside version control. The app build reads that file if present, but it is optional for normal source builds.

## Generated Directories

These are generated during the build and should not be committed:

- `app/src/main/cpp-generated/`
- `app/src/main/assets-generated/`
- `app/src/main/jniLibs-generated/`
- `tools/atari800/work/`
- `tools/fujinet/work/`

## Repository Layout

- `app/`
  - Android app code, Compose UI, JNI bridge, native host code, tests
- `tools/atari800/`
  - Atari800 source staging script and local patches
- `tools/fujinet/`
  - FujiNet runtime build script, support code, and local patches
- `CHANGELOG.md`
  - release notes/history

## License

The repository root license is GPLv3. Third-party components remain under their own licenses and notices. See [LICENSE](./LICENSE), [COMPLIANCE.md](./COMPLIANCE.md), and [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md).
