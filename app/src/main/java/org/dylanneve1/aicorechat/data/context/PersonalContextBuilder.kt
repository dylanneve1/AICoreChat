package org.dylanneve1.aicorechat.data.context

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PersonalContextBuilder gathers on-device context (time, device, locale,
 * battery, storage, network summary, and coarse location if permitted).
 */
class PersonalContextBuilder(private val app: Application) {
    private fun getBatteryPercent(): Int? {
        val bm = app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 1..100) level else null
    }

    private fun getNetworkSummary(): String {
        return try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "Offline/unknown"
            val caps = cm.getNetworkCapabilities(network)
            val parts = mutableListOf<String>()
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) parts.add("Wi‑Fi")
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) parts.add("Cellular")
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) parts.add("Ethernet")
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true) parts.add("Bluetooth")
            if (parts.isEmpty()) "Online" else parts.joinToString(", ")
        } catch (e: Exception) {
            "Unknown"
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getLastKnownLocation(): Location? {
        return try {
            val fused = LocationServices.getFusedLocationProviderClient(app)
            val hasFine = ContextCompat.checkSelfPermission(app, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(app, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) return null
            suspendCancellableCoroutine { cont ->
                fused.lastLocation.addOnSuccessListener { cont.resume(it, onCancellation = null) }
                    .addOnFailureListener { cont.resume(null, onCancellation = null) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatLatLon(loc: Location?): String {
        return if (loc == null) "(not granted)" else "${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}"
    }

    suspend fun build(userName: String): String {
        val now = SimpleDateFormat("EEE, d MMM yyyy HH:mm z", Locale.getDefault()).format(Date())
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        val locale = Locale.getDefault().toString()
        val timeZone = java.util.TimeZone.getDefault().id
        val namePart = if (userName.isNotBlank()) "User Name: ${userName}\n" else ""
        val battery = getBatteryPercent()?.let { "Battery: $it%\n" } ?: ""
        val storageStat = try {
            val stat = android.os.StatFs(Environment.getDataDirectory().path)
            val free = stat.availableBytes / (1024 * 1024)
            val total = stat.totalBytes / (1024 * 1024)
            "Storage: ${free}MB free / ${total}MB total\n"
        } catch (e: Exception) {
            ""
        }
        val appVersion = try {
            val pm = app.packageManager
            val pInfo = pm.getPackageInfo(app.packageName, 0)
            "App Version: ${pInfo.versionName} (${pInfo.longVersionCode})\n"
        } catch (e: Exception) {
            ""
        }
        val network = "Network: ${getNetworkSummary()}\n"
        val location = "Location: ${formatLatLon(getLastKnownLocation())}\n"
        return "[PERSONAL_CONTEXT]\n${namePart}Current Time: ${now}\nDevice: ${device}\nLocale: ${locale}\nTime Zone: ${timeZone}\n${battery}${network}${storageStat}${appVersion}$location[/PERSONAL_CONTEXT]\n\n"
    }
} 
