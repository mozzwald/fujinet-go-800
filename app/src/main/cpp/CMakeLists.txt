cmake_minimum_required(VERSION 3.22.1)

project("atari800emu")

option(FUJI800_ENABLE_ATARI_DEBUG "Enable Atari800 DEBUG logging" OFF)

set(ATARI800_GENERATED_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../cpp-generated/atari800")
set(ATARI800_ANDROID_SUPPORT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/atari800_support")

if(NOT EXISTS "${ATARI800_GENERATED_DIR}/atari.c")
    message(FATAL_ERROR "Generated Atari800 source tree is missing. Run tools/atari800/build-atari800-source.sh before configuring CMake.")
endif()

set(ATARI800_CORE_SOURCES
        af80.c
        afile.c
        antic.c
        artifact.c
        atari.c
        binload.c
        bit3.c
        cartridge.c
        cartridge_info.c
        cassette.c
        cfg.c
        colours.c
        colours_external.c
        colours_ntsc.c
        colours_pal.c
        compfile.c
        cpu.c
        crc32.c
        devices.c
        cycle_map.c
        esc.c
        filter_ntsc.c
        gtia.c
        ide.c
        img_tape.c
        input.c
        log.c
        memory.c
        monitor.c
        mzpokeysnd.c
        netsio.c
        pal_blending.c
        pbi.c
        pbi_bb.c
        pbi_mio.c
        pbi_proto80.c
        pbi_scsi.c
        pbi_xld.c
        pia.c
        pokey.c
        pokeyrec.c
        pokeysnd.c
        rdevice.c
        remez.c
        roms/altirra_5200_charset.c
        roms/altirra_5200_os.c
        roms/altirra_basic.c
        roms/altirraos_800.c
        roms/altirraos_xl.c
        rtime.c
        screen.c
        sio.c
        sound.c
        statesav.c
        sysrom.c
        util.c
        videomode.c
        voicebox.c
        votrax.c
        votraxsnd.c
        xep80.c
        xep80_fonts.c
        atari_ntsc/atari_ntsc.c
)

list(TRANSFORM ATARI800_CORE_SOURCES PREPEND "${ATARI800_GENERATED_DIR}/")

# ARM-specific toggles (no-ops for x86/x86_64)
if(ANDROID_ABI STREQUAL "arm64-v8a" OR ANDROID_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_ANDROID_ARM_MODE ON)
    set(CMAKE_ANDROID_ARM_NEON ON)
endif()

# 16K page size support for newer NDKs on ARM targets (lld expects -z,max-page-size)
if((ANDROID_ABI STREQUAL "arm64-v8a" OR ANDROID_ABI STREQUAL "armeabi-v7a")
        AND (NOT DEFINED CMAKE_ANDROID_NDK_VERSION OR CMAKE_ANDROID_NDK_VERSION VERSION_GREATER_EQUAL 26))
    add_link_options(-Wl,-z,max-page-size=16384)
endif()

add_library(
        ataricore
        SHARED
        atari_log_sink.cpp
        atari_core.cpp
        fujinet_android.cpp
        session_runtime.cpp
        ${ATARI800_CORE_SOURCES}
        ${ATARI800_ANDROID_SUPPORT_DIR}/platform_stub.c
        ${ATARI800_ANDROID_SUPPORT_DIR}/codecs_stub.c
        ${ATARI800_ANDROID_SUPPORT_DIR}/file_export_stub.c
        ${ATARI800_ANDROID_SUPPORT_DIR}/ui_stub.c
)

find_library(
        log-lib
        log
)

find_library(
        z-lib
        z
)

find_library(
        dl-lib
        dl
)

target_include_directories(ataricore PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${ATARI800_ANDROID_SUPPORT_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/../cpp-generated
        ${ATARI800_GENERATED_DIR}
        ${ATARI800_GENERATED_DIR}/atari_ntsc
)

if(FUJI800_ENABLE_ATARI_DEBUG)
    target_compile_definitions(ataricore PRIVATE DEBUG)
endif()

target_link_libraries(
        ataricore
        ${log-lib}
        ${dl-lib}
        ${z-lib}
)
