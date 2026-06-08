// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/bottomsheet/AzBottomSheetWindowHost.kt
package com.hereliesaz.aznavrail.bottomsheet

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hereliesaz.aznavrail.internal.AzBottomSheetShell
import com.hereliesaz.aznavrail.internal.AzNavBarDecorWindow
import com.hereliesaz.aznavrail.internal.AzNavMode
import com.hereliesaz.aznavrail.internal.heightForDetent
import com.hereliesaz.aznavrail.internal.navBarExtensionPx
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Hosts an [AzBottomSheet] inside a `TYPE_APPLICATION_OVERLAY` `WindowManager` window so the sheet
 * can float over the active foreground application, the same way LogKitty's overlay service does.
 *
 * The library ships no `Service`. Consumers are expected to instantiate this from their own
 * foreground service (or any context that has the `SYSTEM_ALERT_WINDOW` permission) and forward
 * lifecycle callbacks: [attach] from `onCreate`, [detach] from `onDestroy`. If you also need the
 * navigation-bar color sync, call [attachNavBarDecor] once your accessibility service is bound.
 *
 * Visual behavior is identical to the in-tree [AzBottomSheet] but the window itself hard-jumps
 * between detent heights (no `animateDpAsState`) — matching LogKitty's existing look frame-for-frame.
 *
 * @param context A context with [WindowManager] available (your Service).
 * @param controller Shared controller (use [AzSheetController] directly; no `remember` needed since
 *   the window's lifecycle is service-scoped).
 * @param config Static configuration; pass [updateConfig] later to change live.
 * @param lifecycleOwner The owner whose [Lifecycle] drives the detent collector.
 * @param viewModelStoreOwner Wired into the [ComposeView] tree.
 * @param savedStateRegistryOwner Wired into the [ComposeView] tree.
 * @param navBarHeightPx Pixel height of the system navigation bar; used only by [attachNavBarDecor].
 *   Defaults to 0; query `resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android"))`.
 * @param content Sheet content. Same contract as [AzBottomSheet]'s content slot.
 */
class AzBottomSheetWindowHost(
    private val context: Context,
    private val controller: AzSheetController,
    config: AzSheetConfig = AzSheetConfig(),
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val navBarHeightPx: Int = 0,
    private val content: @Composable BoxScope.() -> Unit,
) {

    private val configState = mutableStateOf(config)
    private var sheetView: ComposeView? = null
    private var sheetParams: WindowManager.LayoutParams? = null
    private var navBarDecor: AzNavBarDecorWindow? = null
    private var lastNavBarInsetPx: Int = 0
    // Pixels the overlay window grows downward into the nav-bar region (drawBehindNavBar + button
    // nav). Backed by a Compose state so the shell extends the card in lock-step with the window.
    private val navBarExtensionState = mutableStateOf(0)
    private var collectJob: kotlinx.coroutines.Job? = null

    /**
     * Adds the sheet's overlay window. Idempotent: a second call without an intervening [detach] is a no-op.
     */
    fun attach() {
        if (sheetView != null) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            setContent {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val extensionDp = with(LocalDensity.current) { navBarExtensionState.value.toDp() }
                    AzBottomSheetShell(
                        controller = controller,
                        config = configState.value,
                        parentHeight = maxHeight,
                        animate = false,
                        onSwipeLeft = null,
                        onSwipeRight = null,
                        navBarExtensionDp = extensionDp,
                        content = content,
                    )
                }
            }
            setOnKeyListener listener@{ _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP &&
                    configState.value.collapseOnBack && controller.detent != AzSheetDetent.HIDDEN
                ) {
                    controller.stepDown()
                    return@listener true
                }
                false
            }
        }

        // Wire window insets through the outer ComposeView so the inner AndroidComposeView's
        // own WindowInsetsHolder still receives them and WindowInsets.navigationBars /
        // Modifier.navigationBarsPadding() resolve inside the content. We record the nav-bar
        // bottom inset (for tests/diagnostics) but return the insets unchanged — never consumed,
        // so both the Compose tree and the app below keep seeing them.
        ViewCompat.setOnApplyWindowInsetsListener(composeView) { v, insets ->
            lastNavBarInsetPx = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            // Once we know the real nav-bar inset, (re)derive how far the window must grow behind
            // the bar and resize it immediately — the initial layout may have run before insets
            // were available, so HIDDEN/PEEK strips would otherwise stop above the bar.
            val ext = resolveExtensionPx(configState.value)
            if (ext != navBarExtensionState.value) {
                navBarExtensionState.value = ext
                sheetParams?.let { p ->
                    p.height = windowHeightFor(controller.detent, configState.value)
                    runCatching { wm.updateViewLayout(composeView, p) }
                }
            }
            ViewCompat.onApplyWindowInsets(v, insets)
            insets
        }

        navBarExtensionState.value = resolveExtensionPx(configState.value)
        val params = buildParams(configState.value, controller.detent)
        wm.addView(composeView, params)
        sheetView = composeView
        sheetParams = params
        // Kick an initial dispatch so the overlay window picks up insets without waiting for a
        // system change (rotation, IME, etc.).
        ViewCompat.requestApplyInsets(composeView)

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Combine in configState (via snapshotFlow) alongside the detent so updateConfig()
                // re-applies the WindowManager layout immediately, not just on the next detent change.
                combine(
                    controller.detentFlow,
                    controller.enabledFlow,
                    snapshotFlow { configState.value },
                ) { detent, _, config -> detent to config }
                    .collect { (detent, config) ->
                        val current = sheetParams ?: return@collect
                        // Keep the card's downward extension in sync with config changes (e.g.
                        // toggling drawBehindNavBar live) before resizing the window.
                        navBarExtensionState.value = resolveExtensionPx(config)
                        // For HIDDEN/PEEK the height is absolute Dp (plus the nav-bar extension);
                        // for HALF/FULL we use MATCH_PARENT and let the shell fill via fractions.
                        val needsFocus = detent == AzSheetDetent.HALF || detent == AzSheetDetent.FULL
                        current.height = windowHeightFor(detent, config)
                        current.flags = baseFlags(needsFocus)
                        runCatching { wm.updateViewLayout(composeView, current) }
                    }
            }
        }
    }

    /** Removes the sheet (and decor) overlay windows. Safe to call multiple times. */
    fun detach() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        sheetView?.let { runCatching { wm.removeView(it) } }
        sheetView = null
        sheetParams = null
        lastNavBarInsetPx = 0
        navBarExtensionState.value = 0
        collectJob?.cancel()
        collectJob = null
        navBarDecor?.detach()
        navBarDecor = null
    }

    /**
     * Updates the live config without rebuilding the window. Content effects apply on the next
     * recomposition; in addition, while the sheet is attached at [AzSheetDetent.HIDDEN] or
     * [AzSheetDetent.PEEK] the overlay window is resized immediately to match the new
     * `hiddenStripDp`/`peekDp` (HALF/FULL stay `MATCH_PARENT`).
     */
    fun updateConfig(config: AzSheetConfig) {
        configState.value = config
    }

    /**
     * Adds the separate navigation-bar decoration window (a `TYPE_ACCESSIBILITY_OVERLAY` that
     * tints the system nav bar to match the sheet). Call from your accessibility service's
     * `onServiceConnected`, never before. Idempotent.
     */
    fun attachNavBarDecor() {
        if (navBarDecor != null || navBarHeightPx <= 0) return
        navBarDecor = AzNavBarDecorWindow(
            context = context,
            controller = controller,
            navBarHeightPx = navBarHeightPx,
            configProvider = { configState.value },
            buttonNav = AzNavMode.isButtonNav(context),
            lifecycleOwner = lifecycleOwner,
            viewModelStoreOwner = viewModelStoreOwner,
            savedStateRegistryOwner = savedStateRegistryOwner,
        ).also { it.attach() }
    }

    private fun buildParams(config: AzSheetConfig, detent: AzSheetDetent): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        val needsFocus = detent == AzSheetDetent.HALF || detent == AzSheetDetent.FULL
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            windowHeightFor(detent, config),
            type,
            baseFlags(needsFocus),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
            // Anchor flush to the screen bottom; the height already includes any nav-bar extension.
            y = 0
            @Suppress("DEPRECATION")
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            // Allow the window to lay out into the display cutout / system-bar area so the extended
            // height can actually occupy the nav-bar region (paired with FLAG_LAYOUT_NO_LIMITS).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    /**
     * Window height (px) for a [detent]. HIDDEN/PEEK resolve to an absolute pixel height; HALF/FULL
     * use `MATCH_PARENT` and let the shell fill via fractions. When [AzSheetConfig.drawBehindNavBar]
     * is active on button-nav, the absolute heights grow by the nav-bar inset so the window extends
     * behind the bar (the top edge stays put because the window is anchored `Gravity.BOTTOM`).
     */
    private fun windowHeightFor(detent: AzSheetDetent, config: AzSheetConfig): Int = when (detent) {
        AzSheetDetent.HIDDEN, AzSheetDetent.PEEK -> {
            val d = context.resources.displayMetrics.density
            (heightForDetent(detent, 0.dp, config).value * d).toInt() + resolveExtensionPx(config)
        }
        AzSheetDetent.HALF, AzSheetDetent.FULL -> WindowManager.LayoutParams.MATCH_PARENT
    }

    /**
     * Pixels by which the window grows downward into the nav-bar region. Uses the live measured
     * navigation-bar inset when available (the authoritative button-vs-gesture signal), falling back
     * to the consumer-supplied [navBarHeightPx] under button navigation before insets first arrive.
     */
    private fun resolveExtensionPx(config: AzSheetConfig): Int {
        val measured = lastNavBarInsetPx
        val isButton = AzNavMode.isButtonNav(context)
        val inset = if (measured > 0) measured else if (isButton) navBarHeightPx else 0
        return navBarExtensionPx(
            drawBehindNavBar = config.drawBehindNavBar,
            buttonNav = isButton,
            navBarInsetPx = inset,
        )
    }

    private fun baseFlags(focusable: Boolean): Int {
        var f = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (!focusable) {
            f = f or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        }
        return f
    }

    /** Exposed for tests. */
    internal fun currentParams(): WindowManager.LayoutParams? = sheetParams

    /** Exposed for tests. */
    internal fun isAttached(): Boolean = sheetView != null

    /** Exposed for tests. */
    internal fun isNavBarDecorAttached(): Boolean = navBarDecor != null

    /** Exposed for tests: the last navigation-bar bottom inset (px) delivered to the sheet view. */
    internal fun lastNavBarInsetPx(): Int = lastNavBarInsetPx

    /** Exposed for tests: the px the window currently grows downward behind the nav bar. */
    internal fun navBarExtensionPxForTest(): Int = navBarExtensionState.value

    /** Exposed for tests: the live sheet view, or null when detached. */
    internal fun sheetViewForTest(): View? = sheetView

    @Suppress("unused")
    private val unusedView: View? get() = sheetView
}
