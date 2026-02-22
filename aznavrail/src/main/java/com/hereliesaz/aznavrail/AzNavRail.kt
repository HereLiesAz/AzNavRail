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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

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
                    imageVector = Icons.Default.ArrowDropDown,
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
                // STRICT RULE: No Rounded Corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RectangleShape)
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
