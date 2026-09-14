package com.sahiix.ocrplaybook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sahiix.ocrplaybook.util.Constants.DB_NAME

@Database(entities = [ScanEntity::class, OcrBlockEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, DB_NAME
                ).fallbackToDestructiveMigration(false).build().also { INSTANCE = it }
            }
    }
}
