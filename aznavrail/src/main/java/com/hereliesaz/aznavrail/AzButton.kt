package com.hereliesaz.aznavrail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// --- AzButton ---

interface AzButtonScope {
    fun text(text: String)
    fun onClick(action: () -> Unit)
    fun color(color: Color)
}

private class AzButtonScopeImpl : AzButtonScope {
    var text: String = ""
    var onClick: () -> Unit = {}
    var color: Color? = null

    override fun text(text: String) {
        this.text = text
    }

    override fun onClick(action: () -> Unit) {
        this.onClick = action
    }

    override fun color(color: Color) {
        this.color = color
    }
}

@Composable
fun AzButton(
    modifier: Modifier = Modifier,
    content: AzButtonScope.() -> Unit
) {
    val scope = AzButtonScopeImpl().apply(content)
    AzNavRailButton(
        onClick = scope.onClick,
        text = scope.text,
        modifier = modifier,
        color = scope.color ?: MaterialTheme.colorScheme.primary
    )
}

// --- AzToggle ---

interface AzToggleScope {
    fun default(text: String, color: Color? = null)
    fun alt(text: String, color: Color? = null)
}

private class AzToggleScopeImpl : AzToggleScope {
    var defaultText: String = ""
    var defaultColor: Color? = null
    var altText: String = ""
    var altColor: Color? = null

    override fun default(text: String, color: Color?) {
        this.defaultText = text
        this.defaultColor = color
    }

    override fun alt(text: String, color: Color?) {
        this.altText = text
        this.altColor = color
    }
}

@Composable
fun AzToggle(
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: AzToggleScope.() -> Unit
) {
    val scope = remember { AzToggleScopeImpl() }.apply(content)

    val text = if (isOn) scope.altText else scope.defaultText
    val color = if (isOn) scope.altColor else scope.defaultColor

    AzNavRailButton(
        onClick = onToggle,
        text = text,
        modifier = modifier,
        color = color ?: MaterialTheme.colorScheme.primary
    )
}


// --- AzCycler ---

data class CyclerState(
    val text: String,
    val onClick: () -> Unit,
    val color: Color?
)

interface AzCyclerScope {
    fun state(text: String, onClick: () -> Unit, color: Color? = null)
}

private class AzCyclerScopeImpl : AzCyclerScope {
    val states = mutableListOf<CyclerState>()
    override fun state(text: String, onClick: () -> Unit, color: Color?) {
        states.add(CyclerState(text, onClick, color))
    }
}

@Composable
fun AzCycler(
    modifier: Modifier = Modifier,
    content: AzCyclerScope.() -> Unit
) {
    val scope = AzCyclerScopeImpl().apply(content)
    var currentIndex by rememberSaveable { mutableStateOf(0) }

    if (scope.states.isEmpty()) {
        return
    }

    val currentState = scope.states[currentIndex]

    AzNavRailButton(
        onClick = {
            currentState.onClick()
            currentIndex = (currentIndex + 1) % scope.states.size
        },
        text = currentState.text,
        modifier = modifier,
        color = currentState.color ?: MaterialTheme.colorScheme.primary
    )
}
