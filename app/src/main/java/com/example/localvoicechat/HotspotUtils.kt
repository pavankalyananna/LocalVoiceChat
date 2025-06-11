package com.example.localvoicechat

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter

object HotspotUtils {

    fun getHotspotIpAddress(context: Context): String {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val dhcpInfo = wifiManager.dhcpInfo
                Formatter.formatIpAddress(dhcpInfo.gateway)
            } else {
                // For simplicity, return default hotspot IP
                "192.168.43.1"
            }
        } catch (ex: Exception) {
            "192.168.43.1" // Fallback IP
        }
    }
}