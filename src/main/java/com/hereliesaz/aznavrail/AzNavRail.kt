package com.hereliesaz.aznavrail

import android.content.ActivityNotFoundException
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

private object AzNavRailDefaults {
    val ExpandedRailWidth = 260.dp
    val CollapsedRailWidth = 80.dp
    const val SWIPE_THRESHOLD_PX = 20f
    val HeaderPadding = 16.dp
    val HeaderIconSize = 56.dp
    val HeaderTextSpacer = 8.dp
    val RailContentHorizontalPadding = 4.dp
    val RailContentVerticalArrangement = 8.dp
    val RailContentSpacerHeight = 72.dp
    val MenuItemHorizontalPadding = 24.dp
    val MenuItemVerticalPadding = 12.dp
    val FooterDividerHorizontalPadding = 16.dp
    val FooterDividerVerticalPadding = 8.dp
    val FooterSpacerHeight = 12.dp
}

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

    val appName = remember(packageName) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            Log.e("AzNavRail", "Error getting app name", e)
            "App" // Fallback name
        }
    }

    val appIcon = remember(packageName) {
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            Log.e("AzNavRail", "Error getting app icon", e)
            null // Fallback to default icon
        }
    }

    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val onToggle: () -> Unit = remember { { isExpanded = !isExpanded } }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) AzNavRailDefaults.ExpandedRailWidth else AzNavRailDefaults.CollapsedRailWidth,
        label = "railWidth"
    )

    NavigationRail(
        modifier = modifier
            .width(railWidth)
            .pointerInput(isExpanded, disableSwipeToOpen) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val (x, _) = dragAmount
                    if (isExpanded) {
                        if (x < -AzNavRailDefaults.SWIPE_THRESHOLD_PX) { onToggle() }
                    } else if (!disableSwipeToOpen) {
                        if (x > AzNavRailDefaults.SWIPE_THRESHOLD_PX) { onToggle() }
                    }
                }
            },
        containerColor = Color.Transparent,
        header = {
            IconButton(onClick = onToggle, modifier = Modifier.padding(top = AzNavRailDefaults.HeaderPadding, bottom = AzNavRailDefaults.HeaderPadding)) {
                if (scope.displayAppNameInHeader) {
                    Text(text = if (isExpanded) appName else appName.firstOrNull()?.toString() ?: "", style = MaterialTheme.typography.titleMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize)) {
                            if (appIcon != null) {
                                Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "$appName Icon")
                            } else {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Toggle Menu")
                            }
                        }
                        if (isExpanded) {
                            Spacer(modifier = Modifier.width(AzNavRailDefaults.HeaderTextSpacer))
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
                    modifier = Modifier.padding(horizontal = AzNavRailDefaults.RailContentHorizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (scope.packRailButtons) Arrangement.Top else Arrangement.spacedBy(AzNavRailDefaults.RailContentVerticalArrangement, Alignment.CenterVertically)
                ) {
                    if (scope.packRailButtons) {
                        railItems.forEach { item ->
                            RailContent(item = item)
                        }
                    } else {
                        scope.navItems.forEach { menuItem ->
                            if (menuItem.isRailItem) {
                                RailContent(item = menuItem)
                            } else {
                                Spacer(modifier = Modifier.height(AzNavRailDefaults.RailContentSpacerHeight))
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
            .padding(horizontal = AzNavRailDefaults.MenuItemHorizontalPadding, vertical = AzNavRailDefaults.MenuItemVerticalPadding),
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
    val onAboutClick: () -> Unit = remember(context, appName) {
        {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HereLiesAz/$appName"))
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("AzNavRail.Footer", "Could not open 'About' link.", e)
            }
        }
    }
    val onFeedbackClick: () -> Unit = remember(context, appName) {
        {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("hereliesaz@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback for $appName")
                }
                context.startActivity(Intent.createChooser(intent, "Send Feedback"))
            } catch (e: ActivityNotFoundException) {
                Log.e("AzNavRail.Footer", "Could not open 'Feedback' link.", e)
            }
        }
    }
    val onCreditClick: () -> Unit = remember(context) {
        {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/hereliesaz"))
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("AzNavRail.Footer", "Could not open 'Credit' link.", e)
            }
        }
    }

    Column {
        HorizontalDivider(modifier = Modifier.padding(horizontal = AzNavRailDefaults.FooterDividerHorizontalPadding, vertical = AzNavRailDefaults.FooterDividerVerticalPadding), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        MenuItem(item = AzNavItem(id = "about", text = "About", isRailItem = false, onClick = onAboutClick))
        MenuItem(item = AzNavItem(id = "feedback", text = "Feedback", isRailItem = false, onClick = onFeedbackClick))
        MenuItem(
            item = AzNavItem(
                id = "credit",
                text = "@HereLiesAz",
                isRailItem = false,
                onClick = onCreditClick
            )
        )
        Spacer(modifier = Modifier.height(AzNavRailDefaults.FooterSpacerHeight))
    }
}
