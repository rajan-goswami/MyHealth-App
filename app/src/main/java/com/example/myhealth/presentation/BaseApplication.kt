package com.example.myhealth.presentation

import android.app.Application
import com.example.myhealth.data.HealthConnectManager

class BaseApplication : Application() {
    val healthConnectManager by lazy {
        HealthConnectManager(this)
    }
}