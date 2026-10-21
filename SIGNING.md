# Signing Pocket Numbat

F-Droid signs its own builds. The key below is for a local release or a
self-hosted distribution. Never commit a private key.

## Create a keystore

```bash
keytool -genkeypair -v \
  -keystore pocket-numbat.jks \
  -alias pocket-numbat \
  -keyalg RSA -keysize 4096 -validity 10000
```

Keep the keystore outside the repository, or rely on the existing ignore rules.
The repository ignores `key.properties`, `*.jks`, and `*.keystore`.

## Local signing

Build the unsigned release APK:

```bash
./gradlew :app:assembleRelease
```

Sign it with the SDK build tools:

```bash
$ANDROID_HOME/build-tools/37.0.0/apksigner sign \
  --ks /path/to/pocket-numbat.jks \
  --ks-key-alias pocket-numbat \
  --out app/build/outputs/apk/release/app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

Verify the result:

```bash
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release-signed.apk
```

The release build enables R8 and resource shrinking. The JNI entry points are
kept by the rule in `app/proguard-rules.pro`.

## Reproducible builds

F-Droid rebuilds the app from source. For a local comparison, clean and build
twice with the same toolchain:

```bash
./gradlew clean :app:assembleRelease
```

Gradle and the Android toolchain can embed timestamps, so use F-Droid's
reproducible-build tooling for a strict comparison.
