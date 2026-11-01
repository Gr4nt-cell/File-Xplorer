# File Xplorer v0.2 validation · 2026-11-01

## Passed

- 73 host assertions cover search, archive round trips, encrypted ZIPs, path/symlink protection, exclusive writes, cancellation, ISO/BIN conversion, DEX/smali and hex parsing.
- Native Android source compiles, D8 dexing succeeds, resources and theme animations package with aapt2, and the APK aligns correctly.
- APK Signature Scheme v2 and v3 verify. The development signing identity is retained from v0.1.
- The APK workshop decodes the real binary manifest and re-signs/verifies a real APK with a host test key.
- The v0.2 Figma importer passes JavaScript syntax checks. Ten editable SVG references were generated; mobile, tablet and media contact sheets were rendered and visually inspected using Roboto.

The app compiles using Java 17, installed Android platform android-37.0 and build-tools 36.0.0; minSdk 26, targetSdk 35. Build defaults and CI request platform 35. Dependencies remain SHA-256 locked.

## Implemented in this version

- Mobile Search moves into a five-item bottom dock; the top mobile search button/field is removed. Copy/Move/Paste/More sits above the dock.
- Shared rounded glass styling covers app-owned menus, lists, forms, checkboxes, confirmations, progress/error dialogs, editors, rows, navigation, status messages and new media controls.
- System/light/dark appearance and animation/blur settings apply to phone/tablet layouts.
- Underlying activity blur uses RenderEffect on Android 12+ and a blurred snapshot on Android 8–11. Native window background blur is requested where available. Main surfaces sample the blurred decorative backdrop; media toolbars sample the displayed media.
- Built-in image fit/zoom/pan/rotate/share and video play/pause/seek/skip/mute/speed/share. Audio focus loss pauses playback. Device codec support determines playable formats; GIF is a static first-frame preview.
- Android/data and Android/obb shortcuts use su root transport into subfolders. Root directory listing now rejects unreadable locations, root deletion preserves symlink identity, and copy-into-self protection also applies across normal/root transports.

## Not exercised

No emulator or physical Android/root device was available. Launch/install, touch input, device-specific blur/graphics, video codec/audio focus behavior, system theme changes, permissions, document-provider interoperability and root access still require on-device QA. The 73 checks cover core logic, not visual or device integration tests.

Figma canvas access remains quota-blocked; the new importer has not been executed remotely, and the created file remains empty.

System permission screens, root-manager prompts and external apps retain their own UI. Shizuku is not included. Full MT Manager / 7-Zip / PowerISO parity is not claimed; see app/src/main/assets/capabilities.txt.
