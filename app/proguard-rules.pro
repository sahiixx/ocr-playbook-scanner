# Keep OCR / Camera / Room rules tight for release shrinking
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.sahiix.ocrplaybook.data.db.** { *; }
