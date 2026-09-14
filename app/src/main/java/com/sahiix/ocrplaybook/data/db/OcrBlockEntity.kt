package com.sahiix.ocrplaybook.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One OCR text block belonging to a scan (for detail view + report fidelity). */
@Entity(
    tableName = "ocr_blocks",
    foreignKeys = [ForeignKey(
        entity = ScanEntity::class,
        parentColumns = ["id"],
        childColumns = ["scanId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("scanId")]
)
data class OcrBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanId: Long = 0,
    val position: Int = 0,
    val text: String = "",
    val confidence: Float = -1f,
    val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0,
    val languages: String = ""
)
