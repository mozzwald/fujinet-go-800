#pragma once

#ifdef __cplusplus
extern "C" {
#endif

void AtariLogSink_StartCapture(const char* runtime_root_path);
void AtariLogSink_StopCapture(void);
void AtariLogSink_Write(const char* text);

#ifdef __cplusplus
}
#endif
