package com.example.localvoicechat

import android.app.Application
import androidx.multidex.MultiDex

class LocalVoiceChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
    }
}