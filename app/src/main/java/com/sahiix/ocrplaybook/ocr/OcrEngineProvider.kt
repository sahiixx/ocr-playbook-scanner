package com.sahiix.ocrplaybook.ocr

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt binding for the app's single OcrEngine.
 *
 * Default: the on-device ML Kit engine — bundled model, zero setup, works
 * offline and in CI. To experiment with the local Tesseract engine, swap the
 * provider parameter below from `engine: MlKitOcrEngine` to
 * `engine: TesseractLocalEngine`; that engine needs eng.traineddata in
 * <filesDir>/tessdata/ or bundled at assets/tessdata/eng.traineddata
 * (see TesseractLocalEngine.ensureTrainedData()).
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrEngineProvider {

    @Provides @Singleton
    fun provideOcrEngine(engine: MlKitOcrEngine): OcrEngine = engine
}