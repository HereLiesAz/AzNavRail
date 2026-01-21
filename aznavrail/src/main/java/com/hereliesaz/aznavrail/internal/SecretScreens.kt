package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.model.AzButtonShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SecretCredentialsDialog(
    secLoc: String?,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    if (secLoc.isNullOrEmpty()) {
        onUnlock()
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Developer Credentials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter the secret location identifier to access history.",
                    style = MaterialTheme.typography.bodyMedium
                )

                var error by remember { mutableStateOf(false) }

                AzTextBox(
                    hint = "Enter credentials...",
                    secret = true,
                    isError = error,
                    onSubmit = { input ->
                        if (input == secLoc) {
                            onUnlock()
                        } else {
                            error = true
                        }
                    },
                    submitButtonContent = {
                        Text("Unlock")
                    }
                )
            }
        }
    }
}

data class LocationEntry(
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val provider: String
)

@Composable
internal fun LocationHistoryDialog(
    onDismiss: () -> Unit
) {
    // Mock data
    val history = remember {
        mutableStateListOf(
            LocationEntry(System.currentTimeMillis() - 3600000, 37.7749, -122.4194, "gps"),
            LocationEntry(System.currentTimeMillis() - 7200000, 37.7750, -122.4183, "network"),
            LocationEntry(System.currentTimeMillis() - 10800000, 37.7751, -122.4172, "fused")
        )
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Location History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = dateFormatter.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Lat: ${entry.lat}, Lng: ${entry.lng}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Provider: ${entry.provider}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AzButton(
                    onClick = {
                        // Add mock location update
                        val lat = 37.7749 + (Math.random() - 0.5) * 0.01
                        val lng = -122.4194 + (Math.random() - 0.5) * 0.01
                        history.add(0, LocationEntry(System.currentTimeMillis(), lat, lng, "mock"))
                    },
                    text = "Update Location",
                    modifier = Modifier.fillMaxWidth(),
                    shape = AzButtonShape.RECTANGLE
                )

                Spacer(modifier = Modifier.height(8.dp))

                AzButton(
                    onClick = onDismiss,
                    text = "Close",
                    modifier = Modifier.fillMaxWidth(),
                    shape = AzButtonShape.RECTANGLE,
                    // Typically close buttons might look different, but keeping style consistent
                )
            }
        }
    }
}
