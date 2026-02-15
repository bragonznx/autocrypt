package com.autocrypt

import android.os.Build

object DeviceUtils {

    /**
     * Check if the device is manufactured by Samsung
     */
    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("Samsung", ignoreCase = true) ||
               Build.BRAND.equals("Samsung", ignoreCase = true)
    }

    /**
     * Check if Samsung Knox is present on the device
     */
    fun hasKnoxSecurity(): Boolean {
        if (!isSamsungDevice()) {
            return false
        }

        return try {
            // Try to detect Knox by checking for Knox-related system properties
            val knoxVersion = getSystemProperty("ro.build.characteristics")
            knoxVersion?.contains("knox", ignoreCase = true) == true ||
            // Check for Knox container
            hasKnoxClass()
        } catch (e: Exception) {
            // If we can't determine, assume Samsung devices have Knox
            isSamsungDevice()
        }
    }

    /**
     * Check if Knox classes are available
     */
    private fun hasKnoxClass(): Boolean {
        return try {
            Class.forName("com.samsung.android.knox.SemPersonaManager")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Get system property value
     */
    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get device information for debugging
     */
    fun getDeviceInfo(): String {
        return """
            Manufacturer: ${Build.MANUFACTURER}
            Brand: ${Build.BRAND}
            Model: ${Build.MODEL}
            Device: ${Build.DEVICE}
            Android Version: ${Build.VERSION.RELEASE}
            SDK: ${Build.VERSION.SDK_INT}
            Samsung Device: ${isSamsungDevice()}
            Knox Detected: ${hasKnoxSecurity()}
        """.trimIndent()
    }
}
