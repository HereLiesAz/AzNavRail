package com.hereliesaz.aznavrail.internal

import android.app.Application
import android.content.Context
import android.provider.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AzNavModeTest {

    private fun context(): Context = RuntimeEnvironment.getApplication() as Application

    @Test
    fun gesturalMode_isGestureNav() {
        val ctx = context()
        Settings.Secure.putInt(ctx.contentResolver, "navigation_mode", 2)
        assertTrue(
            "navigation_mode=2 is gesture navigation; AzNavMode.isGestureNav must return true.",
            AzNavMode.isGestureNav(ctx),
        )
        assertFalse(
            "isButtonNav must be the inverse of isGestureNav.",
            AzNavMode.isButtonNav(ctx),
        )
    }

    @Test
    fun threeButtonMode_isButtonNav() {
        val ctx = context()
        Settings.Secure.putInt(ctx.contentResolver, "navigation_mode", 0)
        assertFalse(
            "navigation_mode=0 is 3-button navigation; isGestureNav must be false.",
            AzNavMode.isGestureNav(ctx),
        )
        assertTrue(AzNavMode.isButtonNav(ctx))
    }

    @Test
    fun twoButtonMode_isButtonNav() {
        val ctx = context()
        Settings.Secure.putInt(ctx.contentResolver, "navigation_mode", 1)
        assertFalse(
            "navigation_mode=1 is 2-button navigation; isGestureNav must be false.",
            AzNavMode.isGestureNav(ctx),
        )
    }

    @Test
    fun missingKey_defaultsToButtonNav() {
        val ctx = context()
        // The key is unset on a fresh Robolectric environment; AzNavMode must default to button
        // navigation (the conservative choice that preserves the library's existing margins).
        assertFalse(
            "When navigation_mode is unset, isGestureNav must default to false (button nav).",
            AzNavMode.isGestureNav(ctx),
        )
        assertTrue(AzNavMode.isButtonNav(ctx))
    }
}
