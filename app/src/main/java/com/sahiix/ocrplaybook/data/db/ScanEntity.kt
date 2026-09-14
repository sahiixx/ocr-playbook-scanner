package com.sahiix.ocrplaybook.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One persisted scan: source image + OCR text + derived playbook. */
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null,       // internal files dir copy (nullable for text-only imports)
    val fullText: String = "",
    val wordCount: Int = 0,
    val lineCount: Int = 0,
    val meanConfidence: Float = -1f,
    val processingMs: Long = 0L,
    val engine: String = "mlkit-on-device",
    val title: String = "",              // auto title = first non-empty line (<=60ch)
    val tags: String = "",               // comma-separated, user-editable later
    val favorite: Boolean = false
)
