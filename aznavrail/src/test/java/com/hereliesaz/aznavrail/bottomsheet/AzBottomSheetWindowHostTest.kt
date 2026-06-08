package com.hereliesaz.aznavrail.bottomsheet

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AzBottomSheetWindowHostTest {

    private class TestOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        val registry = LifecycleRegistry(this)
        private val ssr = SavedStateRegistryController.create(this).apply {
            performRestore(null)
        }
        private val vms = ViewModelStore()
        override val lifecycle get() = registry
        override val viewModelStore: ViewModelStore get() = vms
        override val savedStateRegistry: SavedStateRegistry get() = ssr.savedStateRegistry
    }

    private fun newContext(): Context = RuntimeEnvironment.getApplication() as Application

    /** Flush pending snapshot writes (so snapshotFlow re-emits) and drain the main looper. */
    private fun idleMain() {
        Snapshot.sendApplyNotifications()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun attach_addsOverlayWindow_andDetachRemovesIt() {
        val ctx = newContext()
        val owner = TestOwner().also { it.registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        val controller = AzSheetController(initial = AzSheetDetent.HIDDEN)
        val host = AzBottomSheetWindowHost(
            context = ctx,
            controller = controller,
            config = AzSheetConfig(),
            lifecycleOwner = owner,
            viewModelStoreOwner = owner,
            savedStateRegistryOwner = owner,
        ) { Box(modifier = androidx.compose.ui.Modifier) {} }

        assertFalse(host.isAttached())
        host.attach()
        assertTrue(host.isAttached())
        val params = host.currentParams()
        assertNotNull(params)
        assertEquals(android.view.Gravity.BOTTOM, params!!.gravity)
        // FLAG_NOT_FOCUSABLE is set initially because detent is HIDDEN.
        assertTrue((params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0)

        host.detach()
        assertFalse(host.isAttached())
    }

    @Test
    fun attach_isIdempotent() {
        val ctx = newContext()
        val owner = TestOwner().also { it.registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        val controller = AzSheetController()
        val host = AzBottomSheetWindowHost(
            context = ctx,
            controller = controller,
            lifecycleOwner = owner,
            viewModelStoreOwner = owner,
            savedStateRegistryOwner = owner,
        ) { Box(modifier = androidx.compose.ui.Modifier) {} }
        host.attach()
        host.attach() // second call must not throw
        assertTrue(host.isAttached())
        host.detach()
    }

    @Test
    fun attachNavBarDecor_isNoopWhenHeightZero() {
        val ctx = newContext()
        val owner = TestOwner().also { it.registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        val controller = AzSheetController()
        val host = AzBottomSheetWindowHost(
            context = ctx,
            controller = controller,
            lifecycleOwner = owner,
            viewModelStoreOwner = owner,
            savedStateRegistryOwner = owner,
            navBarHeightPx = 0,
        ) { Box(modifier = androidx.compose.ui.Modifier) {} }
        host.attachNavBarDecor()
        assertFalse(host.isNavBarDecorAttached())
    }

    @Test
    fun updateConfig_resizesLiveWindow_atPeek() {
        val ctx = newContext()
        val owner = TestOwner().also { it.registry.currentState = Lifecycle.State.RESUMED }
        val controller = AzSheetController(initial = AzSheetDetent.PEEK)
        val host = AzBottomSheetWindowHost(
            context = ctx,
            controller = controller,
            config = AzSheetConfig(peekDp = 100.dp),
            lifecycleOwner = owner,
            viewModelStoreOwner = owner,
            savedStateRegistryOwner = owner,
        ) { Box(modifier = androidx.compose.ui.Modifier) {} }

        host.attach()
        idleMain()
        val density = ctx.resources.displayMetrics.density
        assertEquals(
            "Initial PEEK window height must match the starting peekDp (100.dp).",
            (100f * density).toInt(),
            host.currentParams()!!.height,
        )

        host.updateConfig(AzSheetConfig(peekDp = 240.dp))
        idleMain()
        assertEquals(
            "updateConfig() with a larger peekDp must resize the live overlay window at PEEK " +
                "without waiting for a detent change — the attach() collector should react to " +
                "configState via snapshotFlow.",
            (240f * density).toInt(),
            host.currentParams()!!.height,
        )

        host.detach()
    }

    @Test
    fun updateConfig_resizesLiveWindow_atHidden() {
        val ctx = newContext()
        val owner = TestOwner().also { it.registry.currentState = Lifecycle.State.RESUMED }
        val controller = AzSheetController(initial = AzSheetDetent.HIDDEN)
        val host = AzBottomSheetWindowHost(
            context = ctx,
            controller = controller,
            config = AzSheetConfig(hiddenStripDp = 16.dp),
            lifecycleOwner = owner,
            viewModelStoreOwner = owner,
            savedStateRegistryOwner = owner,
        ) { Box(modifier = androidx.compose.ui.Modifier) {} }

        host.attach()
        idleMain()
        val density = ctx.resources.displayMetrics.density

        host.updateConfig(AzSheetConfig(hiddenStripDp = 64.dp))
        idleMain()
        assertEquals(
            "updateConfig() with a larger hiddenStripDp must resize the live overlay window at HIDDEN.",
            (64f * density).toInt(),
            host.currentParams()!!.height,
        )

        host.detach()
    }

    @Test
    fun attach_wiresWindowInsets_withoutConsumingThem() {
        val ctx = newContext()
        val owner = TestOwner().also { it.registry.currentState = Lifecycle.State.RESUMED }
        val controller = AzSheetController()
        val host = AzBottomSheetWindowHost(
            context = ctx,
            controller = controller,
            lifecycleOwner = owner,
            viewModelStoreOwner = owner,
            savedStateRegistryOwner = owner,
        ) { Box(modifier = androidx.compose.ui.Modifier) {} }

        host.attach()
        val view = host.sheetViewForTest()
        assertNotNull("attach() must create the sheet view.", view)

        val navBarBottom = 48
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navBarBottom))
            .build()
        val result = ViewCompat.dispatchApplyWindowInsets(view!!, insets)

        assertEquals(
            "The sheet view's inset listener must deliver the navigation-bar inset to the content " +
                "(so Modifier.navigationBarsPadding() resolves) — recorded inset mismatched.",
            navBarBottom,
            host.lastNavBarInsetPx(),
        )
        assertEquals(
            "The inset listener must NOT consume the navigation-bar inset — the app below still needs it.",
            navBarBottom,
            result.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom,
        )

        host.detach()
    }
}
