# Android update checking

Use the optional `com.github.HereLiesAz.AzNavRail:aznavrail-updates` artifact at the same published revision as AzNavRail. It is independent of navigation and works in Android activities hosting either native views or Compose Multiplatform.

Call `AzUpdateChecker.check(this, "HereLiesAz/YourRepo")` after activity creation. For a settings action call it with `manual = true`. Import `com.hereliesaz.aznavrail.updates.AzUpdateChecker`.

GitHub installs query stable releases and prioritize the release workflow's versionCode. Debug builds and unrelated stores are ignored. Automatic checks are throttled for six hours. A skipped version can still be checked manually. The dialog opens the official release page rather than downloading an APK.

For Google Play builds add `com.github.HereLiesAz.AzNavRail:aznavrail-updates-play` through `playImplementation` (or the actual Play-only configuration). Pass `playSource = AzPlayUpdates` from the Play source set, importing `com.hereliesaz.aznavrail.updates.play.AzPlayUpdates`. The adapter queries actual Play update availability. Without it no offer is shown; GitHub is never substituted for Play. The core artifact has no Play dependency.

## Reliable artifact repository

The dedicated `Publish Android update libraries` workflow tests and publishes both standalone modules into the public Maven repository at `https://raw.githubusercontent.com/HereLiesAz/AzNavRail/update-maven/`. Use a successfully published numeric release tag (for example, `12.5`) as the version, or a full source commit SHA for development builds. The workflow publishes stable numeric release tags and backfills the latest stable tag when main is published. The publication branch retains previous versions. This avoids requiring consumers to trigger an on-demand JitPack build. Existing navigation artifacts and JitPack coordinates are unchanged.

In Gradle settings, add a Maven repository for that URL with an exclusive content filter for the two update modules. Do not select this repository for navigation artifacts.

## Older Android toolchains

The optional `aznavrail-updates-compat` and `aznavrail-updates-play-compat` artifacts are built from the exact same numeric release tag, including 12.5, using Kotlin 1.9.24, AGP 8.7.3, compile SDK 35 and Java 8 bytecode. They retain minSdk 24 and the same public package/API; use these coordinates instead of (not alongside) the normal modules in older consumers. Add the two compatibility module names to the repository content filter. The Play compatibility module depends on the core compatibility module, not the modern one. These separately published coordinates do not replace or mutate an existing release.
