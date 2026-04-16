package com.mantismoonlabs.fujinetgo800.storage

enum class MediaKind {
    DISK,
    CARTRIDGE,
    EXECUTABLE,
    ROM,
}

enum class MediaRole {
    DISK,
    CARTRIDGE,
    EXECUTABLE,
    ROM,
}

data class MediaSelection(
    val mediaKind: MediaKind,
    val uriString: String,
    val importedPath: String,
    val displayName: String,
    val lastUpdatedEpochMillis: Long,
)
