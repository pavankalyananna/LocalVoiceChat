package com.example.localvoicechat

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

object HotspotUtils {

    fun enableHotspot(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0+ we need to use Settings panel
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } else {
            // For older versions, use reflection (requires root or special permissions)
            try {
                val method = wifiManager.javaClass.getMethod(
                    "setWifiApEnabled",
                    android.net.wifi.WifiConfiguration::class.java,
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(wifiManager, null, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}