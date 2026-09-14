# OCR Playbook Scanner

On-device Android OCR app: point the camera (or pick a photo/file) at any
document, get the text instantly via **ML Kit on-device OCR** (offline, no
API key), and auto-generate a structured **Playbook report** (summary, key
points, action items, dates, contacts, keywords, full transcript) that
exports to **PDF** and **Markdown**.

![CI](https://github.com/sahiixx/ocr-playbook-scanner/actions/workflows/android.yml/badge.svg)

## Features

| Area | Details |
|---|---|
| Real-time scanning | CameraX preview + throttled ML Kit analyzer (1 pass / 1.2 s) with live text overlay |
| Photo / file import | Photo picker, Storage Access Framework (`image/*`), and `ACTION_SEND` from other apps |
| OCR engine | ML Kit Text Recognition v2 (Latin), 100% on-device. Swap in Chinese/Devanagari/Japanese/Korean recognizers via one-line Gradle changes |
| Playbook reports | Pure-Kotlin builder: summary, key points, action items, dates, emails/phones, keyword frequency, low-confidence flags, verbatim transcript |
| Export | Framework `PdfDocument` PDF writer (no heavy dep) + Markdown; share via FileProvider |
| Storage | Room (`scans` + `ocr_blocks`), search, favorites, delete; images as downscaled JPEGs in private storage |
| Memory design | `inSampleSize` downsampling, RGB_565 decode, 2048px cap (1280px on low-RAM), bitmap recycle after every pass, `STRATEGY_KEEP_ONLY_LATEST` |

## Project layout

```
ocr-playbook-scanner/
├── app/
│   ├── build.gradle.kts            # deps: Compose BOM, CameraX, ML Kit, Room, Hilt, Coil
│   └── src/main/java/com/sahiix/ocrplaybook/
│       ├── MainActivity.kt         # bottom-nav host (Scan / History / Report / Settings)
│       ├── OcrPlaybookApp.kt       # @HiltAndroidApp
│       ├── ocr/
│       │   ├── OcrResult.kt        # framework-free OCR model (testable on JVM)
│       │   ├── MlKitOcrEngine.kt   # on-device recognizer wrapper
│       │   └── RealtimeOcrAnalyzer.kt  # throttled CameraX ImageAnalysis
│       ├── data/db/                # Room: ScanEntity, OcrBlockEntity, ScanDao, AppDatabase
│       ├── data/repo/ScanRepository.kt # OCR + persist pipeline
│       ├── report/
│       │   ├── PlaybookBuilder.kt  # pure-Kotlin report logic (unit-tested)
│       │   ├── PdfReportWriter.kt  # framework PdfDocument renderer
│       │   └── ReportExporter.kt   # Markdown/PDF export + share intents
│       ├── viewmodel/              # Scanner / History / Report ViewModels (Hilt)
│       └── ui/screens/             # Scanner, CameraPreviewBox, History, Report, Settings
├── docs/BUILD.md                   # full build + deploy guide
├── docs/MEMORY.md                  # memory-optimization design notes
└── .github/workflows/android.yml   # CI: unit tests + debug APK artifact
```

## Quick start (5 minutes)

**Option A — cloud build (recommended on this machine):**

1. Push this folder to GitHub (see below).
2. Open the **Actions** tab → run **Android CI**.
3. Download the `ocr-playbook-debug-apk` artifact → install on your phone.

**Option B — local build (needs Android Studio or SDK + JDK 17):**

```bash
# 1. Install Android Studio (bundles SDK + JDK), or:
#    SDK cmdline-tools + platform-34 + build-tools 34.0.0, JDK 17
# 2. Point Gradle at your SDK:
cp local.properties.example local.properties   # then edit sdk.dir
# 3. Build:
gradle :app:assembleDebug
# APK lands at: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Full instructions (Linux/macOS/Windows, signing a release, Play Store
checklist, troubleshooting): **[docs/BUILD.md](docs/BUILD.md)**.

## Memory design

TL;DR: every bitmap is sampled down before allocation, decoded as RGB_565,
recycled after use, and the realtime path processes at most one small frame
per 1.2 s. Details: **[docs/MEMORY.md](docs/MEMORY.md)**.

## Privacy

No network calls, no analytics, no accounts. OCR runs on-device; scans live
in app-private storage and the app-private Room DB.

## License

MIT — see [LICENSE](LICENSE).
