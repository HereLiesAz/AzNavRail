# Android update checking

Use the optional `com.github.HereLiesAz.AzNavRail:aznavrail-updates` artifact at the same published revision as AzNavRail. It is independent of navigation and works in Android activities hosting either native views or Compose Multiplatform.

Call `AzUpdateChecker.check(this, "HereLiesAz/YourRepo")` after activity creation. For a settings action call it with `manual = true`. Import `com.hereliesaz.aznavrail.updates.AzUpdateChecker`.

GitHub installs query stable releases and prioritize the release workflow's versionCode. Debug builds and unrelated stores are ignored. Automatic checks are throttled for six hours. A skipped version can still be checked manually. The dialog opens the official release page rather than downloading an APK.

For Google Play builds add `com.github.HereLiesAz.AzNavRail:aznavrail-updates-play` through `playImplementation` (or the actual Play-only configuration). Pass `playSource = AzPlayUpdates` from the Play source set, importing `com.hereliesaz.aznavrail.updates.play.AzPlayUpdates`. The adapter queries actual Play update availability. Without it no offer is shown; GitHub is never substituted for Play. The core artifact has no Play dependency.
