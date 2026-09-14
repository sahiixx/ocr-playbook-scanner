package com.sahiix.ocrplaybook.di

import android.content.Context
import com.sahiix.ocrplaybook.data.db.AppDatabase
import com.sahiix.ocrplaybook.data.db.ScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase = AppDatabase.get(ctx)

    @Provides @Singleton
    fun provideDao(db: AppDatabase): ScanDao = db.scanDao()
}
