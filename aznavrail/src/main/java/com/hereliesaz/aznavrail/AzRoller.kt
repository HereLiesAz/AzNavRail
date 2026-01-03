package com.hereliesaz.aznavrail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * A dropdown menu that works like a roller or slot machine, cycling through options infinitely.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param options The list of options to display.
 * @param selectedOption The currently selected option.
 * @param onOptionSelected Called when an option is selected.
 * @param hint The text to display when no option is selected.
 * @param enabled Controls the enabled state of the component.
 * @param outlineColor The color of the border and text.
 */
@Composable
fun AzRoller(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedOption: String? = null,
    onOptionSelected: (String) -> Unit,
    hint: String = "",
    enabled: Boolean = true,
    outlineColor: Color = MaterialTheme.colorScheme.primary
) {
    var expanded by remember { mutableStateOf(false) }
    var componentHeight by remember { mutableIntStateOf(0) }
    var componentWidth by remember { mutableIntStateOf(0) }

    val effectiveColor = if (!enabled) {
        outlineColor.copy(alpha = 0.5f)
    } else {
        outlineColor
    }

    val backgroundColor = AzTextBoxDefaults.getBackgroundColor().copy(alpha = AzTextBoxDefaults.getBackgroundOpacity())

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .background(backgroundColor)
                .border(1.dp, effectiveColor)
                .onSizeChanged {
                    componentHeight = it.height
                    componentWidth = it.width
                }
                .clickable(enabled = enabled) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val text = selectedOption ?: hint
                Text(
                    text = text,
                    fontSize = 10.sp,
                    color = if (selectedOption == null) effectiveColor.copy(alpha = 0.5f) else effectiveColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            CompositionLocalProvider(LocalContentColor provides effectiveColor) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.size(24.dp).padding(end = 4.dp),
                    tint = effectiveColor
                )
            }
        }

        if (expanded && enabled && options.isNotEmpty()) {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = Int.MAX_VALUE / 2)

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, componentHeight),
                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
                onDismissRequest = { expanded = false }
            ) {
                val density = LocalDensity.current
                val surfaceColor = MaterialTheme.colorScheme.surface

                Box(
                    modifier = Modifier
                        .width(with(density) { componentWidth.toDp() })
                        .height(200.dp)
                        .background(surfaceColor)
                        .border(1.dp, effectiveColor.copy(alpha = 0.5f))
                ) {
                    LazyColumn(
                        state = listState
                    ) {
                        items(Int.MAX_VALUE) { index ->
                            val option = options[index % options.size]
                            val isSelected = option == selectedOption
                            val itemBgColor = if (isSelected) {
                                effectiveColor.copy(alpha = 0.1f)
                            } else if (index % 2 == 0) {
                                surfaceColor.copy(alpha = 0.9f)
                            } else {
                                surfaceColor.copy(alpha = 0.8f)
                            }

                            Text(
                                text = option,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(itemBgColor)
                                    .clickable {
                                        onOptionSelected(option)
                                        expanded = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
