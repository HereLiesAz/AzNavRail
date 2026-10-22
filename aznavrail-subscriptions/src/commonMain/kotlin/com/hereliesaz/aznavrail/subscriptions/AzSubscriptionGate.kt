package com.hereliesaz.aznavrail.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Entitlement states for the optional Google Play storefront.
 *
 * This component has no dependency on Google Play or Android. A platform-specific
 * billing adapter supplies state; no app content is composed unless [GRANTED].
 */
public enum class AzSubscriptionAccess { CHECKING, LOCKED, GRANTED, ERROR }

/** A customer-eligible offer, with localized prices supplied by the storefront. */
public data class AzSubscriptionOffer(
    val id: String,
    val label: String,
    val price: String,
    val terms: String,
)

/**
 * Display state used by [AzSubscriptionGate].
 *
 * [message] is diagnostic/help text and must not be used as an entitlement.
 */
public data class AzSubscriptionUiState(
    val access: AzSubscriptionAccess = AzSubscriptionAccess.CHECKING,
    val appName: String,
    val offers: List<AzSubscriptionOffer> = emptyList(),
    val message: String? = null,
)

/** Storefront-independent purchase observation, for deterministic entitlement checks. */
internal data class AzPurchaseSnapshot(
    val productId: String,
    val purchased: Boolean,
)

/**
 * True only for an exact matching, completed purchase in the latest successful
 * storefront query. A pending purchase, another SKU, or an empty query cannot unlock.
 */
internal fun hasActivePurchase(productId: String, purchases: List<AzPurchaseSnapshot>): Boolean =
    productId.isNotBlank() && purchases.any { it.productId == productId && it.purchased }

/**
 * Whole-application subscription boundary. The [content] lambda is not even composed
 * while access is unknown, failed, or denied; callers must wrap their entire root.
 *
 * On Android/Play, [AzPlaySubscriptionGate] feeds this UI from Play Billing. Other
 * platforms can supply their own state without importing Google Play classes.
 *
 * [onSubscribe] receives the opaque offer token's UI identifier, not a price.
 * The price/renewal/trial text must come from the store, never hard-coded.
 */
@Composable
public fun AzSubscriptionGate(
    state: AzSubscriptionUiState,
    onSubscribe: (String) -> Unit,
    onRestore: () -> Unit,
    onManage: () -> Unit,
    onRetry: () -> Unit,
    onGitHub: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (state.access == AzSubscriptionAccess.GRANTED) {
        content()
        return
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(state.appName, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("The full app is also free on GitHub.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            when (state.access) {
                AzSubscriptionAccess.CHECKING ->
                    Text("Checking your Google Play subscription…")
                AzSubscriptionAccess.ERROR -> {
                    Text(state.message ?: "Google Play billing is unavailable.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) { Text("Try again") }
                }
                AzSubscriptionAccess.LOCKED -> {
                    Text(state.message ?: "Subscribe through Google Play to continue.")
                    Spacer(Modifier.height(16.dp))
                    state.offers.forEach { offer ->
                        Button(
                            onClick = { onSubscribe(offer.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("${offer.label} — ${offer.price}") }
                        Text(offer.terms, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (state.offers.isEmpty()) {
                        Text("No subscription offer available. Check your Play Store account and try again.")
                        OutlinedButton(onClick = onRetry) { Text("Refresh offers") }
                    }
                }
                AzSubscriptionAccess.GRANTED -> Unit
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRestore) { Text("Restore purchase") }
            OutlinedButton(onClick = onManage) { Text("Manage or cancel subscription") }
            OutlinedButton(onClick = onGitHub) { Text("Get the free GitHub edition") }
            Spacer(Modifier.height(8.dp))
            Text(
                "Google Play subscriptions renew automatically until canceled. " +
                    "Trials, eligibility, renewal frequency, and localized price appear above " +
                    "and in Google's checkout before you confirm.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
