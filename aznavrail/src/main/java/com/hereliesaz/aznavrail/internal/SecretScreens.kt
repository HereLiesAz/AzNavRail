package com.hereliesaz.aznavrail.internal

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

data class SecLocEntry(
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val provider: String
)

@SuppressLint("MissingPermission")
@Composable
internal fun SecLocHistoryDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val secLocHistory = remember { mutableStateListOf<SecLocEntry>() }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            hasPermission = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    secLocHistory.add(0, SecLocEntry(
                        timestamp = location.time,
                        lat = location.latitude,
                        lng = location.longitude,
                        provider = location.provider ?: "unknown"
                    ))
                }
                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                // Request updates from GPS and Network providers
                val providers = locationManager.getProviders(true)
                if (providers.contains(LocationManager.GPS_PROVIDER)) {
                     locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000L, // 5 seconds
                        5f,    // 5 meters
                        listener
                    )
                }
                if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                     locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000L,
                        5f,
                        listener
                    )
                }

                // Get last known location to start with
                val lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastKnownNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                val lastKnown = lastKnownGps ?: lastKnownNet

                if (lastKnown != null) {
                     secLocHistory.add(0, SecLocEntry(
                        timestamp = lastKnown.time,
                        lat = lastKnown.latitude,
                        lng = lastKnown.longitude,
                        provider = lastKnown.provider ?: "last_known"
                    ))
                }

            } catch (e: Exception) {
                // Handle security exception or other errors
                AzNavRailLogger.e("SecretScreens", "Error requesting location updates", e)
            }

            onDispose {
                locationManager.removeUpdates(listener)
            }
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Location History (SecLoc)",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (!hasPermission) {
                    Text(
                        text = "Location permission is required to show history.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(secLocHistory) { entry ->
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                AzButton(
                    onClick = onDismiss,
                    text = "Close",
                    modifier = Modifier.fillMaxWidth(),
                    shape = AzButtonShape.RECTANGLE,
                )
            }
        }
    }
}
