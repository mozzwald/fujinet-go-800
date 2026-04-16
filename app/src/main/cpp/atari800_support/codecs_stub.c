/*
 * codecs_stub.c - Disable multimedia container/codecs for Android core build.
 */

#include "config.h"

/* Stubs for file_export expectations */
int CODECS_AUDIO_Initialise(void) { return 0; }
int CODECS_VIDEO_Initialise(void) { return 0; }
void CODECS_AUDIO_ReadConfig(void) {}
void CODECS_VIDEO_ReadConfig(void) {}
void CODECS_AUDIO_WriteConfig(void) {}
void CODECS_VIDEO_WriteConfig(void) {}

/* Dummy container handles */
typedef struct container_s { int dummy; } container_t;
container_t *container = 0;
void *audio_codec = 0;
void *video_codec = 0;

void CONTAINER_Close(container_t *c) { (void)c; }
container_t *CONTAINER_Open(const char *filename, int type) {
    (void)filename;
    (void)type;
    return 0;
}
int CONTAINER_AddAudioSamples(container_t *c, const void *samples, unsigned int count) {
    (void)c; (void)samples; (void)count;
    return 0;
}
