package com.hereliesaz.aznavrail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.model.AzNavItem

@Composable
fun AzNavRail(
    content: @Composable AzNavRailScope.() -> Unit
) {
    val scope = remember(content) { AzNavRailScopeImpl().apply(content) }
    val navItems = scope.navItems
    val displayAppNameInHeader = scope.displayAppNameInHeader
    val packRailButtons = scope.packRailButtons

    val context = LocalContext.current
    val appName = try {
        val packageManager = context.packageManager
        val packageName = context.packageName
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) { "App" }

    val appIcon = try {
        context.packageManager.getApplicationIcon(context.packageName)
    } catch (e: Exception) { null }

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val onToggle: () -> Unit = { isExpanded = !isExpanded }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 80.dp,
        label = "railWidth"
    )

    NavigationRail(
        modifier = Modifier.width(railWidth),
        header = {
            IconButton(onClick = onToggle, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                if (displayAppNameInHeader) {
                    Text(text = appName, style = MaterialTheme.typography.titleMedium)
                } else {
                    if (appIcon != null) {
                        Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "App Icon", modifier = Modifier.size(56.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", modifier = Modifier.size(56.dp))
                    }
                }
            }
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isExpanded) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    navItems.forEach { item ->
                        MenuItem(item = item)
                    }
                }
                Footer(appName = appName)
            } else {
                val railItems = navItems.filter { it.isRailItem }
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (packRailButtons) Arrangement.Top else Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    if (packRailButtons) {
                        railItems.forEach { item ->
                            RailContent(item = item)
                        }
                    } else {
                        navItems.forEach { menuItem ->
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
                putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@example.com"))
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
