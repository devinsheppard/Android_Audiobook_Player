package com.dkshe.audiobookplayer

import android.app.Application
import com.dkshe.audiobookplayer.app.AppContainer

class AudiobookApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

