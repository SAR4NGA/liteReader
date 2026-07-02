# liteReader

A lightweight Android .docx document reader. ~658 KB APK, no frameworks, no ads, no permissions beyond reading files you open.

## Features

- Open .docx files directly from the file manager ("Open with" support)
- In-app file picker to browse and select .docx files
- High-fidelity rendering: tables, images, text formatting, borders
- Dark mode toggle
- Pinch-to-zoom and horizontal scrolling for wide documents
- Fully offline — no internet required

## How it works

Kotlin `MainActivity` wraps a `WebView` that loads a local `index.html`. The `docx-preview.js` library (with `jszip.js` for decompression) renders the document inside the WebView. The .docx file is read on the Java side, Base64-encoded, and passed to JavaScript via `evaluateJavascript`.

## Building

Requires JDK 23 and Android SDK 34.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
.\gradlew.bat clean assembleRelease --no-daemon
```

APK output: `app/build/outputs/apk/release/app-release.apk`

## Installing

```powershell
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Technical details

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | WebView (no native UI toolkit) |
| Rendering | [docx-preview.js](https://github.com/Volox/docx) v0.3.x |
| Decompression | [JSZip](https://stuk.github.io/jszip/) v3.10.1 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 |
| Minification | R8 (ProGuard), shrink resources enabled |
| APK size | ~658 KB |

## Intent filters

The app registers for `ACTION_VIEW` with `application/vnd.openxmlformats-officedocument.wordprocessingml.document` MIME type for both `file` and `content` URI schemes, so it appears in the "Open with" menu for .docx files.

## Project structure

```
liteReader/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── index.html              # WebView UI
│   │   │   ├── docx-preview.min.js     # Document renderer
│   │   │   └── jszip.min.js            # ZIP decompression
│   │   ├── java/com/litereader/
│   │   │   └── MainActivity.kt         # Core logic
│   │   ├── AndroidManifest.xml
│   │   └── res/                        # Launcher icons
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```
