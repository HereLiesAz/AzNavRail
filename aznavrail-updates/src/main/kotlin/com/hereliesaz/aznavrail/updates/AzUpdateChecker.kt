package com.hereliesaz.aznavrail.updates

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** An update available from the same distribution channel as the installed application. */
data class AzUpdateOffer(val version: String, val notes: String = "")

/** Optional Play-flavor adapter; implementations must report availability using the Play update API. */
fun interface AzPlayUpdateSource {
    fun check(activity: Activity, callback: (AzUpdateOffer?) -> Unit)
}

/**
 * Nonblocking, opt-in Android update checker, usable from native, Compose and CMP activities.
 * GitHub installs use stable releases; Play installs use only the supplied Play adapter.
 * Unknown stores are left alone. No APK is downloaded or installed by this library.
 */
object AzUpdateChecker {
    private val executor = Executors.newSingleThreadExecutor()
    private val repositoryPattern = Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
    private val sideload = setOf(null, "com.android.shell", "com.android.packageinstaller", "com.google.android.packageinstaller")
    private const val interval = 6 * 60 * 60 * 1000L

    /**
     * Call from the main thread, after the activity is created. [manual] bypasses throttling
     * and version dismissal. A Play build may supply [playSource] without adding Play
     * dependencies to the GitHub flavor. Network failures never interrupt startup.
     */
    @JvmStatic
    @JvmOverloads
    fun check(activity: Activity, repository: String, manual: Boolean = false, playSource: AzPlayUpdateSource? = null) {
        require(repositoryPattern.matches(repository) && repository.split('/').none { it == "." || it == ".." })
        if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) return
        val prefs = activity.getSharedPreferences("az_update_checks", Context.MODE_PRIVATE)
        val pm = activity.packageManager
        @Suppress("DEPRECATION")
        val installer = try {
            if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(activity.packageName).installingPackageName
            else pm.getInstallerPackageName(activity.packageName)
        } catch (_: Exception) { return }
        val key = "$repository:$installer"
        val now = System.currentTimeMillis()
        val last = prefs.getLong("$key:last", 0L)
        if (!manual && now >= last && now - last < interval) return
        if (installer == "com.android.vending" && playSource == null) return
        if (installer != "com.android.vending" && installer !in sideload) return
        prefs.edit().putLong("$key:last", now).apply()
        fun present(offer: AzUpdateOffer?, play: Boolean) {
            if (offer == null) return
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                if (!manual && prefs.getString("$key:dismissed", null) == offer.version) return@runOnUiThread
                AlertDialog.Builder(activity)
                    .setTitle("Update available")
                    .setMessage(listOf(offer.version, offer.notes.take(4000)).filter { it.isNotBlank() }.joinToString("\n\n"))
                    .setPositiveButton("Update") { _, _ ->
                        val uri = if (play) "https://play.google.com/store/apps/details?id=${activity.packageName}"
                            else "https://github.com/$repository/releases/latest"
                        try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
                        catch (_: android.content.ActivityNotFoundException) { }
                    }
                    .setNegativeButton("Later", null)
                    .setNeutralButton("Skip version") { _, _ ->
                        prefs.edit().putString("$key:dismissed", offer.version).apply()
                    }.show()
            }
        }
        if (installer == "com.android.vending") {
            playSource?.check(activity) { present(it, true) }
            return
        }
        val packageName = activity.packageName
        executor.execute {
            val offer = try {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(packageName, 0)
                @Suppress("DEPRECATION")
                val installedCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
                val connection = URL("https://api.github.com/repos/$repository/releases/latest").openConnection() as HttpURLConnection
                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    if (connection.responseCode != 200) null else {
                        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        val tag = json.optString("tag_name")
                        val body = json.optString("body")
                        if (json.optBoolean("draft") || json.optBoolean("prerelease") ||
                            !ReleaseVersion.newer(tag, body, info.versionName, installedCode)) null
                        else AzUpdateOffer(tag, body)
                    }
                } finally { connection.disconnect() }
            } catch (_: Exception) { null }
            present(offer, false)
        }
    }
}
