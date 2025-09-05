package com.hereliesaz.aznavrail

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.model.AzNavItem

@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
) {
    val scope = remember(content) { AzNavRailScopeImpl().apply(content) }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName

    val appName = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        Log.e("AzNavRail", "Error getting app name", e)
        "App" // Fallback name
    }

    val appIcon = try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        Log.e("AzNavRail", "Error getting app icon", e)
        null // Fallback to default icon
    }

    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val onToggle: () -> Unit = { isExpanded = !isExpanded }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 80.dp,
        label = "railWidth"
    )

    NavigationRail(
        modifier = modifier
            .width(railWidth)
            .pointerInput(isExpanded, disableSwipeToOpen) {
                if (isExpanded) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, _) = dragAmount
                        if (x < -20) { onToggle() }
                    }
                } else if (!disableSwipeToOpen) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, _) = dragAmount
                        if (x > 20) { onToggle() }
                    }
                }
            },
        containerColor = Color.Transparent,
        header = {
            IconButton(onClick = onToggle, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                if (scope.displayAppNameInHeader) {
                    Text(text = if (isExpanded) appName else appName.first().toString(), style = MaterialTheme.typography.titleMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp)) {
                            if (appIcon != null) {
                                Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "App Icon")
                            } else {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                        if (isExpanded) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = appName, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isExpanded) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    scope.navItems.forEach { item ->
                        MenuItem(item = item)
                    }
                }
                Footer(appName = appName)
            } else {
                val railItems = scope.navItems.filter { it.isRailItem }
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (scope.packRailButtons) Arrangement.Top else Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    if (scope.packRailButtons) {
                        railItems.forEach { item ->
                            RailContent(item = item)
                        }
                    } else {
                        scope.navItems.forEach { menuItem ->
                            val railItem = railItems.find { it.id == menuItem.id }
                            if (railItem != null) {
                                RailContent(item = railItem)
                            } else {
                                Spacer(modifier = Modifier.height(72.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RailContent(item: AzNavItem) {
    val textToShow = if (item.isCycler) item.selectedOption ?: "" else item.text
    AzNavRailButton(
        onClick = item.onClick,
        text = textToShow,
        color = item.color ?: MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun MenuItem(item: AzNavItem) {
    val textToShow = if (item.isCycler) "${item.text}: ${item.selectedOption}" else item.text

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = textToShow, style = MaterialTheme.typography.bodyMedium)
        if (item.isToggle) {
            Spacer(modifier = Modifier.weight(1f))
            Text(text = if (item.isChecked == true) "On" else "Off")
        }
    }
}

@Composable
private fun Footer(appName: String) {
    val context = LocalContext.current
    Column {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        MenuItem(item = AzNavItem(id = "about", text = "About", isRailItem = false, onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HereLiesAz/$appName"))
            context.startActivity(intent)
        }))
        MenuItem(item = AzNavItem(id = "feedback", text = "Feedback", isRailItem = false, onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("hereliesaz@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Feedback for $appName")
            }
            context.startActivity(Intent.createChooser(intent, "Send Feedback"))
        }))
        MenuItem(
            item = AzNavItem(
                id = "credit",
                text = "@HereLiesAz",
                isRailItem = false,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/hereliesaz"))
                    context.startActivity(intent)
                }
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
