/*
 * file_export_stub.c - Disable multimedia recording/export for Android build.
 */
#include "config.h"
#include "file_export.h"

int File_Export_Initialise(int *argc, char *argv[]) { (void)argc; (void)argv; return 1; }
int File_Export_ReadConfig(char *string, char *ptr) { (void)string; (void)ptr; return 1; }
void File_Export_WriteConfig(FILE *fp) { (void)fp; }

int File_Export_StopRecording(void) { return 0; }
int File_Export_StartRecording(const char *fileName) { (void)fileName; return 0; }
int File_Export_GetRecordingStats(int *seconds, int *size, char **media_type) {
    (void)seconds; (void)size; (void)media_type; return 0;
}

int File_Export_WriteAudio(const UBYTE *samples, int num_samples) {
    (void)samples; (void)num_samples; return 0;
}
int File_Export_WriteVideo(void) { return 0; }

int File_Export_ImageTypeSupported(const char *id) { (void)id; return FALSE; }
int File_Export_SaveScreen(const char *filename, UBYTE *ptr1, UBYTE *ptr2) {
    (void)filename; (void)ptr1; (void)ptr2; return 0;
}
