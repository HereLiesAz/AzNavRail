package com.hereliesaz.aznavrail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
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
 * @param isError Whether the input is in an error state.
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
    isError: Boolean = false,
    outlineColor: Color = MaterialTheme.colorScheme.primary
) {
    var expanded by remember { mutableStateOf(false) }
    var componentHeight by remember { mutableIntStateOf(0) }
    var componentWidth by remember { mutableIntStateOf(0) }
    var isTyping by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

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
        // Core TextBox
        AzTextBox(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = {
                if (isTyping) {
                   onOptionSelected(it)
                }
            },
            hint = hint,
            enabled = enabled,
            isError = isError,
            outlineColor = outlineColor,
            showClearButton = false, // Controlled by Split Click logic
            focusRequester = focusRequester,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.size(24.dp), // Click handled by Overlay
                    tint = effectiveColor
                )
            },
            onSubmit = { expanded = false }
        )

        // Split Click Overlay
        if (enabled && !isTyping) {
            Row(
                modifier = Modifier
                    .matchParentSize()
            ) {
                // Left Half: Text Edit Mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("AzRollerLeft")
                        .clickable {
                            isTyping = true
                            expanded = true // Show filtered list
                            focusRequester.requestFocus()
                        }
                )

                // Right Half: Dropdown Selection Mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("AzRollerRight")
                        .clickable {
                            isTyping = false
                            expanded = true
                            focusManager.clearFocus() // Hide keyboard
                        }
                )
            }

        if (expanded && enabled) {
            val density = LocalDensity.current
            val itemHeight = with(density) { componentHeight.toDp() }
            val visibleItemsAbove = 2
            val visibleItemsBelow = 2
            val popupYOffset = -(componentHeight * visibleItemsAbove)

            val filteredOptions = remember(text, options, isTyping) {
                if (isTyping && text.isNotEmpty()) {
                     options.filter { it.contains(text, ignoreCase = true) }
                } else {
                     options
                }
            }

            val fullList = remember(filteredOptions) {
                val topSpacers = List(visibleItemsAbove + 1) { null }
                val bottomSpacers = List(visibleItemsBelow) { null }
                topSpacers + filteredOptions + bottomSpacers
            }

            // Initial Scroll: Start at index 0 (Top Spacer visible, List Below)
            // Or jump to selection if NOT typing.
            val initialIndex = remember(options, text, isTyping) {
                if (isTyping) 0 else {
                    val idx = options.indexOf(text)
                    if (idx != -1) idx + 1 else 0
                }
            }

            val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
            val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

            // Fix Scroll Reset: When typing (filtering changes), reset scroll to top.
            LaunchedEffect(filteredOptions) {
                if (isTyping) {
                    listState.scrollToItem(0)
                }
            }

            // Auto-select item when scrolling stops
            LaunchedEffect(listState, isTyping) {
                snapshotFlow { listState.firstVisibleItemIndex }
                    .distinctUntilChanged()
                    .collect { index ->
                        if (!listState.isScrollInProgress && !isTyping) {
                            val slotIndex = index + 2
                            val item = fullList.getOrNull(slotIndex)

                            if (item != null) {
                                onOptionSelected(item)
                            }
                        }
                    }
            }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, popupYOffset),
                properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true),
                onDismissRequest = {
                    if (!isTyping) {
                        val index = listState.firstVisibleItemIndex
                        val slotIndex = index + 2
                        val item = fullList.getOrNull(slotIndex)
                        if (item != null) {
                            onOptionSelected(item)
                        }
                    }
                    expanded = false
                }
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
                        modifier = Modifier.fillMaxWidth().background(Color.Transparent),
                    ) {
                        itemsIndexed(fullList) { index, option ->
                            val isSelected = option == text
                            val isSpacer = option == null

                            if (isSpacer) {
                                Box(modifier = Modifier.height(itemHeight).fillMaxWidth())
                            } else {
                                Box(
                                    modifier = Modifier
                                        .height(itemHeight)
                                        .fillMaxWidth()
                                        .background(if (isSelected && !isTyping) effectiveColor.copy(alpha = 0.1f) else surfaceColor)
                                        .clickable {
                                            if (option != null) {
                                                onOptionSelected(option)
                                                expanded = false
                                            }
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (option != null) {
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
                        }
                    }

                    // Selection indicator (The "Slot")
                    Box(
                        modifier = Modifier
                            .padding(top = itemHeight * visibleItemsAbove)
                            .height(itemHeight)
                            .fillMaxWidth()
                            .border(2.dp, if (!isTyping) effectiveColor.copy(alpha = 0.5f) else Color.Transparent)
                    )
                }
            }
        }
    }
}
