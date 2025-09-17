package org.dylanneve1.aicorechat.util

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class DeviceSupportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun isAICoreInstalled_returnsFalseWhenPackageMissing() {
        assertFalse(isAICoreInstalled(context))
    }

    @Test
    fun isAICoreInstalled_returnsTrueWhenPackageInstalled() {
        val packageInfo = PackageInfo()
        packageInfo.packageName = "com.google.android.aicore"
        shadowOf(context.packageManager).installPackage(packageInfo)

        assertTrue(isAICoreInstalled(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun checkDeviceSupport_requiresAndroid14() = runBlocking {
        val status = checkDeviceSupport(context)

        assertTrue(status is DeviceSupportStatus.NotReady)
        assertEquals("Android 14 or newer is required", (status as DeviceSupportStatus.NotReady).reason)
    }

    @Test
    fun checkDeviceSupport_returnsMissingWhenAICoreNotInstalled() = runBlocking {
        val status = checkDeviceSupport(context)

        assertTrue(status is DeviceSupportStatus.AICoreMissing)
    }
}
