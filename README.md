# File Xplorer

An original offline Android file manager with a light liquid-glass visual style, a compact phone layout, and two-pane navigation for tablets and Android desktop windows. Native Java views; Android 8.0+ (API 26), target API 35.

This is a development build, not complete MT Manager, 7-Zip or PowerISO feature parity. See [the exact capability list](app/src/main/assets/capabilities.txt). The desktop layout runs in Android windows, not as a Windows/macOS executable. Glass surfaces use rounded translucent panels and a blurred scene; modal backgrounds and built-in media controls also blur the content behind them. Android 12+ uses RenderEffect/native window blur, with a blurred snapshot fallback on Android 8–11. Dynamic optical refraction is not implemented.

## v0.5 appearance and media update

Phone actions now sit above the five-item bottom dock, which contains Search. The mobile top search button and overview search field have been removed to reclaim space. Tablets retain their top search and two-pane action toolbar.

Choose **Settings → Appearance → Follow device theme / Light glass / Dark glass**. The theme covers phone/tablet navigation, file rows, action capsules, fields, checkboxes, submenu lists, search forms, confirmations, error/progress dialogs and editors. **Smooth animations** and **Blur glass backgrounds** can be disabled in Settings. Motion also respects Android's global animator setting.

App-owned image/video viewers use floating rounded glass toolbars, with a blurred sample of the media behind the controls. Images support fit, pinch-to-zoom, pan and rotate; video supports play/pause, seeking, skip, mute and speed presets. Sharing is user-initiated through Android's chooser. Codec support depends on the device; other formats can still use external viewers. System permission dialogs and external apps retain their own styling.

## Search

Tap the magnifier or the overview search field. Search names and folders, or plain-text contents (UTF-8/UTF-16), recursively or just in the selected folders. Combine a filename query with extensions, minimum/maximum sizes such as `500KB` / `2GB`, and modified-date boundaries. Enable case sensitivity, RE2 regular expressions, or hidden files as needed. RE2 avoids catastrophic backtracking; backreferences and lookbehind are unsupported.

Search the current pane or all accessible locations, including granted SD/USB document trees. Results stream in with scanned/skipped counts, a filename/path, and a content line/snippet when relevant. Tap a result to open; use its containing-folder action to reveal it. Search can be stopped. Protected locations need Android grants; there is no permission bypass. Content is bounded to 50 MiB/file and 64K-character chunks; 5,000 results maximum. No archive-inside or PDF/Office document-text indexing.

## Working tools

- File operations: create, rename, copy, move, multiselect, permanent delete with confirmation, favorites, recents, sorting, hidden files, external viewers.
- ZIP/AES-256 ZIP, 7z/LZMA2, TAR and compressed TAR creation; supported archive extraction, inspection, integrity tests and ZIP entry edits with backups.
- ISO9660/Joliet creation and extraction; raw single-track data BIN-to-ISO conversion. BIN files can be inspected as hex or opened by a compatible installed app; Browse does not provide a direct binary-run action. No optical burning, UDF or proprietary image-format parity.
- Text/hex editor, checksums, APK metadata/signature inspection, binary manifest decoding, DEX index, smali assembly/disassembly, APK signing and installed APK/split export.
- App-scope terminal; commands run with File Xplorer's normal Android permissions.

The app requests Internet access for FTP/FTPS. It asks separately for all-files access, document-tree grants and unknown-source installation when those capabilities are used. Transfers are not persistent Android foreground services and stop if the process is killed. Android and document-provider restrictions still apply. APK signing uses a new device key, not an original publisher's identity.

## First run

1. Install the development APK using Android's package installer.
2. Use the private workspace immediately, grant **All files access** for shared storage, or **Choose a folder** for an SD card/USB/document provider.
3. Long-press files to select, then Copy/Move and Paste into the active pane. Wide layouts show both panes; phones switch Panel A/B.
4. Open **Tools → Capabilities** for format support and limits.

## Build

Requires Java 17, Python 3, Android SDK platform 35 and build-tools 36.0.0. No Gradle distribution is required. The build resolves 20 SHA-256-locked libraries, compiles Java, dexes dependencies, packages resources, aligns and signs the APK.

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
python3 tools/dependencies.py
python3 tools/test.py
bash build.sh
python3 tools/apk-smoke.py
```

`ANDROID_PLATFORM` and `BUILD_TOOLS_VERSION` can override the SDK directories. Output: `build/File-Xplorer-v0.7.2.apk`. A local development keystore is generated in `build/`; preserve it locally to install compatible development updates. The release keystore backup is `build/release-signing.jks`; keep it offline and never commit it. Both are excluded from source control.

GitHub Actions runs core tests and the APK smoke test on pushes and pull requests. On pushes and manual runs, it also builds a release-signed APK and AAB. Signing uses the repository Actions secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`; keep the same keystore for every update and maintain a secure backup. Dependency URLs and hashes are recorded in `dependency-lock.json`. Original application code is Apache-2.0; bundled dependencies retain their respective licenses in `app/src/main/assets/licenses`.

## Design

[Phone light layout](design/v0.5/phone-light.svg) · [Tablet dark layout](design/v0.5/tablet-dark.svg) · [Editable Figma importer](design/README.md).

A Figma file was created at https://www.figma.com/design/Kib2t3tMfhqztEAejCcfgT, but account canvas quota blocked writing its contents. It remains empty until the included local importer is run. The SVG references use illustrative file names and storage values; the Android application reads actual device data.

## Validation and remaining work

73 host assertions cover archive round trips and encrypted ZIPs, Unicode and empty folders, traversal/symlink rejection, exclusive writes, cancellation, raw BIN conversion, search filters/UTF-16/regex, smali assembly/disassembly, and real APK manifest decoding plus signing/verification. Generated ISO/Joliet data was also independently read using pycdlib. APK v2/v3 signatures verify.

No Android emulator or physical device was available for this delivery. UI, touch/keyboard accessibility, permission flows, document-provider behavior and installation need device testing. This build is a substantial first implementation, not a production-certified release. Network storage beyond FTP/FTPS, full resource recompilation, RAR5/proprietary codecs, persistent background transfers, recycle bin and true optical-drive operations are not implemented.

## v0.5 UI updates

- Simplified phone overview based on the supplied sketch, with a prominent on-device storage card, rich icon tiles, recent files, and storage-location shortcuts.
- Mobile Browse now switches between rounded grid cards and a list view; copy, move, paste, more, and panel switching sit above the four-item dock. Tablet layout remains unchanged.
- APK files offer install, inspect, or browse actions. Android system confirmation handles installation; unknown-source access opens the system permission page.
- Phone background now uses a soft pastel glass palette. Raised gradient icon badges add depth while the tablet theme remains unchanged.

## Clear Studio mobile direction

The selected mobile direction uses warm neutral backgrounds, restrained terracotta accents, square three-column file tiles, rounded glass cards, and a floating four-tab dock. It uses standard Android layouts and shape backgrounds. The Figma importer source is in `design/figma-import-clear-studio.js`; in-app tablet layout keeps its established palette.
