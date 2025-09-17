package org.dylanneve1.aicorechat.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class DeviceSupportStatus {
    data object Supported : DeviceSupportStatus()
    data object AICoreMissing : DeviceSupportStatus()
    data class NotReady(val reason: String?) : DeviceSupportStatus()
}

fun isAICoreInstalled(context: Context): Boolean {
    return try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo("com.google.android.aicore", PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo("com.google.android.aicore", 0)
        }
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}

suspend fun checkDeviceSupport(context: Context): DeviceSupportStatus = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return@withContext DeviceSupportStatus.NotReady("Android 14 or newer is required")
    }

    if (!isAICoreInstalled(context)) {
        return@withContext DeviceSupportStatus.AICoreMissing
    }

    var model: GenerativeModel? = null
    try {
        val cfg = generationConfig {
            this.context = context.applicationContext
            temperature = 0.1f
            topK = 1
        }
        model = GenerativeModel(generationConfig = cfg)
        model.prepareInferenceEngine()
        DeviceSupportStatus.Supported
    } catch (e: Exception) {
        DeviceSupportStatus.NotReady(e.message)
    } finally {
        try {
            model?.close()
        } catch (_: Exception) {
        }
    }
} 
