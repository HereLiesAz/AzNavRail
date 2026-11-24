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

// Launch Activity, after super.onCreate(savedInstanceState):
AzPlayReview.request(this)
~~~

Register once on the main thread during Activity creation. The module waits one minute after the app enters the foreground and then makes at most one review attempt per local calendar day. There is no minimum usage-day threshold or multi-day cooldown. Reopening on the same day does not repeat the attempt; returning from the background starts a fresh one-minute timer. Activity changes and configuration recreation do not reset an ongoing foreground timer. If window focus is temporarily unavailable, the request waits until focus returns. The app must be in the foreground when Play's review flow is launched. An app left open across midnight waits until the next foreground session before attempting again.

The attempt timestamp is persisted before contacting Play, including failed or suppressed requests, and existing timestamps remain valid after an upgrade. The deprecated three-argument overload remains source- and binary-compatible but ignores its old policy arguments. State is app-local (it may be cleared or restored by Android app-data management), not tied to an account or version. Debug, sideload and unknown-store installs are ignored.

**Existing ratings:** Play may suppress the native review prompt for accounts that already reviewed the app or exceed its quota. The application cannot query a user's review status, cannot know whether a dialog appeared, and cannot know whether a rating or review was submitted. Local attempt state must never be called a rating status. No pre-screening, incentives, or fallback dialog is used.

For an explicitly user-initiated Rate app link, Google recommends linking to the Play listing instead of promising the in-app dialog. See the [guidelines](https://developer.android.com/guide/playcore/in-app-review#when-to-request) and [testing instructions](https://developer.android.com/guide/playcore/in-app-review/test). Unit tests verify the local policy; a Play internal test-track install with an eligible Google account is needed to test the actual dialog. Internal testing does not enforce production quotas.

## Publishing

`Verify Play review library` tests the standalone build on pull requests without acquiring the publication queue. `Publish Android update libraries` then publishes the review artifact after the update artifacts, using the same SHA/stable-tag matrix and Maven branch. Revisions predating the review module are skipped. Keeping the writers in one workflow avoids GitHub Actions cancelling a pending publication when a third run enters the shared concurrency group. No existing release is mutated.
