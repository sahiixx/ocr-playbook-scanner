# Build & Deploy Guide — OCR Playbook Scanner

This doc covers every path from source to an installed app on your phone.

## 0. What you need

| Requirement | Version | Notes |
|---|---|---|
| JDK | 17 | AGP 8.5 requires JDK 17 (not 8/11/21) |
| Android SDK | platform-34, build-tools 34.0.0 | via Android Studio or cmdline-tools |
| Gradle | 8.7 | wrapper properties pin this; CI installs it |
| Device | Android 8.0+ (API 26) | camera optional — gallery works without it |
| Disk | ~8 GB free for SDK + caches | first build downloads ~1–2 GB of deps |

## 1. Cloud build (no local SDK needed)

You are on a Termux/PRoot box without Java — this is the practical path.

```bash
cd /root/ocr-playbook-scanner
gh repo create sahiixx/ocr-playbook-scanner --public --source=. --push
# then:
gh workflow run "Android CI — build + test + APK" --repo sahiixx/ocr-playbook-scanner
gh run watch --repo sahiixx/ocr-playbook-scanner
gh run download --repo sahiixx/ocr-playbook-scanner -n ocr-playbook-debug-apk
```

Install the APK: transfer it to your phone (USB, Drive, Telegram saved
messages, `python3 -m http.server` on LAN) and tap it — enable
"Install unknown apps" once when prompted. Or over USB:

```bash
adb install -r app-debug.apk
```

## 2. Local build with Android Studio (recommended for dev)

1. Install **Android Studio Hedgehog or newer** (bundles JDK 17 + SDK).
2. `File → Open…` → select the `ocr-playbook-scanner` folder.
3. Let it sync (first sync downloads Gradle 8.7 + deps).
4. `Run → Run 'app'` (Shift+F10) with your phone plugged in
   (USB debugging on) — or pick an emulator (Pixel 6, API 34).
5. Debug APK also at `app/build/outputs/apk/debug/app-debug.apk`.

## 3. Local build with command line only

```bash
# Install SDK pieces (example paths; adjust):
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
# Tell Gradle where the SDK is:
cp local.properties.example local.properties
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
# Build + test:
gradle :app:testDebugUnitTest      # JVM tests (no device needed)
gradle :app:assembleDebug          # APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 4. Release build (signed, for sharing / Play)

```bash
# 1. Create a keystore ONCE (guard the passwords!):
keytool -genkeypair -v -keystore ocr-playbook.jks -alias playbook \
  -keyalg RSA -keysize 2048 -validity 10000
# 2. Create keystore.properties (NEVER commit — gitignored):
#    storeFile=/abs/path/ocr-playbook.jks
#    storePassword=...
#    keyAlias=playbook
#    keyPassword=...
# 3. Build:
gradle :app:assembleRelease
# 4. Verify + install:
apksigner verify --print-certs app/build/outputs/apk/release/*.apk
adb install -r app/build/outputs/apk/release/*.apk
```

Play Store: use `gradle :app:bundleRelease` for an `.aab`, upload to the
Play Console, complete the data-safety form (no data collected), and roll
out to internal testing first.

## 5. Deploy cheat-sheet (on your phone)

### Install "unknown apps" permission (for sideloaded APKs)

`Settings → Apps → <browser or file manager> → Install unknown apps → Allow`.

### Direct ADB over Wi-Fi (no USB cable, same LAN)

```bash
# On phone: Settings → Developer options → Wireless debugging → Pair.
adb pair 192.168.1.50:37000     # enter the 6-digit PIN
adb connect 192.168.1.50:39000
adb install -r app-debug.apk
```

### Get the APK onto the phone without USB

```bash
# From GitHub Actions: download artifact → push to phone via Telegram/Drive.
# Or serve on LAN and download in the phone browser:
python3 -m http.server 8000 --directory app/build/outputs/apk/debug
# → http://<your-pc-ip>:8000/app-debug.apk
```

## 6. Runtime troubleshooting

| Symptom | Fix |
|---|---|
| "Could not decode image" | File isn't a decodable bitmap (corrupt/HEIF edge case) — try another photo |
| OCR empty on dark low-contrast text | More light; ML Kit Latin works best on high-contrast, upright text |
| Live overlay never fires | Grant CAMERA; keep the preview surface visible (analyzer runs per frame while paused on text) |
| Slow on 2–4 GB phones | Already handled: frames downscaled to 1280px wide, RGB_565, 1.2 s throttle |
| `EADDRINUSE`/port conflicts | Not applicable — this app opens no sockets |
| CI fails on dependency download | Rerun the job; mavenCentral/Google transient errors are usually one-off |

## 7. Android Studio vs cloud: which to use

- **Start here**: GitHub Actions gives you a signed-able debug APK in ~6 min.
- **Iterate**: Android Studio gives hot reload + emulator previews.
- **Ship to Play**: Android Studio `Bundle Release` (the keystore stays local).
