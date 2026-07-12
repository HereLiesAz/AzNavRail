// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/AzNavBarDecorWindow.kt
package com.hereliesaz.aznavrail.internal

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hereliesaz.aznavrail.bottomsheet.AzSheetController
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent

/**
 * Alpha the navigation-bar decoration paints at. When the sheet opts into
 * [AzSheetConfig.drawBehindNavBar] and the device uses button navigation, the decoration is
 * capped at a semi-transparent value so the sheet window behind it shows through (the
 * "draw behind the nav bar" look); otherwise it uses the full [AzSheetConfig.backgroundAlpha].
 */
internal fun decorAlphaFor(config: AzSheetConfig, buttonNav: Boolean): Float =
    if (config.drawBehindNavBar && buttonNav) minOf(config.backgroundAlpha, 0.5f) else config.backgroundAlpha

/**
 * Manages a `TYPE_ACCESSIBILITY_OVERLAY` window painted on top of the system navigation bar so
 * its color visually blends with an [com.hereliesaz.aznavrail.bottomsheet.AzBottomSheetWindowHost].
 * The view is non-interactive (`FLAG_NOT_TOUCHABLE`); taps and gesture-nav swipes pass through
 * to the system as normal.
 *
 * Adding a `TYPE_ACCESSIBILITY_OVERLAY` window requires a bound accessibility service; consumers
 * therefore attach this *after* their service is bound.
 *
 * @param context The hosting context (usually the consumer's Service).
 * @param controller Drives visibility — the decoration only paints when the sheet is non-HIDDEN
 *   and enabled.
 * @param navBarHeightPx Height of the system navigation bar in pixels.
 * @param configProvider Lambda yielding the current [AzSheetConfig]; called each frame so opacity
 *   and color changes track the sheet.
 * @param buttonNav Whether the device uses button (3-button / 2-button) navigation. When `true`
 *   and the config opts into [AzSheetConfig.drawBehindNavBar], the decoration paints
 *   semi-transparent so the sheet shows through behind the nav bar.
 * @param lifecycleOwner / [viewModelStoreOwner] / [savedStateRegistryOwner] Compose-view owners
 *   the consumer provides (typically the Service implementing the relevant interfaces).
 */
internal class AzNavBarDecorWindow(
    private val context: Context,
    private val controller: AzSheetController,
    private val navBarHeightPx: Int,
    private val configProvider: () -> AzSheetConfig,
    private val buttonNav: Boolean,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
) {
    private var view: View? = null

    fun attach() {
        if (view != null) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            setContent {
                val detent by controller.detentFlow.collectAsState()
                val enabled by controller.enabledFlow.collectAsState()
                val cfg = configProvider()
                val visible = enabled && detent != AzSheetDetent.HIDDEN
                val resolvedBg = if (cfg.backgroundColor.value == Color.Unspecified.value) Color.Black else cfg.backgroundColor
                val color = if (visible) resolvedBg.copy(alpha = decorAlphaFor(cfg, buttonNav)) else Color.Transparent
                Box(Modifier.fillMaxSize().background(color))
            }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            navBarHeightPx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM }
        wm.addView(composeView, params)
        view = composeView
    }

    fun detach() {
        val v = view ?: return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { wm.removeView(v) }
        view = null
    }
}
