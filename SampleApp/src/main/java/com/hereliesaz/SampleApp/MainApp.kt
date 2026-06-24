package com.hereliesaz.SampleApp

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.SampleApp.screens.BottomSheetDemoScreen
import com.hereliesaz.SampleApp.screens.CustomizationDemoScreen
import com.hereliesaz.SampleApp.screens.CustomizationState
import com.hereliesaz.SampleApp.screens.FabOverlayDemoScreen
import com.hereliesaz.SampleApp.screens.FabOverlayState
import com.hereliesaz.SampleApp.screens.FormShowcaseScreen
import com.hereliesaz.SampleApp.screens.HelpSystemDemoScreen
import com.hereliesaz.SampleApp.screens.HelpSystemState
import com.hereliesaz.SampleApp.screens.HiddenMenuDemoScreen
import com.hereliesaz.SampleApp.screens.HiddenMenuDemoState
import com.hereliesaz.SampleApp.screens.LegacyRailDemoScreen
import com.hereliesaz.SampleApp.screens.SampleTutorials
import com.hereliesaz.SampleApp.screens.ShowcaseHomeScreen
import com.hereliesaz.SampleApp.screens.StandaloneWidgetsScreen
import com.hereliesaz.SampleApp.screens.TutorialDemoScreen
import com.hereliesaz.aznavrail.AzHostActivityLayout
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
                displayAppName = false,
                showFooter = true,
                // Blank → the About page auto-derives the repo from this app's namespace
                // (com.hereliesaz.SampleApp → github.com/hereliesaz/SampleApp). The Customization
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

    // Help system state — drives azAdvanced(helpEnabled, helpList) and azConfig(activeClassifiers).
    var helpSystem by remember { mutableStateOf(HelpSystemState(autoInjectHelpEnabled = false, activeClassifiers = emptySet(), dismissCount = 0)) }

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
            expandedWidth = customization.expandedWidth,
            collapsedWidth = customization.collapsedWidth,
            showFooter = customization.showFooter,
            appRepositoryUrl = customization.appRepositoryUrl,
        )

        azTheme(
            defaultShape = customization.defaultShape,
            activeColor = themeColor,
            headerIconShape = customization.headerIconShape,
            translucentBackground = customization.translucentBackground,
            helpLineColors = customization.helpLineColors,
            headerIconSize = customization.headerIconSize,
        )

        // In-app About reader (auto-generated from this repo's docs) + pinned "More" rail item that
        // opens the "More from Az" carousel.
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
            tutorials = SampleTutorials,
            helpList = mapOf(
                "showcase-home" to "Index of every demo screen.",
                "bottom-sheet" to "AzBottomSheet + AzBottomSheetInsetAware demo.",
                "tutorial" to "Interactive tutorial DSL demo — covers every AzAdvanceCondition.",
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

        // ---------- Showcase navigation menu items ----------
        azMenuItem(id = "showcase-home", text = "Showcase Home", route = "showcase-home", screenTitle = "Showcase", info = "Index of every demo screen in this sample.")
        azMenuItem(id = "bottom-sheet", text = "Bottom Sheets", route = "bottom-sheet", screenTitle = "Bottom Sheets", info = "AzBottomSheet detents, drag, scrim, swipe.")
        azMenuItem(id = "tutorial", text = "Tutorials", route = "tutorial", screenTitle = "Tutorials", info = "AzTutorial DSL — every advance condition + highlight.", classifiers = setOf("advanced"))
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
            info = "azRailItem(shape = AzButtonShape.NONE) — text only, no border or fill",
            onClick = { Log.d(TAG, "No Shape item clicked") },
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
                composable("tutorial") { TutorialDemoScreen() }
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
                composable("forms") { FormShowcaseScreen() }
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
                composable("standalone-widgets") { StandaloneWidgetsScreen() }
                composable("legacy") { LegacyRailDemoScreen() }

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
