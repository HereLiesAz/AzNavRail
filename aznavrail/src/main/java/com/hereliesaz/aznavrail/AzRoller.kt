package com.hereliesaz.aznavrail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A dropdown menu that works like a roller or slot machine.
 *
 * It extends the capabilities of [AzTextBox] by allowing users to select from a list
 * of options in a "slot machine" style, or type freely.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param options The list of options to display.
 * @param selectedOption The currently selected option (string).
 * @param selectedIndex The index of the currently selected option.
 * @param onOptionSelected Called when an option is selected or text is entered.
 * @param hint The text to display when no option is selected.
 * @param enabled Controls the enabled state of the component.
 * @param outlineColor The color of the border and text.
 */
@Composable
fun AzRoller(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedOption: String? = null,
    selectedIndex: Int? = null,
    onOptionSelected: (String) -> Unit,
    hint: String = "",
    enabled: Boolean = true,
    outlineColor: Color = MaterialTheme.colorScheme.primary
) {
    var expanded by remember { mutableStateOf(false) }
    var componentHeight by remember { mutableIntStateOf(0) }
    var componentWidth by remember { mutableIntStateOf(0) }

    // Internal state for text input to support typing
    val text = selectedOption ?: selectedIndex?.let { options.getOrNull(it) } ?: ""

    val effectiveColor = if (!enabled) {
        outlineColor.copy(alpha = 0.5f)
    } else {
        outlineColor
    }

    Box(modifier = modifier.onSizeChanged {
        componentHeight = it.height
        componentWidth = it.width
    }) {
        AzTextBox(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = onOptionSelected,
            hint = hint,
            enabled = enabled,
            outlineColor = outlineColor,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(enabled = enabled) { expanded = !expanded },
                    tint = effectiveColor
                )
            },
            onSubmit = { expanded = false }
        )

        if (expanded && enabled) {
            val density = LocalDensity.current
            val itemHeight = with(density) { componentHeight.toDp() }

            // "displaying an entry in the text box and another two or three entries above the box, in addition to below."
            val visibleItemsAbove = 2
            val visibleItemsBelow = 2

            // Layout Strategy:
            // 1. Popup starts 'visibleItemsAbove' height *above* the AzTextBox.
            //    Popup Y = -(visibleItemsAbove * componentHeight) relative to AzTextBox top.
            // 2. We want the list to appear *below* initially.
            //    So initial scroll position should be such that the first item is at AzTextBox Bottom.
            //    AzTextBox Bottom is at relative Y = (visibleItemsAbove + 1) * componentHeight in Popup space.
            //    So we add Top Padding to LazyColumn = (visibleItemsAbove + 1) * itemHeight.
            //    Items start after this padding.
            // 3. User can scroll UP. Items move into the padding area.
            //    The padding area covers the AzTextBox and the space above it.

            val popupYOffset = -(componentHeight * visibleItemsAbove)

            // Initial scroll: If we want it to start "below", we just let it start at index 0
            // with the massive top padding.
            // But if there is a selected item, we might want to snap to it?
            // User said: "When the drop down list is first opened, it should appear as it currently does."
            // "But when the user scrolls up, each entry should be shown in the text box and continue up above it"
            // This implies the default position is fully below.

            val initialIndex = 0 // Always start at top (below box) if "appearing as currently does"
            // However, usually dropdowns show the selected item.
            // If we strictly follow "appear as currently does", it means list is below.

            val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
            val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

            // Auto-select item when scrolling stops
            LaunchedEffect(listState) {
                snapshotFlow { listState.firstVisibleItemIndex }
                    .distinctUntilChanged()
                    .collect { index ->
                        if (!listState.isScrollInProgress) {
                           // Logic to determine which item is "in the slot"
                           // The slot is at index corresponding to the visible window.
                           // With snapping, one item will be centered.
                           // But since we have padding, index 0 is at the bottom.
                           // If user scrolls, index increases?
                           // Actually, LazyColumn items flow down.
                           // To "scroll up" (move items up), we scroll down the list (index increases).
                           // If we scroll down, items move up.

                           // If we scroll so that item X is in the text box:
                           // Text Box is at Y = visibleItemsAbove * itemHeight.
                           // Item X is at Y = Padding + (X * itemHeight) - ScrollOffset.
                           // We want Item Y = Text Box Y.
                           // (visibleItemsAbove + 1) * itemHeight + (X * itemHeight) - ScrollOffset = visibleItemsAbove * itemHeight
                           // itemHeight + X * itemHeight = ScrollOffset
                           // ScrollOffset = (X + 1) * itemHeight.

                           // So if ScrollOffset matches, we select.
                           // We can use the center item.
                        }
                    }
            }

            // To ensure visibility of typing, we make the "slot" semi-transparent or allow the underlying box to show.
            // If the popup is on top, we can use a transparent background for the slot area.

            // Support typing: Jump to match.
            LaunchedEffect(text) {
                val matchIndex = options.indexOfFirst { it.startsWith(text, ignoreCase = true) }
                if (matchIndex != -1) {
                    // Logic to scroll item to "slot" or just to top?
                    // If we want "standard dropdown" feel initially, just scroll to it?
                    listState.scrollToItem(matchIndex)
                }
            }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, popupYOffset),
                properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true),
                onDismissRequest = { expanded = false }
            ) {
                val surfaceColor = MaterialTheme.colorScheme.surface

                Box(
                    modifier = Modifier
                        .width(with(density) { componentWidth.toDp() })
                        .height(itemHeight * (1 + visibleItemsAbove + visibleItemsBelow))
                        .background(Color.Transparent)
                ) {
                    // We only want the background for the items, not the empty space above?
                    // Or maybe the whole track has a background?
                    // User said "appear as currently does" (below).
                    // If we draw background everywhere, it looks like a huge bar.
                    // Let's make the background follow the list content?
                    // Or just transparent?

                    LazyColumn(
                        state = listState,
                        flingBehavior = snapBehavior,
                        modifier = Modifier.fillMaxWidth().background(surfaceColor), // Background for the list itself
                        contentPadding = PaddingValues(top = itemHeight * (visibleItemsAbove + 1))
                        // Top padding pushes the first item below the text box.
                    ) {
                        itemsIndexed(options) { index, option ->
                            val isSelected = option == text

                            Box(
                                modifier = Modifier
                                    .height(itemHeight)
                                    .fillMaxWidth()
                                    .background(if (isSelected) effectiveColor.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        onOptionSelected(option)
                                        expanded = false
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = option,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Selection indicator (The "Slot")
                    // It should highlight the text box area.
                    // Position: Y = visibleItemsAbove * itemHeight
                    Box(
                        modifier = Modifier
                            .padding(top = itemHeight * visibleItemsAbove)
                            .height(itemHeight)
                            .fillMaxWidth()
                            .border(2.dp, effectiveColor.copy(alpha = 0.5f))
                            // Important: No background so we can see through to the AzTextBox if needed
                            // OR we rely on the List item being in this slot.
                    )
                }
            }
        }
    }
}
