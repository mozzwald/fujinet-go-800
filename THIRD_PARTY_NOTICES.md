# Third-Party Notices

This repository depends on third-party open source software. The list below is a practical notice summary for the major upstream components directly used by the build.

## Atari800

- project: Atari800
- upstream repo: `https://github.com/mozzwald/atari800`
- pinned branch used by this repo: `netsio-feb2026`
- pinned commit used by this repo: `a240a3e02ea7110426b59569f7d87633e9082edd`
- local integration files:
  - [`tools/atari800/build-atari800-source.sh`](./tools/atari800/build-atari800-source.sh)
  - [`tools/atari800/patches/`](./tools/atari800/patches/)

The Atari800 upstream source tree includes its own copyright and license notices. In the original tree, the main license text is provided in `COPYING`.

This repository applies local patches to the fetched Atari800 source before staging it for the Android build.

## FujiNet Firmware

- project: FujiNet firmware
- upstream repo: `https://github.com/mozzwald/fujinet-firmware`
- pinned branch used by this repo: `android`
- pinned commit used by this repo: `6d8d610f37e403dea8e8f88f8ab0fda0283a06eb`
- local integration files:
  - [`tools/fujinet/build-fujinet.sh`](./tools/fujinet/build-fujinet.sh)
  - [`tools/fujinet/patches/`](./tools/fujinet/patches/)
  - [`tools/fujinet/support/`](./tools/fujinet/support/)

The FujiNet upstream source tree includes its own copyright and license notices. In the original tree, the main license text is provided in `LICENSE`.

This repository applies both explicit patch files and Android-specific scripted modifications during the build process.

## Mbed TLS

- project: Mbed TLS
- upstream repo: `https://github.com/Mbed-TLS/mbedtls.git`
- pinned tag used by this repo: `mbedtls-3.6.5`
- pinned commit used by this repo: `952b8e597b4e239a7cd592d19a2579e7fc67d21e`

Mbed TLS is pulled by the FujiNet runtime build script as part of the Android runtime build flow.

## Notes On Additional Notices

The fetched upstream source trees may themselves contain additional bundled components, notices, or attribution files.

Examples may include:

- Atari800 bundled notices and subcomponent attributions
- replacement ROM or auxiliary component notices distributed by Atari800 upstream
- any upstream-internal third-party notices carried by FujiNet firmware

Those notices should be read from the fetched upstream source trees associated with the pinned revisions above.

## Source And Modifications

For how these upstream components are fetched, modified, and incorporated into the Android build, see:

- [`README.md`](./README.md)
- [`COMPLIANCE.md`](./COMPLIANCE.md)
