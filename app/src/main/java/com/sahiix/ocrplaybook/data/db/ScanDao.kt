package com.sahiix.ocrplaybook.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<OcrBlockEntity>)

    @Transaction
    suspend fun insertScanWithBlocks(scan: ScanEntity, blocks: List<OcrBlockEntity>): Long {
        val id = insertScan(scan)
        insertBlocks(blocks.map { it.copy(scanId = id) })
        return id
    }

    @Update suspend fun updateScan(scan: ScanEntity)

    @Query("DELETE FROM scans WHERE id = :id") suspend fun deleteScan(id: Long)

    @Query("SELECT * FROM scans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id") suspend fun getById(id: Long): ScanEntity?

    @Query("SELECT * FROM ocr_blocks WHERE scanId = :scanId ORDER BY position ASC")
    suspend fun blocksFor(scanId: Long): List<OcrBlockEntity>

    @Query("SELECT * FROM scans WHERE favorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE fullText LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%' ORDER BY createdAt DESC")
    fun search(q: String): Flow<List<ScanEntity>>

    @Query("SELECT COUNT(*) FROM scans") suspend fun count(): Int
}
