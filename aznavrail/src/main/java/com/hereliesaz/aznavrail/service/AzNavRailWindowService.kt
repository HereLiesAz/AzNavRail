package com.hereliesaz.aznavrail.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.roundToInt

val LocalAzNavRailOverlayController = compositionLocalOf<AzNavRailOverlayController?> { null }

interface AzNavRailOverlayController {
    val contentOffset: State<IntOffset>
    fun onDragStart()
    fun onDrag(dragAmount: Offset)
    fun onDragEnd()
}

/**
 * Base service class for creating a system overlay with AzNavRail using only [android.permission.SYSTEM_ALERT_WINDOW].
 *
 * To use this, extend this class and implement [OverlayContent].
 * You must declare the [android.permission.SYSTEM_ALERT_WINDOW] permission in your manifest.
 *
 * This service does NOT start a foreground service. For a persistent overlay that is less likely to be killed,
 * use [AzNavRailOverlayService] instead.
 */
abstract class AzNavRailWindowService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    /**
     * The Composable content to display in the overlay.
     * Note: You should wrap your content in your app's Theme to ensure proper styling.
     */
    @Composable
    abstract fun OverlayContent()

    protected open val windowParams: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private inner class ControllerImpl : AzNavRailOverlayController {
        private val _contentOffset = mutableStateOf(IntOffset.Zero)
        override val contentOffset: State<IntOffset> = _contentOffset

        override fun onDragStart() {
            if (composeView != null && windowManager != null) {
                // Save current position
                val currentX = windowParams.x
                val currentY = windowParams.y

                // Set content offset to current position so it doesn't jump
                _contentOffset.value = IntOffset(currentX, currentY)

                // Make window full screen
                windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
                windowParams.height = WindowManager.LayoutParams.MATCH_PARENT
                windowParams.x = 0
                windowParams.y = 0
                windowManager?.updateViewLayout(composeView, windowParams)
            }
        }

        override fun onDrag(dragAmount: Offset) {
            val current = _contentOffset.value
            _contentOffset.value = IntOffset(
                (current.x + dragAmount.x).roundToInt(),
                (current.y + dragAmount.y).roundToInt()
            )
        }

        override fun onDragEnd() {
            if (composeView != null && windowManager != null) {
                // Get final position
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                // Clamp final position to ensure at least some part of the window is visible
                // Allow dragging mostly off-screen but keep a margin visible
                val margin = 100 // px
                // Ensure the window is not lost off-screen.
                // Assuming standard gravity (Top|Start), (x,y) is the top-left corner.
                // We allow negative coordinates up to -margin to let the user drag the rail partially off-screen (left/top).
                // We also cap at screenWidth/Height - margin to ensure it doesn't disappear off the bottom/right.
                val safeX = _contentOffset.value.x.coerceIn(-margin, screenWidth - margin)
                val safeY = _contentOffset.value.y.coerceIn(-margin, screenHeight - margin)

                val finalX = safeX
                val finalY = safeY

                // Reset content offset
                _contentOffset.value = IntOffset.Zero

                // Set window back to WRAP_CONTENT at new position
                windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                windowParams.x = finalX
                windowParams.y = finalY
                windowManager?.updateViewLayout(composeView, windowParams)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Create a ComposeView. We don't apply a theme wrapper here because we don't know the app's theme.
        // The user should wrap their content in your theme.
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AzNavRailWindowService)
            setViewTreeSavedStateRegistryOwner(this@AzNavRailWindowService)
            setViewTreeViewModelStoreOwner(this@AzNavRailWindowService)
            setContent {
                val controller = remember { ControllerImpl() }
                CompositionLocalProvider(LocalAzNavRailOverlayController provides controller) {
                    OverlayContent()
                }
            }
        }

        windowManager?.addView(composeView, windowParams)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun updatePosition(x: Float, y: Float) {
        if (composeView != null && windowManager != null) {
            windowParams.x += x.toInt()
            windowParams.y += y.toInt()
            windowManager?.updateViewLayout(composeView, windowParams)
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (composeView != null) {
            windowManager?.removeView(composeView)
        }
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
