# Releasing Pocket Numbat

Releases are driven by Conventional Commits on `master`.

## Versioning

`version.txt` is the Release Please source of truth. Keep `versionName` and `versionCode` as
literal values in `app/build.gradle.kts` because F-Droid reads them without running Gradle.

## Release pull request

1. Merge normal changes to `master` with Conventional Commit messages.
2. Release Please opens a release pull request that updates `version.txt` and `CHANGELOG.md`.
3. The `Release PR metadata` workflow runs on release-please branches. It syncs
   `versionName`, bumps `versionCode` only when the version name changes, and creates
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` if that file does not exist.
4. Review the pull request. Edit the fastlane changelog if needed. Do not merge while the metadata
   check is failing.
5. Merge the pull request.

For the first release, `version.txt` starts at `0.0.1` while Gradle already has
`versionName = "0.1.0"` and `versionCode = 1`. The metadata workflow keeps version code 1
because the version name already matches, and the existing `changelogs/1.txt` is preserved.

## Release

When the release pull request is merged, Release Please creates the `v<version>` tag and a GitHub
release whose body is the matching `CHANGELOG.md` section. GitHub automatically attaches the
source code archives. No APK is published on GitHub.

## Secrets

Configure `PAT` as a repository secret. Release Please and the metadata workflow use it and need
contents and pull request write access. No signing secrets are needed.

## F-Droid

F-Droid builds and signs the APK from the tagged commit. After the first `v0.1.0` release, add
metadata for `io.github.ntbbloodbath.pocketnumbat` to the `fdroiddata` repository and keep the
fastlane changelog under 500 characters.
