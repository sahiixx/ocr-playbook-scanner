package com.sahiix.ocrplaybook.util

/** App-wide constants. Keep in one place to avoid magic numbers. */
object Constants {
    const val DB_NAME = "ocr_playbook.db"
    const val REPORTS_DIR = "reports"
    const val IMAGES_DIR = "images"

    /** Downscale so the long edge never exceeds this (memory guard). */
    const val MAX_IMAGE_EDGE = 2048

    /** JPEG quality for persisted scan images (0-100). */
    const val SCAN_JPEG_QUALITY = 85

    /** Realtime analyzer: minimum ms between OCR passes. */
    const val REALTIME_THROTTLE_MS = 1200L

    /** Realtime analyzer: input resolution (smaller = faster + less RAM). */
    const val REALTIME_TARGET_WIDTH = 1280

    /** Confidence threshold below which a block is flagged "review". */
    const val LOW_CONFIDENCE_THRESHOLD = 0.6f

    const val FILEPROVIDER_SUFFIX = ".fileprovider"
}
