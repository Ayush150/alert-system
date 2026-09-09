package com.sih26001.mobilealert

import android.app.Application
import com.sih26001.mobilealert.di.DependencyContainer

class MobileAlertApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DependencyContainer.initialize(this)
    }
}
