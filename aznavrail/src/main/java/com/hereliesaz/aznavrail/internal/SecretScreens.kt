package com.hereliesaz.aznavrail.internal

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.model.AzButtonShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hereliesaz.aznavrail.BuildConfig

@Composable
internal fun SecretScreens(
    secLoc: String?,
): () -> Unit {
    if (secLoc.isNullOrEmpty()) return {}

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        SecLocMainDialog(
            secLoc = secLoc,
            onDismiss = { showDialog = false }
        )
    }

    return { showDialog = true }
}

@Composable
private fun SecLocMainDialog(
    secLoc: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Check for Phone State Permission (needed to verify identity)
    val hasPhonePermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
         ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    if (!hasPhonePermission) {
        // Fallback: If we can't verify identity, we can't determine role.
        // Show generic permission request message.
        PermissionRequiredDialog(onDismiss, "Phone permissions (READ_PHONE_STATE or READ_PHONE_NUMBERS) are required to verify device identity.")
        return
    }

    // Determine Role
    val deviceNumber = remember(context) { getDevicePhoneNumber(context) }

    // Normalize numbers for comparison (remove spaces, dashes, etc.)
    val isSourceDevice = remember(deviceNumber, secLoc) {
        val cleanDevice = deviceNumber?.replace(Regex("[^0-9]"), "")
        val cleanSecLoc = secLoc.replace(Regex("[^0-9]"), "")
        // Simple check: do they match? (Handling nulls)
        !cleanDevice.isNullOrEmpty() && (cleanDevice == cleanSecLoc || cleanDevice.endsWith(cleanSecLoc) || cleanSecLoc.endsWith(cleanDevice))
    }

    if (isSourceDevice) {
        SecLocSourceDialog(onDismiss = onDismiss)
    } else {
        SecLocViewerFlow(secLoc = secLoc, onDismiss = onDismiss)
    }
}

@Composable
private fun PermissionRequiredDialog(onDismiss: () -> Unit, message: String) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Permission Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                AzButton(onClick = onDismiss, text = "Close")
            }
        }
    }
}

@Composable
private fun SecLocSourceDialog(onDismiss: () -> Unit) {
    // This is the source device. It should log locations.
    // It also displays the current log.
    SecLocHistoryDialog(
        isSource = true,
        onDismiss = onDismiss
    )
}

@Composable
private fun SecLocViewerFlow(secLoc: String, onDismiss: () -> Unit) {
    var isAuthenticated by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        SecretCredentialsDialog(
            secLoc = secLoc,
            onDismiss = onDismiss,
            onUnlock = { isAuthenticated = true }
        )
    } else {
        // Viewer Mode: "Download" log
        SecLocViewerDialog(onDismiss = onDismiss)
    }
}

@Composable
private fun SecretCredentialsDialog(
    secLoc: String?,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
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
                    text = "Enter the secLoc number or Build PIN.",
                    style = MaterialTheme.typography.bodyMedium
                )

                var error by remember { mutableStateOf(false) }

                AzTextBox(
                    hint = "Enter credentials...",
                    secret = true,
                    isError = error,
                    onSubmit = { input ->
                        // Validate against secLoc (phone) OR generated build PIN
                        val buildPin = BuildConfig.GENERATED_SEC_LOC_PIN
                        if (input == secLoc || input == buildPin) {
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

@Composable
private fun SecLocViewerDialog(onDismiss: () -> Unit) {
    // Simulate downloading
    var isLoading by remember { mutableStateOf(true) }
    val history = remember { mutableStateListOf<SecLocEntry>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            delay(1500) // Simulate network delay
            // In a real app, this would fetch from a server.
            // For now, we return a placeholder or empty list.
            // Or maybe strictly speaking we can't download.
            // But we must show the screen.
            isLoading = false
        }
    }

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
                    text = "Location History (Viewer)",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Downloading log from source device...")
                        }
                    }
                } else {
                    if (history.isEmpty()) {
                         Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No remote logs found (or sync not implemented).")
                         }
                    } else {
                        HistoryList(history = history)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                AzButton(onClick = onDismiss, text = "Close", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun SecLocHistoryDialog(
    isSource: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val secLocHistory = remember { mutableStateListOf<SecLocEntry>() }
    val coroutineScope = rememberCoroutineScope()

    // Permission check for LOCATION
    val hasLocPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Load initial history from file
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val saved = SecLocLogManager.readLog(context)
            secLocHistory.addAll(saved)
        }
    }

    DisposableEffect(hasLocPermission, isSource) {
        if (hasLocPermission && isSource) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val entry = SecLocEntry(
                        timestamp = location.time,
                        lat = location.latitude,
                        lng = location.longitude,
                        provider = location.provider ?: "unknown"
                    )
                    secLocHistory.add(0, entry)
                    // Save to file
                    coroutineScope.launch {
                        SecLocLogManager.appendLog(context, entry)
                    }
                }
                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                val providers = locationManager.getProviders(true)
                if (providers.contains(LocationManager.GPS_PROVIDER)) {
                     locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, listener)
                }
                if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                     locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, listener)
                }
            } catch (e: Exception) {
                AzNavRailLogger.e("SecretScreens", "Error requesting location updates", e)
            }

            onDispose { locationManager.removeUpdates(listener) }
        } else {
            onDispose { }
        }
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
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Text(
                    text = if (isSource) "Source Device Log" else "Location History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (!hasLocPermission && isSource) {
                    Text("Location permission required to log data.", color = MaterialTheme.colorScheme.error)
                }

                HistoryList(history = secLocHistory, modifier = Modifier.weight(1f))

                Spacer(modifier = Modifier.height(16.dp))
                AzButton(onClick = onDismiss, text = "Close", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HistoryList(history: List<SecLocEntry>, modifier: Modifier = Modifier) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(history) { entry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = dateFormatter.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Lat: ${entry.lat}, Lng: ${entry.lng}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(text = "Provider: ${entry.provider}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

data class SecLocEntry(
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val provider: String
)

private object SecLocLogManager {
    private const val FILE_NAME = "secloc_history.log"

    suspend fun appendLog(context: Context, entry: SecLocEntry) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val line = "${entry.timestamp},${entry.lat},${entry.lng},${entry.provider}\n"
            file.appendText(line)
        } catch (e: Exception) {
            AzNavRailLogger.e("SecLocLogManager", "Error saving log", e)
        }
    }

    suspend fun readLog(context: Context): List<SecLocEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SecLocEntry>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.forEachLine { line ->
                    val parts = line.split(",")
                    if (parts.size >= 4) {
                        list.add(
                            SecLocEntry(
                                timestamp = parts[0].toLongOrNull() ?: 0L,
                                lat = parts[1].toDoubleOrNull() ?: 0.0,
                                lng = parts[2].toDoubleOrNull() ?: 0.0,
                                provider = parts[3]
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            AzNavRailLogger.e("SecLocLogManager", "Error reading log", e)
        }
        // Return reversed to show newest first
        list.reversed()
    }
}

@SuppressLint("MissingPermission")
private fun getDevicePhoneNumber(context: Context): String? {
    return try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        @Suppress("DEPRECATION")
        tm?.line1Number
    } catch (e: SecurityException) {
        null
    } catch (e: Exception) {
        null
    }
}
