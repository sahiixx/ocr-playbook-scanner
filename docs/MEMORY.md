# Memory Optimization — On-Device OCR Design Notes

This app targets the whole Android 8.0+ spectrum, including 2–4 GB RAM
devices. Every decision below exists to keep peak RSS bounded.

## 1. Decode-time downsampling (`BitmapUtils.decodeSampled`)

Modern phone cameras produce 12–48 MP stills. A full 12 MP ARGB_8888 decode
= **48 MB**; a 48 MP one = ~192 MB — that alone can OOM.

- **Pass 1**: `BitmapFactory.Options.inJustDecodeBounds = true` reads only
  the header (w/h) — zero pixel allocation.
- **Pass 2**: `inSampleSize` (power of two) shrinks so the **long edge
  ≤ 2048 px** (≤ 1280 px on `ActivityManager.isLowRamDevice`).
  2048×1536 RGB_565 ≈ **6 MB** per scan — a 8–32× reduction over raw.
- All decode paths (gallery Uri, SAF file, camera capture) route through it.

## 2. RGB_565 for OCR

Arguably the single biggest win: `inPreferredConfig = RGB_565` halves RAM
(2 bytes/px instead of 4). OCR only needs luminance edges — alpha is
irrelevant. Also slightly faster to process.

## 3. Bitmap recycling discipline

- `MlKitOcrEngine.recognize` downscales the input, recognizes the
  copy, then **recycles the copy** in a `finally`.
- `ScanRepository.scanUri` decodes → scans → recycles the source.
- `RealtimeOcrAnalyzer` releases the `ImageProxy` immediately after copying
  to a bitmap (so the camera buffer pool never stalls), then recycles the
  bitmap when the coroutine finishes.

## 4. Throttled realtime pipeline

OCR every frame at 30 fps would saturate the CPU and queue gigabytes of
unread frames.

- `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` — CameraX drops stale frames.
- Software throttle: at most **one recognition per 1.2 s** (`busy` flag +
  elapsed-realtime check); `isWorking` drives the spinner UI.
- Inputs capped at **1280 px wide**; the engine's `scaleForRealtime` is a
  lightweight `createScaledBitmap` before ML Kit sees the frame.
- OCR runs on `Dispatchers.Default`, never the main thread.

## 5. Disk / DB footprint

- Persisted scans are **downscaled JPEG @ quality 85** — single-digit MB per
  scan instead of tens of MB.
- Room stores text blocks; a full page of OCR text is roughly 5–20 KB.
- Histories stay local; deleting a scan also deletes its image file.

## 6. No network, no analytics SDKs

ML Kit Text Recognition v2 ships a bundled (non-downloadable, on-device)
model. There is no network upload, no account dependency, and no extra
heap-mountain from analytics/KPI SDKs.

## 7. Watching memory in practice

```bash
# Per app while scanning:
adb shell dumpsys meminfo com.sahiix.ocrplaybook | grep -A5 TOTAL
# Any OOM would come back as:
adb logcat -d | grep -i "OutOfMemory\|lowmemorykiller"
```

## 8. Pluggable OCR engines

- Recognition goes through an `OcrEngine` interface; the default ML Kit
  engine ships a bundled model (no data files on disk), while the optional
  Tesseract engine (tess-two) loads native libraries plus an `eng.traineddata`
  file into the process — weigh per low-RAM device.
- tess-two allocates image buffers on the **native** heap (outside the
  JVM/GC): the wrapper's `clear()` after each recognition is what frees a
  frame's pixels, whereas the ML Kit path recycles Kotlin bitmaps explicitly.
- `TesseractLocalEngine` serializes all native calls on an internal lock and
  runs on `Dispatchers.Default`; `eng.traineddata` lives on disk under
  `<filesDir>/tessdata/` so it never counts against Java heap.