# Google Play in-app reviews

The optional Android-only `aznavrail-reviews-play` artifact works independently of navigation and updates. It uses the native [Google Play In-App Review API](https://developer.android.com/guide/playcore/in-app-review). No account permissions, backend, analytics, or custom review UI are added.

## Install

Use a published version that contains this module (the SHA of a successful publication or a later stable release containing it). Older release 12.5 does not contain it. The Java 8 artifact is built with AGP 8.7.3 and supports API 24+, without a Kotlin metadata requirement.

In Gradle settings, add the same public Maven repository used for updates, with an exclusive content filter for this module (or extend the existing exclusive filter):

~~~kotlin
exclusiveContent {
    forRepository {
        maven("https://raw.githubusercontent.com/HereLiesAz/AzNavRail/update-maven/")
    }
    filter {
        includeModule("com.github.HereLiesAz.AzNavRail", "aznavrail-reviews-play")
    }
}
~~~

Add only to the Play distribution's dependency configuration:

~~~kotlin
dependencies {
    "playImplementation"("com.github.HereLiesAz.AzNavRail:aznavrail-reviews-play:VERSION")
}
~~~

For Play-only apps, `implementation` is sufficient. Never add it to GitHub/FOSS builds or common multiplatform source sets.

## Request

~~~kotlin
import com.hereliesaz.aznavrail.reviews.play.AzPlayReview

// At a natural stopping point, after the user completes a meaningful action:
AzPlayReview.request(activity)
~~~

For a custom policy:

~~~kotlin
AzPlayReview.request(activity, 5, 120)
~~~

Arguments are minimum distinct usage days and cooldown days. Defaults: three distinct UTC days with a meaningful opportunity; ninety 24-hour days between attempts. Call on the main thread when the Activity is focused, not during startup, on every recomposition, or during another dialog. A day is counted at most once even if the Activity is recreated. The attempt is persisted before requesting Play, including failed or suppressed requests. State is app-local (it may be cleared or restored by Android app-data management); it is not tied to an account or version. Debug, sideload and unknown-store installs are ignored.

**Existing ratings:** Play may suppress the native review prompt for accounts that already reviewed the app or exceed its quota. The application cannot query a user's review status, cannot know whether a dialog appeared, and cannot know whether a rating or review was submitted. Local cooldown state must never be called a rating status. No pre-screening, incentives, or fallback dialog is used.

For an explicitly user-initiated Rate app link, Google recommends linking to the Play listing instead of promising the in-app dialog. See the [guidelines](https://developer.android.com/guide/playcore/in-app-review#when-to-request) and [testing instructions](https://developer.android.com/guide/playcore/in-app-review/test). Unit tests verify the local policy; a Play internal test-track install with an eligible Google account is needed to test the actual dialog. Internal testing does not enforce production quotas.

## Publishing

`Publish Play review library` builds and tests the standalone module and publishes immutable SHA coordinates from main and numeric coordinates for subsequent stable releases containing the module. It uses the shared Maven branch and serialization group to avoid publication races. No existing release is mutated.
