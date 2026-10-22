package com.hereliesaz.aznavrail.subscriptions

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Expected states are fixed independently of the matching implementation. */
class AzSubscriptionEntitlementTest {
    @Test fun activeMatchingSubscriptionGrantsAccess() {
        assertTrue(hasActivePurchase("az_premium", listOf(AzPurchaseSnapshot("az_premium", true))))
    }

    @Test fun pendingPaymentDoesNotGrantAccess() {
        assertFalse(hasActivePurchase("az_premium", listOf(AzPurchaseSnapshot("az_premium", false))))
    }

    @Test fun differentAppProductCannotUnlock() {
        assertFalse(hasActivePurchase("az_premium", listOf(AzPurchaseSnapshot("another_app", true))))
    }

    @Test fun revokedSubscriptionIsLockedAfterFreshQuery() {
        assertFalse(hasActivePurchase("az_premium", emptyList()))
    }

    @Test fun blankConfiguredSkuNeverUnlocks() {
        assertFalse(hasActivePurchase("", listOf(AzPurchaseSnapshot("", true))))
    }

    @Test fun otherPurchasesDoNotOverridePendingPayment() {
        assertFalse(
            hasActivePurchase(
                "az_premium",
                listOf(AzPurchaseSnapshot("another_app", true), AzPurchaseSnapshot("az_premium", false)),
            ),
        )
    }
}
