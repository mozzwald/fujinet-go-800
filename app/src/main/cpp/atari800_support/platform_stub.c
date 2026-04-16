/*
 * platform_stub.c - Minimal platform hooks for Android build.
 * Provides dummy implementations expected by core without pulling desktop ports.
 */

#include "config.h"
#include "atari.h"
#include "colours.h"
#include "platform.h"
#include "videomode.h"
#include "sound.h"
#include "input.h"
#include <pthread.h>
#include <time.h>
#include <string.h>

static uint8_t g_stick_port[4] = {INPUT_STICK_CENTRE, INPUT_STICK_CENTRE, INPUT_STICK_CENTRE, INPUT_STICK_CENTRE};
static uint8_t g_trig_port[4] = {1, 1, 1, 1}; // active low: 0 pressed
static pthread_mutex_t g_sound_mutex = PTHREAD_MUTEX_INITIALIZER;

int PLATFORM_Initialise(int *argc, char *argv[]) {
    (void)argc;
    (void)argv;
    return TRUE;
}

int PLATFORM_Exit(int run_monitor) {
    (void)run_monitor;
    return 0;
}

int PLATFORM_Keyboard(void) { return 0; }
void PLATFORM_DisplayScreen(void) {}

int PLATFORM_PORT(int num) {
    // Pack two sticks per port group
    if (num == 0)
        return (g_stick_port[1] << 4) | (g_stick_port[0] & 0x0f);
    if (num == 1)
        return (g_stick_port[3] << 4) | (g_stick_port[2] & 0x0f);
    return 0xFF;
}
int PLATFORM_TRIG(int num) {
    if (num >= 0 && num < 4)
        return g_trig_port[num];
    return 1;
}

void PLATFORM_ConfigInit(void) {}
int PLATFORM_Configure(char *option, char *parameters) {
    (void)option;
    (void)parameters;
    return 0;
}
void PLATFORM_ConfigSave(FILE *fp) { (void)fp; }

void PLATFORM_PaletteUpdate(void) {}

void PLATFORM_Sleep(double s) {
    if (s <= 0.0) return;
    struct timespec ts;
    ts.tv_sec = (time_t)s;
    ts.tv_nsec = (long)((s - ts.tv_sec) * 1e9);
    nanosleep(&ts, NULL);
}

double PLATFORM_Time(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (double)ts.tv_sec + (double)ts.tv_nsec / 1e9;
}

int PLATFORM_SupportsVideomode(VIDEOMODE_MODE_t mode, int stretch, int rotate90) {
    (void)mode; (void)stretch; (void)rotate90;
    return TRUE;
}

void PLATFORM_SetVideoMode(VIDEOMODE_resolution_t const *res, int windowed, VIDEOMODE_MODE_t mode, int rotate90) {
    (void)res; (void)windowed; (void)mode; (void)rotate90;
}

VIDEOMODE_resolution_t *PLATFORM_AvailableResolutions(unsigned int *size) {
    // Report a single fixed resolution to satisfy VIDEOMODE init
    static VIDEOMODE_resolution_t res = {320, 240};
    if (size) *size = 1;
    return &res;
}

VIDEOMODE_resolution_t *PLATFORM_DesktopResolution(void) {
    static VIDEOMODE_resolution_t res = {320, 240};
    return &res;
}

int PLATFORM_WindowMaximised(void) { return FALSE; }

void PLATFORM_GetPixelFormat(PLATFORM_pixel_format_t *format) {
    if (!format) return;
    format->bpp = 32;
    format->rmask = 0x00FF0000;
    format->gmask = 0x0000FF00;
    format->bmask = 0x000000FF;
}

void PLATFORM_MapRGB(void *dest, int const *palette, int size) {
    if (!dest || !palette || size <= 0) return;
    ULONG *d = (ULONG *)dest;
    for (int i = 0; i < size; ++i) {
        d[i] = (ULONG)palette[i];
    }
}

int PLATFORM_SoundSetup(Sound_setup_t *setup) {
    if (setup == NULL) {
        return FALSE;
    }
    if (setup->freq < 8000) {
        setup->freq = 8000;
    }
    setup->sample_size = 2;
    setup->channels = 1;
    if (setup->buffer_ms == 0) {
        setup->buffer_ms = 40;
    }
    if (setup->buffer_frames == 0) {
        setup->buffer_frames = setup->freq * setup->buffer_ms / 1000;
    }
    return TRUE;
}

void PLATFORM_SoundExit(void) {}
void PLATFORM_SoundPause(void) {}
void PLATFORM_SoundContinue(void) {}
void PLATFORM_SoundLock(void) { pthread_mutex_lock(&g_sound_mutex); }
void PLATFORM_SoundUnlock(void) { pthread_mutex_unlock(&g_sound_mutex); }

int Atari_POT(int pot) { (void)pot; return 228; }

// Helpers to set joystick state from JNI
void PLATFORM_SetJoystick(int port, uint8_t stick_code, uint8_t trig_pressed) {
    if (port < 0 || port > 3) return;
    g_stick_port[port] = stick_code & 0x0f;
    g_trig_port[port] = trig_pressed ? 0 : 1; // pressed -> 0
}
