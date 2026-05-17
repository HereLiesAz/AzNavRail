package com.hereliesaz.aznavrail.bottomsheet

import android.app.Application
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
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
}
