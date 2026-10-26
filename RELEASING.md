# Releasing Pocket Numbat

Releases are driven by Conventional Commits on `master`.

## Versioning

`version.txt` is the Release Please source of truth. Keep `versionName` and `versionCode` as
literal values in `app/build.gradle.kts` because the release metadata workflow reads them without
running Gradle.

## Release pull request

1. Merge normal changes to `master` with Conventional Commit messages.
2. Release Please opens a release pull request that updates `version.txt` and `CHANGELOG.md`.
3. The `Release PR metadata` workflow runs on release-please branches. It syncs
   `versionName`, bumps `versionCode` only when the version name changes, and creates
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` if that file does not exist.
4. Review the pull request. Edit the fastlane changelog if needed. Do not merge while the metadata
   check is failing.
5. Merge the pull request.

## Release

When the release pull request is merged, Release Please creates the `v<version>` tag and a GitHub
release. The `Release APK` workflow runs on `release: published`, builds `assembleRelease` from the
tag, signs the APK, and uploads `pocket-numbat-<version>.apk` to the release.

To rebuild or attach an APK to an existing release, run the `Release APK` workflow manually with the
tag, for example `v1.0.0`. An existing asset with the same name is replaced.

## Signing

Generate a release keystore once and back it up offline. Losing it means users must uninstall and
reinstall to receive updates.

```bash
keytool -genkeypair -v \
  -keystore pocket-numbat-release.jks \
  -alias pocket-numbat \
  -keyalg RSA -keysize 4096 -validity 3650
```

Add these repository secrets:

- `SIGNING_KEY_BASE64`: output of `base64 -w0 pocket-numbat-release.jks`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Configure `PAT` as a repository secret. Release Please and the metadata workflow use it and need
contents and pull request write access.

Do not commit the keystore. `.gitignore` excludes `*.jks`, `*.keystore`, and `key.properties`.

## Distribution

GitHub Releases is the distribution channel. Friends install the signed APK once and can use
Obtainium with the repository URL for updates.

The first signed release uses a new key. Anyone on a debug APK must uninstall it once before
installing the signed APK.
