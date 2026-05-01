package com.hereliesaz.aznavrail.internal

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * A secret menu for debugging and location history syncing.
 * Activated by long-pressing the @HereLiesAz footer item when a `secLoc`
 * (Developer Configuration) is passed.
 * Restored to 6.99 visual style (Rounded corners).
 */
@Composable
internal fun SecretScreens(
    secLoc: String?,
    secLocPort: Int
): () -> Unit {
    if (secLoc.isNullOrEmpty()) return {}

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        SecLocMainDialog(
            secLoc = secLoc,
            secLocPort = secLocPort,
            onDismiss = { showDialog = false }
        )
    }

    return { showDialog = true }
}

@Composable
private fun SecLocMainDialog(
    secLoc: String,
    secLocPort: Int,
    onDismiss: () -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf<String?>(null) }

    if (!isAuthenticated) {
        SecretCredentialsDialog(
            secLoc = secLoc,
            onDismiss = onDismiss,
            onUnlock = { isAuthenticated = true }
        )
    } else if (selectedMode == null) {
        ModeSelectionDialog(
            onDismiss = onDismiss,
            onSelectMode = { selectedMode = it }
        )
    } else if (selectedMode == "SOURCE") {
        SecLocSourceDialog(secLoc = secLoc, secLocPort = secLocPort, onDismiss = onDismiss)
    } else {
        SecLocViewerDialog(secLoc = secLoc, secLocPort = secLocPort, onDismiss = onDismiss)
    }
}

@Composable
private fun ModeSelectionDialog(onDismiss: () -> Unit, onSelectMode: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Operating Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                AzButton(onClick = { onSelectMode("SOURCE") }, text = "Run as Source (Server)")
                Spacer(modifier = Modifier.height(8.dp))
                AzButton(onClick = { onSelectMode("VIEWER") }, text = "Run as Viewer (Client)")
                Spacer(modifier = Modifier.height(16.dp))
                AzButton(onClick = onDismiss, text = "Cancel")
            }
        }
    }
}

@Composable
private fun SecretCredentialsDialog(
    secLoc: String,
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
                    text = "Enter the configuration key or Build PIN.",
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

@Composable
private fun SecLocSourceDialog(secLoc: String, secLocPort: Int, onDismiss: () -> Unit) {
    SecLocHistoryDialog(
        secLoc = secLoc,
        secLocPort = secLocPort,
        isSource = true,
        onDismiss = onDismiss
    )
}

@Composable
private fun SecLocViewerDialog(secLoc: String, secLocPort: Int, onDismiss: () -> Unit) {
    var sourceIp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<SecLocEntry>() }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val shape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(shape)
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

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sync from Source Device", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        AzTextBox(
                            hint = "Enter Source IP (e.g. 192.168.1.5)",
                            value = sourceIp,
                            onValueChange = { sourceIp = it },
                            onSubmit = { /* Optional auto-submit */ }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AzButton(
                            text = if (isLoading) "Downloading..." else "Download Log",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (sourceIp.isBlank()) {
                                    errorMsg = "Please enter Source IP"
                                    return@AzButton
                                }
                                isLoading = true
                                errorMsg = null
                                coroutineScope.launch {
                                    try {
                                        val logs = SecLocNetworkUtils.fetchLogs(sourceIp, secLoc, secLocPort)
                                        history.clear()
                                        history.addAll(logs)
                                        if (logs.isEmpty()) {
                                            errorMsg = "No logs received."
                                        }
                                    } catch (e: Exception) {
                                        errorMsg = "Sync failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        )
                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                    HistoryList(history = history, dateFormatter = dateFormatter, modifier = Modifier.weight(1f))
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
    secLoc: String,
    secLocPort: Int,
    isSource: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val secLocHistory = remember { mutableStateListOf<SecLocEntry>() }
    val coroutineScope = rememberCoroutineScope()
    var serverIp by remember { mutableStateOf<String?>(null) }

    val hasLocPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val saved = SecLocLogManager.readLog(context)
            secLocHistory.addAll(saved)
        }
        if (isSource) {
            serverIp = SecLocNetworkUtils.getLocalIpAddress()
            coroutineScope.launch(Dispatchers.IO) {
                SecLocNetworkUtils.startServer(context, secLoc, secLocPort)
            }
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
                Log.e("SecretScreens", "Error requesting location updates", e)
            }

            onDispose { locationManager.removeUpdates(listener) }
        } else {
            onDispose { }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            SecLocNetworkUtils.stopServer()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val shape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(shape)
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

                if (isSource) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Sync Server Running", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text("Viewer can connect to: ${serverIp ?: "Finding IP..."}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (!hasLocPermission && isSource) {
                    Text("Location permission required to log data.", color = MaterialTheme.colorScheme.error)
                }

                HistoryList(history = secLocHistory, dateFormatter = dateFormatter, modifier = Modifier.weight(1f))

                Spacer(modifier = Modifier.height(16.dp))
                AzButton(onClick = onDismiss, text = "Close", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Scrollable list of [SecLocEntry] items. Tapping an entry opens the coordinates in a maps app.
 *
 * @param history Entries to display (most recent first).
 * @param dateFormatter Formatter applied to [SecLocEntry.timestamp].
 * @param modifier Applied to the outer [androidx.compose.foundation.lazy.LazyColumn].
 */
@Composable
internal fun HistoryList(
    history: List<SecLocEntry>,
    dateFormatter: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(history) { entry ->
            val context = LocalContext.current
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().clickable {
                    try {
                        if (entry.lat in -90.0..90.0 && entry.lng in -180.0..180.0) {
                            val uri = Uri.parse("geo:${entry.lat},${entry.lng}?q=${entry.lat},${entry.lng}")
                            if (uri.scheme == "geo") {
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } else {
                                Log.w("SecretScreens", "Invalid URI scheme: ${uri.scheme}")
                            }
                        } else {
                            Log.w("SecretScreens", "Invalid coordinates: lat=${entry.lat}, lng=${entry.lng}")
                        }
                    } catch (e: Exception) {
                        Log.e("SecretScreens", "Could not open map", e)
                    }
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = dateFormatter.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lat: ${entry.lat}, Lng: ${entry.lng}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "Provider: ${entry.provider}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * A single recorded location data point from the SecLoc system.
 *
 * @param timestamp Unix timestamp in milliseconds.
 * @param lat Latitude in decimal degrees.
 * @param lng Longitude in decimal degrees.
 * @param provider Location provider name (e.g., "gps", "network").
 */
data class SecLocEntry(
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val provider: String
)

/** Manages reading and writing the on-device SecLoc location log file. */
internal object SecLocLogManager {
    private const val FILE_NAME = "secloc_history.log"

    /** Appends [entry] as a pipe-delimited line to the log file. */
    suspend fun appendLog(context: Context, entry: SecLocEntry) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val line = "${entry.timestamp}|${entry.lat}|${entry.lng}|${entry.provider}\n"
            file.appendText(line)
        } catch (e: Exception) {
            Log.e("SecLocLogManager", "Error saving log", e)
        }
    }

    /** Reads all entries from the log file, returning them in reverse chronological order. */
    suspend fun readLog(context: Context): List<SecLocEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SecLocEntry>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.forEachLine { line ->
                    val parts = line.split("|")
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
            Log.e("SecLocLogManager", "Error reading log", e)
        }
        list.reversed()
    }

    /** Returns the [File] handle for the log file (used by the server to stream content). */
    fun getLogFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }
}

/**
 * Network utilities for the SecLoc source/viewer sync system.
 *
 * The source device starts a TCP server on [startServer]; the viewer connects via [fetchLogs].
 * Authentication is performed by comparing the raw secret string sent by the client against
 * the configured [com.hereliesaz.aznavrail.model.AzAdvancedConfig.secLoc] key.
 */
internal object SecLocNetworkUtils {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    /** Returns the device's non-loopback IPv4 address, or null if unavailable. */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    /**
     * Starts a blocking TCP server on [port] that streams the log file to authenticated clients.
     * A 3-second penalty delay is applied on authentication failure to deter brute-force attempts.
     */
    suspend fun startServer(context: Context, secret: String, port: Int) = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            while (isRunning) {
                try {
                    val client = serverSocket?.accept()
                    client?.let { socket ->
                        socket.soTimeout = 5000 // Prevent blocking indefinitely
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val clientSecret = reader.readLine()

                        val clientBytes = clientSecret?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
                        val secretBytes = secret.toByteArray(Charsets.UTF_8)

                        if (MessageDigest.isEqual(clientBytes, secretBytes)) {
                            val writer = PrintWriter(socket.getOutputStream(), true)
                            val file = SecLocLogManager.getLogFile(context)
                            if (file.exists()) {
                                file.forEachLine { line ->
                                    writer.println(line)
                                }
                            }
                            writer.flush()
                        } else {
                            Log.w("SecLocServer", "Unauthorized access attempt")
                            delay(3000L) // Prevent brute-force attacks
                        }
                        socket.close()
                    }
                } catch (e: Exception) {
                    if (isRunning) Log.e("SecLocServer", "Accept error", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SecLocServer", "Start error", e)
        } finally {
            stopServer()
        }
    }

    /** Stops the running server and closes its socket. Safe to call if the server was never started. */
    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverSocket = null
    }

    /**
     * Connects to the source device at [ip]:[port], authenticates with [secret], and downloads
     * the log entries, returning them in reverse chronological order.
     *
     * @throws Exception if the connection fails or authentication is rejected.
     */
    suspend fun fetchLogs(ip: String, secret: String, port: Int): List<SecLocEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SecLocEntry>()
        var socket: Socket? = null
        try {
            socket = Socket(ip, port)
            val writer = PrintWriter(socket.getOutputStream(), true)
            writer.println(secret)
            writer.flush()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var line: String? = reader.readLine()
            while (line != null) {
                val parts = line.split("|")
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
                line = reader.readLine()
            }
        } catch (e: Exception) {
            throw e
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) { }
        }
        list.reversed()
    }
}
