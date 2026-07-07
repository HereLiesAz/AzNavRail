package com.hereliesaz.aznavrail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.hereliesaz.aznavrail.internal.AzIcons

/**
 * A versatile dropdown component that combines a text filter with a list of options.
 *
 * `AzRoller` behaves like a "slot machine" dropdown.
 * - Clicking the text area allows the user to type and filter the options.
 * - Clicking the dropdown arrow toggles the full list.
 * - When typing, the dropdown automatically expands to show filtered results.
 * - Supports custom values (users can type and submit a value not in the list).
 *
 * Pressing the keyboard "Done" key selects the top filtered option (or commits the typed value
 * verbatim when there are no matches). Selecting from the list or pressing the keyboard always
 * dismisses the popup and re-syncs `filterText` with [selectedOption].
 *
 * Example:
 * ```
 * var country by remember { mutableStateOf<String?>(null) }
 * AzRoller(
 *     options = listOf("USA", "Canada", "Mexico"),
 *     selectedOption = country,
 *     onOptionSelected = { country = it },
 *     hint = "Country",
 * )
 * ```
 *
 * @param options The list of options shown in the dropdown.
 * @param selectedOption The currently selected option; drives the filter text when collapsed.
 * @param onOptionSelected Callback fired when the user picks (or types) an option.
 * @param hint The hint text displayed when the input is empty.
 * @param enabled Whether the component is interactive.
 * @param isError Whether the component is in an error state.
 */
@Composable
fun AzRoller(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    hint: String = "",
    enabled: Boolean = true,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf(selectedOption ?: "") }

    LaunchedEffect(selectedOption, expanded) {
        if (!expanded) {
            filterText = selectedOption ?: ""
        }
    }

    val filteredOptions = remember(filterText, options) {
        if (filterText.isEmpty()) options else options.filter { it.contains(filterText, ignoreCase = true) }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(filteredOptions) {
        listState.scrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        AzTextBox(
            value = filterText,
            onValueChange = { 
                filterText = it
                expanded = true
            },
            hint = hint,
            enabled = enabled,
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = AzIcons.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier
                        .testTag("AzRollerRight")
                        .clickable(enabled = enabled) { 
                            expanded = !expanded 
                            if (expanded) filterText = ""
                        }
                )
            },
            onSubmit = { 
                if (filteredOptions.isNotEmpty()) {
                    onOptionSelected(filteredOptions.first())
                } else {
                    onOptionSelected(filterText)
                }
                expanded = false
            }
        )

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    LazyColumn(state = listState) {
                        items(filteredOptions) { option ->
                            Text(
                                text = option,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        filterText = option
                                        onOptionSelected(option)
                                        expanded = false
                                    }
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
