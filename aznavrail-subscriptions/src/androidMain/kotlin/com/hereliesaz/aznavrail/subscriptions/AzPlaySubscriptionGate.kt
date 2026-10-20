package com.hereliesaz.aznavrail.subscriptions

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Per-app Play Console configuration. [productId] is a SUBS product (not a base plan).
 * [basePlanIds] optionally limits which monthly/yearly plans are offered.
 *
 * Create trial offers in Play Console, not with a device clock. Prices, eligible
 * free trials, renewal cadence and offer tokens are always read from Play.
 */
public data class AzPlaySubscriptionConfig(
    val productId: String,
    val appName: String,
    val githubUrl: String,
    val basePlanIds: Set<String> = emptySet(),
) {
    init {
        require(productId.isNotBlank()) { "Set the Play Console subscription product ID" }
        require(appName.isNotBlank()) { "Set an app display name" }
        require(githubUrl.startsWith("https://github.com/")) { "Use a public HTTPS GitHub URL" }
    }
}

/**
 * Play-only root composition. Put this call in `src/play`, not `src/main`.
 *
 * The entire [content] is withheld until Google Play reports an active PURCHASED
 * subscription with the configured product ID. PENDING never unlocks.
 *
 * IMPORTANT: this is a client-only entitlement adapter. For high-value/hosted
 * benefits, verify purchase tokens and subscription lifecycle server-side.
 * No client-only Android code can prevent a modified APK bypassing the gate.
 */
@Composable
public fun AzPlaySubscriptionGate(
    activity: Activity,
    config: AzPlaySubscriptionConfig,
    content: @Composable () -> Unit,
) {
    val session = remember(activity, config) { PlaySubscriptionSession(activity, config) }
    DisposableEffect(session) {
        session.start()
        onDispose { session.close() }
    }
    AzSubscriptionGate(
        state = session.state.value,
        onSubscribe = session::purchase,
        onRestore = session::restore,
        onManage = session::manage,
        onRetry = session::restore,
        onGitHub = session::github,
        content = content,
    )
}

private class PlaySubscriptionSession(
    private val activity: Activity,
    private val config: AzPlaySubscriptionConfig,
) {
    private data class PlayOffer(
        val product: ProductDetails,
        val detail: ProductDetails.SubscriptionOfferDetails,
    )

    private val editableState = mutableStateOf(AzSubscriptionUiState(appName = config.appName))
    val state: State<AzSubscriptionUiState> get() = editableState
    private var closed = false
    private var connecting = false
    private var querySerial = 0
    private var purchasable: Map<String, PlayOffer> = emptyMap()

    private val client: BillingClient = BillingClient.newBuilder(activity.applicationContext)
        .setListener(PurchasesUpdatedListener { result, _ ->
            onMain {
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> refresh()
                    BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                    else -> editableState.value = editableState.value.copy(
                        access = AzSubscriptionAccess.ERROR,
                        message = "Google Play could not complete the purchase. Please retry.",
                    )
                }
            }
        })
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(resumed: Activity) {
            if (resumed === activity) restore()
        }
        override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
        override fun onActivityStarted(a: Activity) = Unit
        override fun onActivityPaused(a: Activity) = Unit
        override fun onActivityStopped(a: Activity) = Unit
        override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
        override fun onActivityDestroyed(a: Activity) = Unit
    }

    fun start() {
        activity.application.registerActivityLifecycleCallbacks(callbacks)
        connect()
    }

    fun close() {
        if (closed) return
        closed = true
        querySerial++
        activity.application.unregisterActivityLifecycleCallbacks(callbacks)
        client.endConnection()
    }

    private fun onMain(block: () -> Unit) {
        activity.runOnUiThread { if (!closed) block() }
    }

    private fun connect() {
        if (closed || connecting) return
        if (client.isReady) {
            refresh()
            loadOffers()
            return
        }
        connecting = true
        editableState.value = editableState.value.copy(
            access = AzSubscriptionAccess.CHECKING,
            message = null,
        )
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                onMain {
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        refresh()
                        loadOffers()
                    } else {
                        editableState.value = editableState.value.copy(
                            access = AzSubscriptionAccess.ERROR,
                            message = "Google Play Billing is unavailable. Check your connection and retry.",
                        )
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                // BillingClient 9 reconnects automatically when an API request is made.
                onMain { connecting = false }
            }
        })
    }

    fun restore() {
        if (closed) return
        if (!client.isReady) {
            connect()
            return
        }
        refresh()
        loadOffers()
    }

    private fun refresh() {
        if (!client.isReady) {
            connect()
            return
        }
        val requestId = ++querySerial
        // Do not discard an already-composed navigation tree on every resume.
        // The fresh query will revoke access if Play no longer reports entitlement.
        if (editableState.value.access != AzSubscriptionAccess.GRANTED) {
            editableState.value = editableState.value.copy(
                access = AzSubscriptionAccess.CHECKING,
                message = null,
            )
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            onMain {
                if (requestId != querySerial) return@onMain
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    editableState.value = editableState.value.copy(
                        access = AzSubscriptionAccess.ERROR,
                        message = "Could not verify your subscription with Google Play.",
                    )
                    return@onMain
                }
                val snapshots = purchases.flatMap { purchase ->
                    purchase.products.map { id ->
                        AzPurchaseSnapshot(id, purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                    }
                }
                val unlocked = hasActivePurchase(config.productId, snapshots)
                editableState.value = editableState.value.copy(
                    access = if (unlocked) AzSubscriptionAccess.GRANTED else AzSubscriptionAccess.LOCKED,
                    message = if (unlocked) null else if (purchases.any {
                        config.productId in it.products && it.purchaseState == Purchase.PurchaseState.PENDING
                    }) "Payment pending. Access begins after Google Play confirms payment." else null,
                )
                purchases.filter {
                    config.productId in it.products &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        !it.isAcknowledged
                }.forEach { purchase ->
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    client.acknowledgePurchase(params) { _ ->
                        // A failed acknowledgment is retried on the next successful restore.
                    }
                }
            }
        }
    }

    private fun loadOffers() {
        if (!client.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(config.productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        client.queryProductDetailsAsync(params) { result, response ->
            onMain {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    purchasable = emptyMap()
                    editableState.value = editableState.value.copy(offers = emptyList())
                    return@onMain
                }
                val available = response.productDetailsList
                    .filter { it.productId == config.productId }
                    .flatMap { product ->
                        product.subscriptionOfferDetails.orEmpty().map { PlayOffer(product, it) }
                    }
                    .filter {
                        config.basePlanIds.isEmpty() || it.detail.basePlanId in config.basePlanIds
                    }
                    // Only monthly/yearly auto-renewing plans and simple 3/7-day
                    // trial phases: other price ladders need more elaborate terms.
                    .filter { offer ->
                        val phases = offer.detail.pricingPhases.pricingPhaseList
                        val period = phases.lastOrNull()?.billingPeriod
                        (period == "P1M" || period == "P1Y") &&
                            (phases.size == 1 || (
                                phases.size == 2 &&
                                    phases.first().priceAmountMicros == 0L &&
                                    phases.first().billingPeriod in setOf("P3D", "P7D")
                                ))
                    }
                    .sortedByDescending { offer ->
                        offer.detail.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                    }
                    .distinctBy { it.detail.basePlanId }
                purchasable = available.associateBy { it.detail.offerToken }
                val display = available.mapNotNull { offer ->
                    val phases = offer.detail.pricingPhases.pricingPhaseList
                    val renewal = phases.lastOrNull() ?: return@mapNotNull null
                    val cadence = when (renewal.billingPeriod) {
                        "P1M" -> "month"
                        "P1Y" -> "year"
                        else -> renewal.billingPeriod
                    }
                    val label = when (renewal.billingPeriod) {
                        "P1M" -> "Monthly"
                        "P1Y" -> "Yearly"
                        else -> offer.detail.basePlanId
                    }
                    val trial = phases.firstOrNull {
                        it.priceAmountMicros == 0L && it !== renewal
                    }
                    val trialText = trial?.let {
                        val duration = when (it.billingPeriod) {
                            "P3D" -> "3-day"
                            "P7D" -> "7-day"
                            "P1M" -> "1-month"
                            else -> it.billingPeriod
                        }
                        "$duration free trial; "
                    }.orEmpty()
                    AzSubscriptionOffer(
                        id = offer.detail.offerToken,
                        label = label,
                        price = renewal.formattedPrice,
                        terms = "${trialText}then ${renewal.formattedPrice}/$cadence. " +
                            "Automatically renews until canceled.",
                    )
                }
                editableState.value = editableState.value.copy(offers = display)
            }
        }
    }

    fun purchase(id: String) {
        val selected = purchasable[id] ?: run {
            restore()
            return
        }
        if (!client.isReady) {
            restore()
            return
        }
        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(selected.product)
            .setOfferToken(selected.detail.offerToken)
            .build()
        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(product))
                .build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            editableState.value = editableState.value.copy(
                access = AzSubscriptionAccess.ERROR,
                message = "Google Play checkout could not open. Please retry.",
            )
        }
    }

    fun manage() {
        val url = Uri.parse("https://play.google.com/store/account/subscriptions")
            .buildUpon()
            .appendQueryParameter("sku", config.productId)
            .appendQueryParameter("package", activity.packageName)
            .build()
        activity.startActivity(Intent(Intent.ACTION_VIEW, url))
    }

    fun github() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(config.githubUrl)))
    }
}
