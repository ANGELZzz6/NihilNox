package com.example.colorblend

import android.app.Application
import android.util.Log

class ColorBlendApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("ColorBlendApp", "Application initialized")
    }
}
