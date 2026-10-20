# Play-only subscriptions (optional Compose Multiplatform module)

The independent `aznavrail-subscriptions` artifact contains a Compose Multiplatform
paywall UI and entitlement policy. Only its **Android** target contains Google Play
Billing 9.1.0. It does **not** alter `aznavrail`, `aznavrail-cmp`, either navigation
graph, existing host APIs, or any consuming app by itself.

**The subscription is attached to the Play FLAVOR, not to the .aab extension.**
An AAB is a distribution format, not a reliably distinguishable Gradle dependency
variant. Never link this artifact through `implementation` or `commonMain` if
GitHub builds must contain no billing/paywall code.

## 1. Configure Google Play Console

For **each** app create its own subscription product ID, with a monthly and/or
annual auto-renewing base plan. Create an eligible 3-day or 7-day free-trial
offer if wanted. In Play Console specify the actual country-specific price,
duration and renewal terms. Do not create weekly plans. **The library cannot
set any Play price, trial, or subscription ID for you.**

The module reads the customer's eligible offers, localized prices, trial phase,
and renewal price directly from Play. It never uses a device-started countdown.

## 2. Only link the library from the Play flavor

In an Android consuming application's `app/build.gradle.kts`, assuming the
existing `play` and `github` flavors are already defined:

~~~kotlin
dependencies {
    add(
        "playImplementation",
        "com.github.HereLiesAz.AzNavRail:aznavrail-subscriptions:<release-tag>",
    )
}
~~~

The app's main source set must NOT import anything from this module. Move only
the root subscription wrapper into `app/src/play`, and use a no-op wrapper in
`app/src/github`. Both files must share package and callable signature.

For example, define `StorefrontRoot` in
`app/src/play/java/com/example/app/StorefrontRoot.kt`:

~~~kotlin
package com.example.app

import android.app.Activity
import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.subscriptions.AzPlaySubscriptionConfig
import com.hereliesaz.aznavrail.subscriptions.AzPlaySubscriptionGate

@Composable
fun StorefrontRoot(activity: Activity, content: @Composable () -> Unit) {
    AzPlaySubscriptionGate(
        activity = activity,
        config = AzPlaySubscriptionConfig(
            productId = "app_premium",
            appName = "My App",
            githubUrl = "https://github.com/HereLiesAz/myapp",
            basePlanIds = setOf("monthly", "annual"),
        ),
        content = content,
    )
}
~~~

Define the *same* `StorefrontRoot` in
`app/src/github/java/com/example/app/StorefrontRoot.kt`:

~~~kotlin
package com.example.app

import android.app.Activity
import androidx.compose.runtime.Composable

@Composable
fun StorefrontRoot(activity: Activity, content: @Composable () -> Unit) {
    content()
}
~~~

The normal `MainActivity` common to both flavors calls:

~~~kotlin
setContent {
    StorefrontRoot(this@MainActivity) {
        AppRoot() // Existing root including the rail, navigation, and all screens.
    }
}
~~~

The module does not automatically modify `MainActivity` or gate activities,
services, deep links, or screens outside the wrapped root. Ensure all user-facing
entry points pass through that root, and protect paid backend operations separately.

## 3. What happens at runtime

- App launch / foreground / Restore purchase: ask Google Play for current SUBS
  purchases. Only a completed PURCHASED purchase for the configured product ID
  grants access; a pending transaction, expired subscription, unrelated SKU,
  missing Play account, or unsuccessful query does not.
- Existing purchases are restored from Play; new ones are acknowledged.
- Available subscriptions are presented with Google Play's localized price,
  trial when eligible, renewal cadence, and automatic-renewal disclosure.
- An in-app button opens Google Play subscription management/cancellation.
- Another button opens the **full-featured free GitHub edition**. This
  alternative is disclosed before checkout, even when the billing API fails.
- When billing cannot confirm entitlement, the root remains gated with Retry.
  This prototype intentionally offers no cached offline unlock; subscribers
  without Play connectivity need a future validated offline-grace policy.
- Cancellation does not automatically revoke already-paid access: Google's
  `queryPurchasesAsync` returns entitlement while still active.
- **No weekly plans; no lifetime supporter plan.**

## 4. Validate both artifacts before release

~~~sh
./gradlew :app:bundlePlayRelease
./gradlew :app:assembleGithubRelease
./gradlew :aznavrail-subscriptions:allTests
~~~

Actual Gradle task capitalization depends on each app's flavor/build-type
configuration. Inspect the release AAB dependencies with `dependencies
--configuration playReleaseRuntimeClasspath` and the GitHub release variant with
`githubReleaseRuntimeClasspath`. Verify that the latter contains **no**
`com.android.billingclient` and **no** `aznavrail-subscriptions`. Exercise
Play Console license testing, free-trial eligibility, canceled-but-still-active
entitlements, restore, pending purchases, unavailable Play Store, and both
monthly and yearly offers on a physical device.

## 5. Security / store compliance

This is a **client-only** implementation. BillingClient is a legitimate
mechanism for obtaining current Play purchases, but it is not a tamper-proof
license server and provides no entitlement expiry timestamp. For high-value
benefits, add backend purchase-token verification against the Google Play
Developer API, real-time subscription notifications, expiry and account-hold
handling; grant/acknowledge only verified entitlements. A modified APK can always
remove local gates.

Google Play's subscription policy requires a clear price, billing frequency,
automatic renewal, trial conversion, accessible cancellation, and sustained
value. The trial and every price are configured in Play Console, **not in the
library**. The free GitHub edition must remain available and honestly described.

The module currently supports CMP-rendered UI for Android, desktop, iOS (macOS
build hosts) and Wasm. **Only Android provides a Play Billing adapter.** Other
targets are not implicitly paywalled. The Play-flavor integration lives entirely
in the consuming Android application; never install it in a shared CMP root.
