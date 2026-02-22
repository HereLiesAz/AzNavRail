// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzButton.kt
package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AzButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentPadding: PaddingValues? = null
) {
    AzNavRailButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        color = color,
        activeColor = activeColor,
        colors = colors,
        size = AzNavRailDefaults.ButtonWidth,
        shape = shape,
        enabled = enabled,
        isSelected = false,
        isLoading = isLoading,
        contentPadding = contentPadding ?: PaddingValues(8.dp)
    )
}

@Composable
fun AzToggle(
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    toggleOnText: String,
    toggleOffText: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true
) {
    val text = if (isChecked) toggleOnText else toggleOffText
    AzNavRailButton(
        onClick = { onToggle(!isChecked) },
        text = text,
        color = color,
        activeColor = activeColor,
        colors = colors,
        size = AzNavRailDefaults.ButtonWidth,
        shape = shape,
        enabled = enabled,
        isSelected = false
    )
}

@Composable
fun AzCycler(
    options: List<String>,
    selectedOption: String,
    onCycle: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true
) {
    var displayedOption by rememberSaveable(selectedOption) { mutableStateOf(selectedOption) }
    var job by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedOption) {
        job?.cancel()
        job = null
        displayedOption = selectedOption
    }

    AzNavRailButton(
        onClick = {
            job?.cancel()
            val currentIndex = options.indexOf(displayedOption)
            val nextIndex = (currentIndex + 1) % options.size
            displayedOption = options[nextIndex]
            job = coroutineScope.launch {
                delay(1000L)
                onCycle(displayedOption)
            }
        },
        text = displayedOption,
        color = color,
        activeColor = activeColor,
        colors = colors,
        size = AzNavRailDefaults.ButtonWidth,
        shape = shape,
        enabled = enabled,
        isSelected = false
    )
}