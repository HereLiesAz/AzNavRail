package com.hereliesaz.sampleapp

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import com.hereliesaz.aznavrail.model.AzSliderConfig
import com.hereliesaz.aznavrail.model.AzSliderSize
import com.hereliesaz.aznavrail.model.AzSliderVariant
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.tutorial.AzGuideShape
import com.hereliesaz.aznavrail.tutorial.AzInstructionStep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.sampleapp.screens.BottomSheetDemoScreen
import com.hereliesaz.sampleapp.screens.CustomizationDemoScreen
import com.hereliesaz.sampleapp.screens.CustomizationState
import com.hereliesaz.sampleapp.screens.FabOverlayDemoScreen
import com.hereliesaz.sampleapp.screens.FabOverlayState
import com.hereliesaz.sampleapp.screens.FormShowcaseScreen
import com.hereliesaz.sampleapp.screens.HelpSystemDemoScreen
import com.hereliesaz.sampleapp.screens.HelpSystemState
import com.hereliesaz.sampleapp.screens.HiddenMenuDemoScreen
import com.hereliesaz.sampleapp.screens.HiddenMenuDemoState
import com.hereliesaz.sampleapp.screens.LegacyRailDemoScreen
import com.hereliesaz.sampleapp.screens.ShowcaseHomeScreen
import com.hereliesaz.sampleapp.screens.StandaloneWidgetsScreen
import com.hereliesaz.sampleapp.screens.TutorialDemoScreen
import com.ramcosta.composedestinations.generated.destinations.FormsDestination
import com.ramcosta.composedestinations.generated.destinations.LegacyDestination
import com.ramcosta.composedestinations.generated.destinations.StandaloneWidgetsDestination
import com.hereliesaz.aznavrail.AzHostActivityLayout
import kotlinx.coroutines.launch
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.bottomsheet.rememberAzSheetController
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzComposableContent
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent

private const val TAG = "SampleApp"

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val currentDestinationEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentDestinationEntry?.destination?.route

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    LaunchedEffect(Unit) {
        Log.d(TAG, "Initializing SampleApp: setting suggestion limit to 3")
        AzTextBoxDefaults.setSuggestionLimit(3)
    }

    // Legacy rail/menu state preserved so the inline rail keeps working.
    var isOnline by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var packRailButtons by remember { mutableStateOf(false) }
    // Three of the four slider variants, driven from the rail without leaving it.
    var volume by remember { mutableFloatStateOf(0.4f) }
    var quality by remember { mutableFloatStateOf(2f) }
    var trim by remember { mutableFloatStateOf(0f) }
    var isDockingRight by remember { mutableStateOf(false) }
    var noMenu by remember { mutableStateOf(false) }
    var usePhysicalDocking by remember { mutableStateOf(false) }

    val railCycleOptions = remember { listOf("A", "B", "C", "D") }
    var railSelectedOption by remember { mutableStateOf(railCycleOptions.first()) }
    val menuCycleOptions = remember { listOf("X", "Y", "Z") }
    var menuSelectedOption by remember { mutableStateOf(menuCycleOptions.first()) }

    // Customization screen state — drives azConfig + azTheme.
    var customization by remember {
        mutableStateOf(
            CustomizationState(
                headerIconShape = AzHeaderIconShape.CIRCLE,
                defaultShape = AzButtonShape.RECTANGLE,
                translucentBackground = Color.Unspecified,
                expandedWidth = 160.dp,
                collapsedWidth = 100.dp,
                railItemWidth = Dp.Unspecified,
                displayAppName = false,
                showFooter = true,
                // Blank → the About page auto-derives the repo from this app's namespace
                // (com.hereliesaz.sampleapp → github.com/hereliesaz/SampleApp). The Customization
                // screen can override it to demo the optional explicit URL.
                appRepositoryUrl = "",
                helpLineColors = emptyList(),
                vibrate = false,
            )
        )
    }

    // expandWhen demo: toggling this causes the Rail Host to auto-expand/collapse.
    val expandWhenDemoState = remember { mutableStateOf(false) }

    // onExpandedChange demo: tracks the current rail expansion state from outside the composable.
    var railIsExpanded by remember { mutableStateOf(false) }

    // Per-host onExpandedChange demo: tracks individual host expansion states.
    val hostExpandedStates = remember { mutableStateMapOf<String, Boolean>() }

    // The third highlight, driven entirely by the app: the "Armed" toggle below lights it.
    var armed by remember { mutableStateOf(false) }
    // Help system state — drives azAdvanced(helpEnabled, helpList) and azConfig(activeClassifiers).
    var helpSystem by remember { mutableStateOf(HelpSystemState(autoInjectHelpEnabled = false, activeClassifiers = emptySet(), dismissCount = 0)) }

    // Release-audit demo state: per-item loading + badge, and a rail-bound popup.
    var itemSyncing by remember { mutableStateOf(false) }
    var itemUnread by remember { mutableStateOf(2) }
    var toolsGrid by remember { mutableStateOf(false) }
    val alerts = com.hereliesaz.aznavrail.rememberAzPopupController()
    val demoScope = androidx.compose.runtime.rememberCoroutineScope()

    // FAB / overlay screen state.
    var fabState by remember {
        mutableStateOf(FabOverlayState(railDragEnabled = true, railLog = "(no drag yet)", overlayDragLog = "(no drag yet)", undockedCount = 0))
    }

    // Bottom-sheet demo: controller + live AzSheetConfig owned at host scope so the
    // DSL-registered azBottomSheet draws above the rail/menu and respects nav-bar insets.
    val sheetController = rememberAzSheetController(initial = AzSheetDetent.PEEK)
    var horizontalSwipeEnabled by remember { mutableStateOf(true) }
    var collapseOnBack by remember { mutableStateOf(true) }
    var handleVisible by remember { mutableStateOf(true) }
    var animateInTree by remember { mutableStateOf(true) }
    var sheetSwipeLog by remember { mutableStateOf("(no swipes yet)") }
    var sheetSwipeCount by remember { mutableStateOf(0) }

    // Bring the sheet up to PEEK whenever the user lands on the bottom-sheet screen so it's
    // immediately obvious. On other routes the user's last-set detent (often HIDDEN) is honoured.
    LaunchedEffect(currentDestination) {
        if (currentDestination == "bottom-sheet" && sheetController.detent == AzSheetDetent.HIDDEN) {
            sheetController.snapTo(AzSheetDetent.PEEK)
        }
    }

    // Hidden menu screen state.
    val relocOrder = remember { mutableStateListOf("reloc-1", "reloc-2", "reloc-nested-h", "reloc-nested-v") }
    var hiddenLastAction by remember { mutableStateOf("(none)") }
    var hiddenRelocateLog by remember { mutableStateOf("(no reorder yet)") }
    val hiddenInputs = remember { mutableStateMapOf("nickname" to "", "tag" to "foo") }

    val themeColor = MaterialTheme.colorScheme.primary

    // Status-driven guidance demo: a custom status backed by app state (see the `azStatus`/`azEdge`
    // declarations below and the activate buttons in TutorialDemoScreen).
    var guideTaskDone by remember { mutableStateOf(false) }
    // Worked example state: an arbitrary, MOVING on-screen highlight target (a draggable coach ball,
    // reported in window-space) plus the status it flips when dragged.
    var coachBallBounds by remember { mutableStateOf<Rect?>(null) }
    var coachBallDragged by remember { mutableStateOf(false) }
    // Host-driven guidance suppression (e.g. while a gesture is in progress).
    var suppressGuidance by remember { mutableStateOf(false) }

    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
        currentDestination = currentDestination,
        isLandscape = isLandscape,
        initiallyExpanded = false,
        onExpandedChange = { railIsExpanded = it },
    ) {
        azConfig(
            packButtons = packRailButtons,
            dockingSide = if (isDockingRight) AzDockingSide.RIGHT else AzDockingSide.LEFT,
            noMenu = noMenu,
            usePhysicalDocking = usePhysicalDocking,
            vibrate = customization.vibrate,
            displayAppName = customization.displayAppName,
            activeClassifiers = helpSystem.activeClassifiers,
            // The declarative half of the secondary highlight: any item tagged "armed" wears it
            // while the toggle below is on.
            secondaryClassifiers = if (armed) setOf("armed") else emptySet(),
            expandedWidth = customization.expandedWidth,
            collapsedWidth = customization.collapsedWidth,
            railItemWidth = customization.railItemWidth,
            showFooter = customization.showFooter,
            appRepositoryUrl = customization.appRepositoryUrl,
            // WP7 menu-drawer knobs.
            dimBehindMenu = customization.dimBehindMenu,
            menuItemAlignment = customization.menuItemAlignment,
            justifyMenuItems = customization.justifyMenuItems,
        )

        azTheme(
            defaultShape = customization.defaultShape,
            // The three highlights: where you are, what you are touching, and whatever the app
            // decides. Focus is left at the theme colour here by default; secondary is amber
            // because it reads as a condition rather than a place.
            activeColor = themeColor,
            focusColor = customization.focusColor,
            secondaryColor = customization.secondaryColor,
            headerIconShape = customization.headerIconShape,
            translucentBackground = customization.translucentBackground,
            helpLineColors = customization.helpLineColors,
            headerIconSize = customization.headerIconSize,
        )

        // In-app About reader (auto-generated from this repo's docs) + pinned "More" rail item that
        // opens the "More from Az" carousel.
        //
        // The rail ends with an About ("?") item on its own; this app declares its own instead
        // (below, after the last rail item) to show that the button is placeable and styleable
        // rather than fixed. `azAbout(aboutRailItem = false)` would drop it entirely.
        azAbout(moreRailItem = true)

        azAdvanced(
            isLoading = isLoading,
            enableRailDragging = fabState.railDragEnabled,
            helpEnabled = helpSystem.autoInjectHelpEnabled,
            onDismissHelp = {
                Log.d(TAG, "Help dismissed")
                helpSystem = helpSystem.copy(dismissCount = helpSystem.dismissCount + 1)
            },
            overlayService = SampleOverlayService::class.java,
            onRailDrag = { x, y ->
                fabState = fabState.copy(railLog = "rail dx=${"%.1f".format(x)} dy=${"%.1f".format(y)}")
            },
            onOverlayDrag = { x, y ->
                fabState = fabState.copy(overlayDragLog = "overlay dx=${"%.1f".format(x)} dy=${"%.1f".format(y)}")
            },
            onUndock = {
                fabState = fabState.copy(undockedCount = fabState.undockedCount + 1)
                Log.d(TAG, "Rail undocked")
            },
            helpList = mapOf(
                "showcase-home" to "Index of every demo screen.",
                "bottom-sheet" to "AzBottomSheet + AzBottomSheetInsetAware demo.",
                "tutorial" to "Status-driven guidance demo — activate goals, watch callouts route live.",
                "fab-overlay" to "FAB-mode drag callbacks + SampleOverlayService.",
                "customization" to "Live theme/config controls.",
                "help-system" to "Demonstrates this very overlay.",
                "forms" to "AzForm + AzTextBox showcase.",
                "hidden-menus" to "Reloc items with rich HiddenMenuScope.",
                "standalone-widgets" to "AzLoad, AzDivider, button/toggle/cycler/roller variants.",
                "legacy" to "The original demo playground.",
                "color-item" to "Demonstrates dynamic content with Color and Custom Text/Colors",
                "icon-item" to "Demonstrates dynamic content with Resource ID",
                "rail-cycler" to "Rail cycler with one disabled option (C).",
                "nested-1" to "Nested rail item — bounds reporting works inside popups.",
            ),
        )

        // ---------- Status-driven guidance demo (replaces the old scripted tutorial) ----------
        // A custom status node backed by app state, plus a hand-authored edge that carries the
        // instruction to reach it. Built-in `az.*` statuses (screen/host/rail) and their auto-edges
        // come for free, so the goals below only need a custom edge for the custom status.
        azStatus("guide_task_done") { guideTaskDone }
        azEdge(
            from = "az.screen.tutorial",
            to = "guide_task_done",
            text = "Press \"Mark task done\" on this screen",
            highlightItemId = "tutorial",
        )
        // Goals the demo activates. `guide_onboarding` self-arms when you land on the Tutorials screen
        // and routes to Bottom Sheets via the auto-generated "Tap Bottom Sheets" edge. The other two
        // are activated together to show simultaneous, independently-placed callouts.
        azGoal(id = "guide_onboarding", target = "az.screen.bottom-sheet", label = "See the Bottom Sheets demo", autoStartWhen = "az.screen.tutorial")
        azGoal(id = "guide_expand_host", target = "az.host.rail-host.expanded", label = "Expand the Rail Host")
        azGoal(id = "guide_custom_task", target = "guide_task_done", label = "Complete a custom task")

        // ---------- Worked example: arbitrary moving target + a mixed manual/reactive paged goal ----------
        // A host-registered target tracks the draggable "coach ball" the screen draws (window-space px),
        // so the spotlight follows it every frame. The single paged edge mixes two tap-advanced info
        // steps with one actionable step that auto-advances when the ball is dragged.
        azGuidanceTarget("coach.ball") {
            coachBallBounds?.let { b -> AzGuideShape.Circle(b.center.x, b.center.y, b.minDimension / 2f, padding = 8f) }
        }
        azStatus("coach_ball_dragged") { coachBallDragged }
        azEdge(
            from = "az.screen.tutorial",
            to = "coach_ball_dragged",
            title = "Meet the coach",
            steps = listOf(
                AzInstructionStep("This coach is status-driven. Tap to continue."),
                AzInstructionStep("It can point at moving on-screen things, not just rail items.", highlightTargetId = "coach.ball"),
                AzInstructionStep("Now drag the circle to finish.", highlightTargetId = "coach.ball", advanceWhen = "coach_ball_dragged"),
            ),
        )
        azGoal(id = "guide_coach", target = "coach_ball_dragged", label = "Meet the moving-target coach")
        // Host-driven suppression: while the toggle is on the overlay hides; when it flips off, guidance
        // re-shows after the default ~700 ms settle.
        azSuppressGuide { suppressGuidance }

        // ---------- Showcase navigation menu items ----------
        azMenuItem(id = "showcase-home", text = "Showcase Home", route = "showcase-home", screenTitle = "Showcase", info = "Index of every demo screen in this sample.", badge = "New!", persistentBadge = true)
        azMenuItem(id = "bottom-sheet", text = "Bottom Sheets", route = "bottom-sheet", screenTitle = "Bottom Sheets", info = "AzBottomSheet detents, drag, scrim, swipe.")
        azMenuItem(id = "tutorial", text = "Guidance", route = "tutorial", screenTitle = "Guidance", info = "Status-driven guidance — azStatus/azEdge/azGoal + live routing.", classifiers = setOf("advanced"))
        azMenuItem(id = "fab-overlay", text = "FAB / Overlay", route = "fab-overlay", screenTitle = "FAB & Overlay", info = "Rail drag callbacks + system overlay service.", classifiers = setOf("advanced", "danger"))
        azMenuItem(id = "customization", text = "Customization", route = "customization", screenTitle = "Theming", info = "Live theme/config controls.")
        azMenuItem(id = "help-system", text = "Help System", route = "help-system", screenTitle = "Help System", info = "screenTitle, info, classifiers, helpList.", classifiers = setOf("focus"))
        azMenuItem(id = "forms", text = "Forms", route = "forms", screenTitle = "Forms", info = "AzForm + AzTextBox parameter showcase.")
        azMenuItem(id = "hidden-menus", text = "Hidden Menus", route = "hidden-menus", screenTitle = "Hidden Menus", info = "Reloc items with HiddenMenuScope.")
        azMenuItem(id = "standalone-widgets", text = "Standalone Widgets", route = "standalone-widgets", screenTitle = "Standalone Widgets", info = "AzLoad / AzDivider / AzRoller / EqualWidthLayout / AutoSizeText.")
        azMenuItem(id = "legacy", text = "Legacy Demo", route = "legacy", screenTitle = "Rail Configuration Demo", info = "Original SampleApp playground.")

        azDivider()

        // ---------- Existing rail-config items (preserved) ----------
        azRailToggle(
            id = "pack-rail",
            isChecked = packRailButtons,
            toggleOnText = "Packed",
            toggleOffText = "Unpacked",
            info = "Toggle to pack items together or space them out.",
            onClick = { packRailButtons = !packRailButtons },
        )

        // A slider that unfolds in its own slot on the rail. Tap it, drag, tap the value to fold.
        azRailSlider(
            id = "volume",
            text = "Vol",
            value = volume,
            config = AzSliderConfig(size = AzSliderSize.SMALL),
            info = "azRailSlider — a continuous track that unfolds where the item stands.",
            valueFormatter = { "${(it * 100).toInt()}%" },
            onValueChange = { volume = it },
        )

        azRailSlider(
            id = "quality",
            text = "Qual",
            value = quality,
            config = AzSliderConfig(
                variant = AzSliderVariant.STEPPED,
                steps = 3,
                valueFrom = 0f,
                valueTo = 4f,
            ),
            info = "azRailSlider(variant = STEPPED) — stop indicators mark every landing point.",
            onValueChange = { quality = it },
        )

        azRailSlider(
            id = "trim",
            text = "Trim",
            value = trim,
            config = AzSliderConfig(
                variant = AzSliderVariant.CENTERED,
                valueFrom = -1f,
                valueTo = 1f,
            ),
            info = "azRailSlider(variant = CENTERED) — the track grows out of zero, so the sign reads without the number.",
            onValueChange = { trim = it },
        )

        azRailItem(
            id = "color-item",
            text = "Color",
            menuText = "Custom Menu Text",
            textColor = Color.White,
            fillColor = Color.Blue,
            content = Color.Red,
            info = "Demonstrates dynamic content with Color and Custom Text/Colors",
            onClick = { Log.d(TAG, "Color item clicked") },
        )

        azRailItem(
            id = "icon-item",
            text = "Icon",
            content = android.R.drawable.ic_menu_agenda,
            info = "Demonstrates dynamic content with Resource ID",
            badge = "5",
            onClick = { Log.d(TAG, "Icon item clicked") },
        )

        azRailItem(
            id = "vector-item",
            text = "Vector",
            content = Icons.Default.Delete,
            info = "Demonstrates dynamic content with a Compose ImageVector (fills + clips the shape)",
            onClick = { Log.d(TAG, "Vector item clicked") },
        )

        // ---------- AzButtonShape showcase: one rail item per shape value ----------
        azRailItem(
            id = "shape-circle",
            text = "Circle",
            shape = AzButtonShape.CIRCLE,
            info = "azRailItem(shape = AzButtonShape.CIRCLE)",
            onClick = { Log.d(TAG, "Circle shape clicked") },
        )
        azRailItem(
            id = "shape-square",
            text = "Square",
            shape = AzButtonShape.SQUARE,
            info = "azRailItem(shape = AzButtonShape.SQUARE)",
            onClick = { Log.d(TAG, "Square shape clicked") },
        )
        azRailItem(
            id = "shape-rectangle",
            text = "Rectangle",
            shape = AzButtonShape.RECTANGLE,
            info = "azRailItem(shape = AzButtonShape.RECTANGLE)",
            onClick = { Log.d(TAG, "Rectangle shape clicked") },
        )
        azRailItem(
            id = "none-shape",
            text = "No Shape",
            shape = AzButtonShape.NONE,
            info = "azRailItem(shape = AzButtonShape.NONE) — borderless on the RECTANGLE footprint",
            onClick = { Log.d(TAG, "No Shape item clicked") },
        )
        azRailItem(
            id = "none-square-shape",
            text = "None\nSquare",
            shape = AzButtonShape.NONE_SQUARE,
            info = "azRailItem(shape = AzButtonShape.NONE_SQUARE) — borderless, square footprint",
            onClick = { Log.d(TAG, "None-square shape clicked") },
        )
        azRailItem(
            id = "none-circle-shape",
            text = "None\nCircle",
            shape = AzButtonShape.NONE_CIRCLE,
            info = "azRailItem(shape = AzButtonShape.NONE_CIRCLE) — borderless, circle footprint",
            onClick = { Log.d(TAG, "None-circle shape clicked") },
        )

        azRailItem(
            id = "profile",
            text = "Profile",
            disabled = true,
            route = "profile",
            info = "User profile settings (Disabled)",
        )

        azDivider()

        azRailToggle(
            id = "online",
            isChecked = isOnline,
            toggleOnText = "Online",
            toggleOffText = "Offline",
            onClick = { isOnline = !isOnline },
        )

        azMenuToggle(
            id = "dark-mode",
            isChecked = isDarkMode,
            toggleOnText = "Dark Mode",
            toggleOffText = "Light Mode",
            onClick = { isDarkMode = !isDarkMode },
        )

        azMenuToggle(
            id = "docking-side",
            isChecked = isDockingRight,
            toggleOnText = "Dock: Right",
            toggleOffText = "Dock: Left",
            onClick = { isDockingRight = !isDockingRight },
        )

        azMenuToggle(
            id = "no-menu",
            isChecked = noMenu,
            toggleOnText = "No Menu: On",
            toggleOffText = "No Menu: Off",
            onClick = { noMenu = !noMenu },
        )

        azMenuToggle(
            id = "physical-docking",
            isChecked = usePhysicalDocking,
            toggleOnText = "Physical Dock: On",
            toggleOffText = "Physical Dock: Off",
            onClick = { usePhysicalDocking = !usePhysicalDocking },
        )

        azHelpRailItem(id = "toggle-help", text = "Help")

        azDivider()

        azRailCycler(
            id = "rail-cycler",
            options = railCycleOptions,
            selectedOption = railSelectedOption,
            disabledOptions = listOf("C"),
            onClick = {
                val nextIndex = (railCycleOptions.indexOf(railSelectedOption) + 1) % railCycleOptions.size
                railSelectedOption = railCycleOptions[nextIndex]
            },
        )

        azMenuCycler(
            id = "menu-cycler",
            options = menuCycleOptions,
            selectedOption = menuSelectedOption,
            onClick = {
                val nextIndex = (menuCycleOptions.indexOf(menuSelectedOption) + 1) % menuCycleOptions.size
                menuSelectedOption = menuCycleOptions[nextIndex]
            },
        )

        azRailItem(id = "loading", text = "Load", onClick = { isLoading = !isLoading })

        // --- Per-item loading + badge (azItemState) -------------------------------------------
        // Only this one button spins; the global `isLoading` above dims the whole screen instead.
        azRailItem(
            id = "item-sync",
            text = "Sync",
            info = "azItemState(isLoading = …) — this item spins its own AzLoad while the rest of the rail stays live.",
            onClick = {
                if (!itemSyncing) {
                    itemSyncing = true
                    demoScope.launch {
                        kotlinx.coroutines.delay(2500)
                        itemSyncing = false
                        itemUnread += 1
                    }
                }
            },
        )
        azItemState(
            id = "item-sync",
            isLoading = itemSyncing,
            badge = itemUnread.takeIf { it > 0 }?.toString(),
        )

        // --- The third highlight (secondary) ---------------------------------------------------
        // "Armed" is a condition, not a destination: the item is not where the user is and it is not
        // what they are touching, so neither the active nor the focus highlight can say it. That is
        // what the secondary highlight is for, and the app is the only thing that can light it.
        azRailToggle(
            id = "item-armed",
            isChecked = armed,
            toggleOnText = "Armed",
            toggleOffText = "Safe",
            info = "azItemState(secondary = …) + azHighlight — the third highlight, driven by the app.",
            classifiers = setOf("armed"),
            onClick = { armed = !armed },
        )
        azItemState(id = "item-armed", secondary = armed)
        // …and this one item disagrees with the rail about what colour "armed" is.
        azHighlight(id = "item-armed", secondary = customization.secondaryColor)

        // --- Popup bound to a rail item -------------------------------------------------------
        // No itemId, so it binds to whatever the user touched last and turns that item into the
        // yellow rounded-corner warning triangle until the popup is dismissed.
        azRailItem(
            id = "item-warn",
            text = "Warn",
            info = "AzPopupKind.WARNING — the last touched rail item becomes a yellow triangle while the popup is up.",
            onClick = {
                alerts.show(
                    kind = com.hereliesaz.aznavrail.AzPopupKind.WARNING,
                    title = "Offline",
                    message = "Changes are queued and will sync when the connection returns.",
                )
            },
        )
        azPopup(alerts)

        // --- Unattached hosts ------------------------------------------------------------------
        // Rail hosts drawn outside the rail strip. FLOATING is draggable and remembers where it was
        // dropped; BOTTOM parks at the bottom of the side opposite the rail.
        azUnattachedHostItem(
            id = "unattached-tools",
            text = "Tools",
            anchor = com.hereliesaz.aznavrail.model.AzUnattachedAnchor.FLOATING,
            info = "azUnattachedHostItem(anchor = FLOATING) — drag it anywhere; its position persists.",
        )
        azRailSubItem(id = "unattached-measure", hostId = "unattached-tools", text = "Measure") {
            Log.d(TAG, "Unattached: Measure clicked")
        }
        azRailSubToggle(
            id = "unattached-grid",
            hostId = "unattached-tools",
            isChecked = toolsGrid,
            toggleOnText = "Grid On",
            toggleOffText = "Grid Off",
        ) { toolsGrid = !toolsGrid }

        azUnattachedHostItem(
            id = "unattached-layers",
            text = "Layers",
            anchor = com.hereliesaz.aznavrail.model.AzUnattachedAnchor.BOTTOM,
            info = "azUnattachedHostItem(anchor = BOTTOM) — bottom of the side opposite the rail.",
        )
        azRailSubItem(id = "unattached-layer-base", hostId = "unattached-layers", text = "Base") {
            Log.d(TAG, "Unattached: Base layer clicked")
        }
        azRailSubItem(id = "unattached-layer-over", hostId = "unattached-layers", text = "Over") {
            Log.d(TAG, "Unattached: Over layer clicked")
        }
        // A relocatable layer row under an unattached host — the exact shape of a Procreate-style
        // layers panel. Tap-to-select and long-press-to-open-hidden-menu both work here; drag-to-
        // reorder does not (see the KDoc on `azRailRelocItem`'s `onRelocate` parameter).
        azRailRelocItem(
            id = "unattached-layer-detail",
            hostId = "unattached-layers",
            text = "Detail",
            info = "azRailRelocItem under an unattached host — tap selects it, long-press opens its hidden menu.",
            onClick = { hiddenLastAction = "unattached-layer-detail → selected" },
        ) {
            listItem(text = "Duplicate") { hiddenLastAction = "unattached-layer-detail → Duplicate" }
            listItem(text = "Delete") { hiddenLastAction = "unattached-layer-detail → Delete" }
        }

        // expandWhen demo toggle — lives in the menu so it doesn't clutter the rail.
        // Toggling On triggers a false→true edge on azRailHostItem("rail-host"), causing it
        // to auto-expand. Toggling Off causes a true→false edge and auto-collapses it.
        // If the user manually collapses while the toggle is On, that collapse is respected
        // (user-wins); the next Off→On cycle will re-expand.
        azMenuToggle(
            id = "expand-when-demo",
            isChecked = expandWhenDemoState.value,
            toggleOnText = "Auto-Expand: On",
            toggleOffText = "Auto-Expand: Off",
            info = "expandWhen demo — when On, Rail Host auto-expands; toggling Off auto-collapses it. Manual collapse while On is respected (user-wins rule).",
            onClick = { expandWhenDemoState.value = !expandWhenDemoState.value },
        )

        azDivider()

        // Host + sub items
        azMenuHostItem(id = "menu-host", text = "Menu Host", route = "menu-host",
            onExpandedChange = { hostExpandedStates["menu-host"] = it })
        azMenuSubItem(id = "menu-sub-1", hostId = "menu-host", text = "Menu Sub 1", route = "menu-sub-1")
        azMenuSubItem(id = "menu-sub-2", hostId = "menu-host", text = "Menu Sub 2", route = "menu-sub-2")
        azHelpSubItem(id = "menu-host-help", hostId = "menu-host", text = "Help")

        azRailHostItem(
            id = "rail-host",
            text = "Rail Host",
            route = "rail-host",
            expandWhen = { expandWhenDemoState.value },
            onExpandedChange = { hostExpandedStates["rail-host"] = it },
        )
        azRailSubItem(id = "rail-sub-1", hostId = "rail-host", text = "Rail Sub 1", route = "rail-sub-1")
        azMenuSubItem(id = "rail-sub-2", hostId = "rail-host", text = "Menu Sub 2", route = "rail-sub-2")

        // Nested host: a sub-item that is itself a host with its own sub-items. Sub-hosts can
        // nest to any depth; children attach by `hostId`, so "rail-subhost"'s children are
        // distinct from its sibling sub-items under "rail-host".
        azRailSubHostItem(id = "rail-subhost", hostId = "rail-host", text = "Rail Sub Host", route = "rail-subhost")
        azRailSubItem(id = "rail-subhost-1", hostId = "rail-subhost", text = "Nested A", route = "rail-subhost-1")
        azRailSubItem(id = "rail-subhost-2", hostId = "rail-subhost", text = "Nested B", route = "rail-subhost-2")

        azMenuSubHostItem(id = "menu-subhost", hostId = "menu-host", text = "Menu Sub Host", route = "menu-subhost")
        azMenuSubItem(id = "menu-subhost-1", hostId = "menu-subhost", text = "Nested 1", route = "menu-subhost-1")
        azMenuSubItem(id = "menu-subhost-2", hostId = "menu-subhost", text = "Nested 2", route = "menu-subhost-2")

        azMenuSubToggle(
            id = "sub-toggle",
            hostId = "menu-host",
            isChecked = isDarkMode,
            toggleOnText = "Sub Toggle On",
            toggleOffText = "Sub Toggle Off",
            onClick = { isDarkMode = !isDarkMode },
        )

        azRailSubCycler(
            id = "sub-cycler",
            hostId = "rail-host",
            options = menuCycleOptions,
            selectedOption = menuSelectedOption,
            onClick = {
                val nextIndex = (menuCycleOptions.indexOf(menuSelectedOption) + 1) % menuCycleOptions.size
                menuSelectedOption = menuCycleOptions[nextIndex]
            },
        )

        // ---------- Hidden menu demo cluster ----------
        azRailRelocItem(
            id = "reloc-1",
            hostId = "rail-host",
            text = "Reloc 1",
            info = "Hidden menu with three plain listItem callbacks.",
            onRelocate = { from, to, newOrder ->
                hiddenRelocateLog = "$from → $to → $newOrder"
                relocOrder.clear(); relocOrder.addAll(newOrder)
            },
        ) {
            listItem(text = "Rename") { hiddenLastAction = "reloc-1 → Rename" }
            listItem(text = "Pin") { hiddenLastAction = "reloc-1 → Pin" }
            listItem(text = "Open standalone widgets", route = "standalone-widgets")
        }

        azRailRelocItem(
            id = "reloc-2",
            hostId = "rail-host",
            text = "Reloc 2",
            info = "Hidden menu mixes listItem + inputItem fields.",
            onRelocate = { from, to, newOrder ->
                hiddenRelocateLog = "$from → $to → $newOrder"
                relocOrder.clear(); relocOrder.addAll(newOrder)
            },
        ) {
            inputItem(hint = "Nickname") { hiddenInputs["nickname"] = it }
            inputItem(hint = "Tag", initialValue = hiddenInputs["tag"] ?: "foo") { hiddenInputs["tag"] = it }
            listItem(text = "Reset") { hiddenLastAction = "reloc-2 → Reset" }
        }

        azRailRelocItem(
            id = "reloc-nested-h",
            hostId = "rail-host",
            text = "Reloc + Horizontal Nested",
            info = "Reloc item with a HORIZONTAL nested popup.",
            onRelocate = { from, to, newOrder ->
                hiddenRelocateLog = "$from → $to → $newOrder"
                relocOrder.clear(); relocOrder.addAll(newOrder)
            },
            nestedRailAlignment = AzNestedRailAlignment.HORIZONTAL,
            nestedContent = {
                azRailItem("nested-tool-h-1", "Tool 1", onClick = { Log.d(TAG, "H Tool 1 clicked") })
                azRailItem("nested-tool-h-2", "Tool 2", onClick = { Log.d(TAG, "H Tool 2 clicked") })
                azRailItem("nested-tool-h-3", "Tool 3", onClick = { Log.d(TAG, "H Tool 3 clicked") })
            },
        ) {
            listItem(text = "Remove") { hiddenLastAction = "reloc-nested-h → Remove" }
        }

        azRailRelocItem(
            id = "reloc-nested-v",
            hostId = "rail-host",
            text = "Reloc + Vertical Nested",
            info = "Reloc item with a VERTICAL nested popup (keepNestedRailOpen = true so it stays open).",
            keepNestedRailOpen = true,
            onRelocate = { from, to, newOrder ->
                hiddenRelocateLog = "$from → $to → $newOrder"
                relocOrder.clear(); relocOrder.addAll(newOrder)
            },
            nestedRailAlignment = AzNestedRailAlignment.VERTICAL,
            nestedContent = {
                azRailItem("nested-tool-v-1", "Tool A", onClick = { Log.d(TAG, "V Tool A clicked") })
                azRailItem("nested-tool-v-2", "Tool B", onClick = { Log.d(TAG, "V Tool B clicked") })
                azRailItem("nested-tool-v-3", "Tool C", onClick = { Log.d(TAG, "V Tool C clicked") })
            },
        ) {
            listItem(text = "Remove") { hiddenLastAction = "reloc-nested-v → Remove" }
        }

        // Nested rails (without routes to avoid nav crashes on tap)
        azNestedRail(
            id = "nested-rail",
            text = "Vertical Nested",
            alignment = AzNestedRailAlignment.VERTICAL,
        ) {
            azRailItem(id = "nested-1", text = "Nested Item 1", route = "nested-1", info = "Nested item bounds report correctly for help — and tapping the nested Help item scopes the overlay to just these items.")
            azRailItem(id = "nested-2", text = "Nested Item 2", route = "nested-2", info = "A second nested item with its own help text.")
            // Help item INSIDE the nested rail — when tapped, the overlay should show only the
            // nested rail's items (Nested Item 1, Nested Item 2, Size Slider), not the main rail.
            azHelpRailItem(id = "nested-help", text = "Help")
            azRailItem(
                id = "nested-custom",
                text = "Size Slider",
                content = AzComposableContent { isEnabled ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isEnabled) {
                                if (isEnabled) {
                                    detectVerticalDragGestures { change, _ -> change.consume() }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(50.dp)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Wide Content (Should Clip)",
                                color = if (isEnabled) Color.White else Color.Gray,
                            )
                        }
                    }
                },
            )
        }

        azNestedRail(
            id = "nested-horizontal",
            text = "Horizontal Nested",
            alignment = AzNestedRailAlignment.HORIZONTAL,
        ) {
            azRailItem(id = "nested-h-1", text = "H-Item 1", route = "nested-h-1", info = "First horizontal nested item.")
            azRailItem(id = "nested-h-2", text = "H-Item 2", route = "nested-h-2", info = "Second horizontal nested item.")
            azRailItem(id = "nested-h-3", text = "H-Item 3", route = "nested-h-3", info = "Third horizontal nested item.")
            azHelpRailItem(id = "nested-h-help", text = "?")
        }

        // ---------- About ("?") ----------
        // Declared explicitly rather than letting the rail append its own, to show that the button
        // is placeable and styleable: this one carries the theme accent and sits after the nested
        // rails. Tapping it opens the About reader; tapping it again — or any other rail or menu
        // item, or the app icon — closes it.
        azAboutRailItem(
            id = "about",
            text = "?",
            color = themeColor,
            info = "azAboutRailItem(...) — the About reader. Persistent, but yours to place.",
        )

        // ---------- Host-registered bottom sheet (azBottomSheet DSL) ----------
        // Registered unconditionally so the HIDDEN strip is always present at the bottom of the
        // screen — that's the affordance for revealing the sheet. The default initial detent is
        // PEEK so first launch makes the sheet obviously visible. A LaunchedEffect below
        // re-snaps to PEEK whenever the user navigates to the bottom-sheet screen.
        azBottomSheet(
            controller = sheetController,
            config = AzSheetConfig(
                horizontalSwipeEnabled = horizontalSwipeEnabled,
                collapseOnBack = collapseOnBack,
                handleVisible = handleVisible,
                animateInTree = animateInTree,
            ),
            onSwipeLeft = {
                sheetSwipeCount++
                sheetSwipeLog = "left @ ${System.currentTimeMillis() % 100000}"
            },
            onSwipeRight = {
                sheetSwipeCount++
                sheetSwipeLog = "right @ ${System.currentTimeMillis() % 100000}"
            },
        ) {
            BottomSheetBody(sheetController.detent)
        }

        // ---------- Backgrounds ----------
        background(weight = 0) {
            Box(Modifier.fillMaxSize().background(Color(0xFFEEEEEE)))
        }
        background(weight = 10) {
            Box(Modifier.fillMaxSize().padding(50.dp).background(Color.Blue.copy(alpha = 0.1f))) {
                Text("Background Layer (Weight 10)", color = Color.Blue)
            }
        }

        // ---------- Onscreen + NavHost ----------
        onscreen(alignment = Alignment.TopStart) {
            Text("Aligned TopStart (Flips)", modifier = Modifier.padding(16.dp))

            // Declared down here, drawn up beside the big screen title — and two of them line up
            // next to each other in declaration order.
            com.hereliesaz.aznavrail.AzDropdownMenu(navController = navController) {
                azConfig(showFooter = false)
                azItem("Clear badge") { itemUnread = 0 }
                azItem("Add badge") { itemUnread += 1 }
            }
            com.hereliesaz.aznavrail.AzDropdownMenu(navController = navController) {
                azConfig(
                    showFooter = false,
                    trigger = com.hereliesaz.aznavrail.model.AzDropdownTrigger.Text("View"),
                )
                azToggle(
                    isChecked = toolsGrid,
                    toggleOnText = "Grid On",
                    toggleOffText = "Grid Off",
                ) { toolsGrid = it }
                azDivider()
                azItem("Notice on Home") {
                    alerts.show(
                        itemId = "home",
                        kind = com.hereliesaz.aznavrail.AzPopupKind.NOTICE,
                        title = "Heads up",
                        message = "Home stays flagged while this notice is open.",
                    )
                }
            }
        }
        onscreen(alignment = Alignment.TopEnd) {
            Text("Aligned TopEnd (Flips)", modifier = Modifier.padding(16.dp))
        }
        onscreen(alignment = Alignment.Center) {
            AzNavHost(startDestination = "showcase-home", navController = navController) {
                composable("showcase-home") {
                    ShowcaseHomeScreen(
                        onNavigate = { route -> navController.navigate(route) },
                        railIsExpanded = railIsExpanded,
                        hostExpandedStates = hostExpandedStates,
                    )
                }
                composable("bottom-sheet") {
                    BottomSheetDemoScreen(
                        controller = sheetController,
                        horizontalSwipeEnabled = horizontalSwipeEnabled,
                        onHorizontalSwipeChange = { horizontalSwipeEnabled = it },
                        collapseOnBack = collapseOnBack,
                        onCollapseOnBackChange = { collapseOnBack = it },
                        handleVisible = handleVisible,
                        onHandleVisibleChange = { handleVisible = it },
                        animateInTree = animateInTree,
                        onAnimateInTreeChange = { animateInTree = it },
                        swipeCount = sheetSwipeCount,
                        swipeLog = sheetSwipeLog,
                    )
                }
                composable("tutorial") {
                    TutorialDemoScreen(
                        taskDone = guideTaskDone,
                        onMarkTaskDone = { guideTaskDone = true },
                        onResetTask = { guideTaskDone = false },
                        coachBallDragged = coachBallDragged,
                        onCoachBallBounds = { coachBallBounds = it },
                        onCoachBallDrag = { coachBallDragged = true },
                        onResetCoach = { coachBallDragged = false },
                        suppressed = suppressGuidance,
                        onToggleSuppress = { suppressGuidance = !suppressGuidance },
                    )
                }
                composable("fab-overlay") {
                    FabOverlayDemoScreen(
                        state = fabState,
                        onToggleRailDrag = { fabState = fabState.copy(railDragEnabled = it) },
                    )
                }
                composable("customization") {
                    CustomizationDemoScreen(state = customization, onChange = { customization = it })
                }
                composable("help-system") {
                    HelpSystemDemoScreen(state = helpSystem, onChange = { helpSystem = it })
                }
                // Route strings for these three come from the generated Destination objects
                // (the compose-destinations pilot -- see FormShowcaseScreen's KDoc) rather than
                // being duplicated as literals here, so an annotation's `route` staying in sync
                // with the rail/menu route it must match is checked at compile time.
                composable(FormsDestination.route) { FormShowcaseScreen() }
                composable("hidden-menus") {
                    HiddenMenuDemoScreen(
                        state = HiddenMenuDemoState(
                            relocOrder = relocOrder.toList(),
                            nicknameValue = hiddenInputs["nickname"].orEmpty(),
                            tagValue = hiddenInputs["tag"].orEmpty(),
                            lastAction = hiddenLastAction,
                            relocateLog = hiddenRelocateLog,
                        ),
                    )
                }
                composable(StandaloneWidgetsDestination.route) { StandaloneWidgetsScreen() }
                composable(LegacyDestination.route) { LegacyRailDemoScreen() }

                // Preserved legacy routes so old menu/rail items still navigate.
                composable("menu-host") { ScreenContent("Menu Host Screen") }
                composable("menu-sub-1") { ScreenContent("Menu Sub 1 Screen") }
                composable("menu-sub-2") { ScreenContent("Menu Sub 2 Screen") }
                composable("rail-host") { ScreenContent("Rail Host Screen") }
                composable("rail-sub-1") { ScreenContent("Rail Sub 1 Screen") }
                composable("rail-sub-2") { ScreenContent("Rail Sub 2 Screen") }
                composable("rail-subhost") { ScreenContent("Rail Sub Host Screen") }
                composable("rail-subhost-1") { ScreenContent("Nested A Screen") }
                composable("rail-subhost-2") { ScreenContent("Nested B Screen") }
                composable("menu-subhost") { ScreenContent("Menu Sub Host Screen") }
                composable("menu-subhost-1") { ScreenContent("Nested 1 Screen") }
                composable("menu-subhost-2") { ScreenContent("Nested 2 Screen") }
                composable("profile") { ScreenContent("Profile Screen") }
                composable("nested-1") { ScreenContent("Nested Item 1 Screen") }
                composable("nested-2") { ScreenContent("Nested Item 2 Screen") }
                composable("nested-h-1") { ScreenContent("H-Item 1 Screen") }
                composable("nested-h-2") { ScreenContent("H-Item 2 Screen") }
                composable("nested-h-3") { ScreenContent("H-Item 3 Screen") }
            }
        }
    }
}

@Composable
fun ScreenContent(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}

@Composable
private fun BottomSheetBody(detent: AzSheetDetent) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Sheet contents", style = MaterialTheme.typography.titleMedium)
        Text("Current detent: $detent", style = MaterialTheme.typography.bodyMedium)
        Text("Drag the handle up/down to step through detents. Each gesture advances exactly one step.")
        Text("Toggle horizontal swipe in the panel to fire onSwipeLeft / onSwipeRight on header drag.")
        repeat(30) { i ->
            Text("Line ${i + 1} — body scrolls independently when sheet is at HALF or FULL.")
        }
    }
}
