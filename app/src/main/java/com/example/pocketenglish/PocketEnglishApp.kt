package com.example.pocketenglish

import android.app.Application

class PocketEnglishApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
