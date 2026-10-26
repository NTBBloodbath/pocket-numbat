# Pocket Numbat

An unofficial, native Android client for the [Numbat](https://numbat.dev) scientific calculator.

Pocket Numbat runs the Numbat engine locally through a Rust JNI bridge, with no WebView in the
calculation path. Units, physical constants, functions, and formatted error diagnostics work
offline. Currency conversion uses cached European Central Bank reference rates.

## Features

- Numbat expressions with units, constants, and functions
- Offline math and readable error diagnostics
- Currency conversion with cached ECB rates
- Scrollable history with copy, reuse, and session persistence
- Native Compose UI with dynamic color
- Android 8.0 or newer

## Privacy

Privacy is a goal of this project. The only permission the app requests is `INTERNET`, used solely
for currency conversion. Network access is optional: without it, math, units, constants, and history
  still work, and history stays on the device. Currency rates are cached locally. There are no
  accounts, ads, analytics, or tracking.

## Transparency

This project used LLMs for insight, brainstorming, and code assistance. I review the code and remain
responsible for it.

## Build

The recommended setup is the included devenv environment. It provides JDK 17, Android SDK 37, NDK
28.2.13676358, the Rust Android targets, and `cargo-ndk`.

```bash
devenv shell
./gradlew assembleDebug
```

Without devenv, install those same tools and set `sdk.dir` in `local.properties` or `ANDROID_HOME`.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The first build also
compiles the Rust bridge, so it takes longer than later builds.

For checks:

```bash
./gradlew lintDebug
cargo test --manifest-path rust/Cargo.toml
```

### Release

Releases are automated. Merge a Release Please pull request on `master`; Release Please creates the
tag and a GitHub release with the changelog and source archives. The `Release APK` workflow builds
and signs the APK from that tag and attaches it to the release. See `RELEASING.md` for the process.

To build an unsigned APK locally:

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release-unsigned.apk`.

## License

Pocket Numbat is licensed under GPL-3.0-or-later. The Numbat engine is used under the MIT license.
See `LICENSE` and `THIRD_PARTY_NOTICES`.

Pocket Numbat is not affiliated with the Numbat project.
