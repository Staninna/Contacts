# Fossify Contacts: droid fork

A personal fork of FossifyOrg/Contacts, maintained as a droid app: modified the Android way and
shipped as signed GitHub releases. The fork replaces the home screen (contacts, favorites, groups)
and the contact details screen with a Jetpack Compose UI in Material 3 Expressive, the look of
Android 16 and 17. Everything else (editor, settings, groups screen, import/export, dialogs) is
upstream's. Load the droid skill with `/droid` before any branch, sync, signing or release work.
When it isn't available, the ground rules below are enough to work safely.

## This repo

- `origin` is the fork (Staninna/Contacts), `upstream` is the original (FossifyOrg/Contacts).
- `my-main` holds all fork work and is the GitHub default branch. `main` mirrors `upstream/main`.
- Install mode: replace. The fork keeps upstream's application ID `org.fossify.contacts` and the
  name. Commons shows a "modded app" warning to any other package name, another reason to keep it.
- Release build: `:app:assembleFossRelease` (the `foss` flavor), JDK 21. Unit tests:
  `./gradlew :app:testFossDebugUnitTest`. compileSdk is 37.2 because Compose 1.13 and Material 3
  1.5 require it; targetSdk and minSdk stay upstream's.
- `.github/workflows/release.yml` is the droid release workflow. It replaced upstream's, which
  published to Google Play through FossifyOrg's shared workflow. Upstream's other workflows are
  for their PRs, issues and translations; leave them alone.
- Machines with 8 GB of RAM: run Gradle tasks one at a time (tests, then assemble), or the daemon
  gets killed. `~/.gradle/gradle.properties` with `org.gradle.jvmargs=-Xmx3g` and
  `kotlin.compiler.execution.strategy=in-process` helps.

## Seeing the UI without a phone

`ScreenshotTest` renders every fork screen with sample people under Robolectric and writes PNGs
to `app/build/fork-screenshots/`. Run the unit tests and look at them after any UI change. On
Robolectric the wallpaper colors come out grey; on a phone they follow the wallpaper.

## Ground rules

- **Commit to `my-main`**, directly or through a short-lived branch merged into it. Upstream reaches
  `my-main` by merge. `my-main` is published, so its history only grows: merge, never rebase.
- **`main` only fast-forwards from `upstream/main`.** If it has diverged, stop and ask.
- **A push to `my-main` is a release.** CI tags it `v<VERSION_NAME>-fork.<n>` and publishes signed
  APKs. The tip commit's body becomes the release notes. Pushes that only touch `paths-ignore`
  paths or carry `[skip ci]` don't release.
- **Nothing goes upstream.** This is a personal fork.
- **Fork code lives in its own files, and upstream files only get `fork:` hooks.** See below.
- **Test on the emulator, never on a physical phone.**
- **Verify before committing:** run the unit tests and build the debug variant. Before pushing,
  also build `:app:assembleFossRelease`: R8 problems only show up in the minified build.
- **Tags:** only `-fork.<n>` tags, so they never clash with upstream's. Pushed tags stay where they
  are. A failed release is fixed by pushing a fix to `my-main`.
- **Signing keys are hands-off.** The key lives outside the repo, and only the droid scripts
  (`signing-key.sh`, `build-signed.sh`) touch it. Never read, print or create keystores (`*.jks`,
  `*.keystore`, `keystore.properties`) or anything in the release-keys folder.
- **Commit identity:** pass the GitHub noreply address per commit with
  `git -c user.name=Staninna -c user.email=26458805+Staninna@users.noreply.github.com commit`.
  Never write git config.
- **Backups:** the app is Seedvault backup-ready (`res/xml/fork_data_extraction_rules.xml`); it
  keeps settings and the private "phone storage" contacts, and excludes nothing. Check this again
  when a change adds or moves stored data.

## Fork code and hooks

Each fork change is either in a fork file or marked where it touches upstream, so every upstream
sync merges cleanly and a conflict shows at a glance whose line it is.

- **New code goes in fork files.** Kotlin goes in `app/src/main/kotlin/org/fossify/contacts/fork/`
  (`data/`, `ui/common/`, `ui/home/`, `ui/details/`, `ui/theme/`), tests in
  `app/src/test/java/org/fossify/contacts/fork/`. Resources get a `fork_` prefix
  (`values/fork_strings.xml`, `xml/fork_*`). Fork files need no markers.
- **How the screens take over.** `SplashActivity` starts `ForkHomeActivity`. Upstream's
  `MainActivity` and `ViewContactActivity` stay registered for outside links, shortcuts and other
  apps; a one-line hook in their `onCreate` forwards the same intent to the fork's screen through
  `fork/ForkUi.kt` and closes. The fork's screens reuse upstream's helpers (`ContactsHelper`,
  `ContactActivity`, the dialogs) rather than copying them.
- **An upstream file gets a hook, not logic.** A hook is the smallest edit that hands off to fork
  code, usually one call. Once it needs more than a call and a condition, move the body into a fork
  file and call that.
- **Every changed upstream line carries `fork: <why>`**, in the file's own comment syntax: `//` in
  Kotlin and Gradle, `#` in TOML and ProGuard, `<!-- -->` in XML. A hook spanning several lines gets
  the comment on the line above it.
- **Call fork code fully qualified** (`org.fossify.contacts.fork.ForkUi.takeOverMain(this)`), so the
  hook stays one line and the upstream import block stays untouched.
- **Removing a fork feature removes its hooks too.** This finds every place upstream touches the fork:

      grep -rn "fork" app/src/main app/build.gradle.kts app/proguard-rules.pro build.gradle.kts gradle/libs.versions.toml | grep -v "/fork/"

- **In a sync conflict on a `fork:` line,** take upstream's version of the line and put the hook
  back on top of it.
