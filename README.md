# RingPrint

[![Release](https://img.shields.io/badge/download-latest%20release-blue)](https://github.com/ELPEDR/RingPrint/releases)

**Replace the fingerprint icon on the One UI lock screen and AOD with a minimal HyperOS-style ring, or with your own icon.**

An LSPosed module (legacy Xposed API) that **does not modify any system APK**. To revert, just disable the module. The settings app (shown on the launcher as **Fingerprint Ring**) uses **Material You** and follows your wallpaper colors.

## How it works

Samsung's fingerprint icon is not a bitmap: it is a set of **Lottie (JSON) animations** inside the biometrics app (`com.samsung.android.biometrics.app.setting`). That app uses `sharedUserId="android.uid.system"` and is signed with Samsung's key, so **re-signing the APK is not possible** (the system rejects it and the fingerprint sensor stops working).

RingPrint hooks `AssetManager.open()` inside that app's process. When the app requests an `ic_fingerprint_*.json`, the module returns a ring (or your imported icon) instead. The original APK stays untouched.

| File | Where it shows up |
|---|---|
| `ic_fingerprint_aod.json` | AOD |
| `ic_fingerprint_dark_theme.json` / `_non_alpha` | Lock screen, dark theme |
| `ic_fingerprint_light_theme.json` / `_non_alpha` | Lock screen, light theme |

## Features

- Minimal ring by default.
- Settings screen with **diameter** and **thickness** sliders and a live preview.
- **Import your own icon**:
  - **PNG** with a transparent background (auto-resized, sized by the diameter slider).
  - **Lottie `.json`** (used as is; `.lottie` files are not supported).
  - Separate slots for **dark/AOD** and **light** themes.
- **Apply** button that restarts only the fingerprint process through root (no reboot needed).
- **Reboot device** and **Restore default ring** in the overflow menu.
- **Material You** UI (Material 3 with dynamic color).

## Requirements

- Rooted Android with **LSPosed** (tested with KernelSU).
- Samsung **One UI**. Tested on **One UI 8 (Android 16)** on a **Galaxy S20 FE**.
- Dynamic colors need Android 12+. On One UI, make sure **Settings > Wallpaper and style > Color palette** is on, otherwise the app falls back to the default colors.
- To build: Android Studio, **JDK 17 to 21** (not JDK 25), Gradle 8.9, Android Gradle Plugin 8.7.3, `compileSdk 35`, Material Components 1.13.0.

> Other devices and One UI versions are untested. If your version draws the icon in a different app or reads it a different way, the hook may not catch it.

## Download

Get the latest APK from the **[Releases page](https://github.com/ELPEDR/RingPrint/releases/tag/public)**, or build it yourself (see [Build](#build)).

## Install

1. Download the APK from [Releases](https://github.com/ELPEDR/RingPrint/releases/tag/public) (or build it) and install it.
2. Open **LSPosed** > Modules > **Fingerprint Ring** and enable it.
3. Select only the scope **`com.samsung.android.biometrics.app.setting`**.
4. Reboot (or restart the biometrics app process).
5. Open the app, adjust the ring or import an icon, and tap **Apply**. Grant root when asked.

To confirm the hook loaded, look for this line in the module's LSPosed log:

```
FpRing: hook active in com.samsung.android.biometrics.app.setting
```

## Build

1. Clone the repo (`git clone https://github.com/ELPEDR/RingPrint.git`), open the folder in Android Studio and let Gradle sync.
2. In **Settings > Build Tools > Gradle**, pick a JDK 17 to 21 (for example the embedded `jbr-21`).
3. **Build > Build APK(s)**.
4. The APK ends up in `app/build/outputs/apk/debug/app-debug.apk`.

The Xposed API library (`de.robv.android.xposed:api:82`) is downloaded from `https://api.xposed.info/`.

## Tips for custom icons

- Use a **white** icon for the dark/AOD theme and a **dark** one for the light theme. A single-color icon can disappear on one of the two backgrounds.
- Prefer simple, clean strokes. The icon is small and semi-transparent on screen.
- Keep the drawing centered and about the size of the sensor, so it is clear where to touch.
- Imported Lottie files are used unchanged: the diameter and thickness sliders do not apply to them. Use a 288x288 canvas like the original.

## Reverting / troubleshooting

- **Back to normal:** disable the module in LSPosed and reboot.
- **Icon did not change:** check the scope in LSPosed, look for the `FpRing: hook active` log line, and tap **Apply** again.
- **Apply does nothing:** make sure the app has root permission in your root manager (KernelSU, Magisk, etc.).

## Contributing

Issues and pull requests are welcome at [github.com/ELPEDR/RingPrint](https://github.com/ELPEDR/RingPrint/issues). If it does not work on your device or One UI version, please include your device model, One UI version and the LSPosed log.

## Disclaimer

The module runs inside a system app. Use at your own risk and keep a way to disable it (recovery or `adb`). Not affiliated with Samsung.

## Project layout

```
app/src/main/java/com/fpring/lsp/
├── Hook.java            # hooks AssetManager.open in the biometrics app
├── Icons.java           # ring Lottie + PNG-to-Lottie generator
├── ConfigProvider.java  # serves config and icons to the hook (other process)
├── MainActivity.java    # Material You settings screen, import and root
├── PreviewView.java     # live preview
└── App.java             # applies dynamic color
```
