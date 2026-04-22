package com.gptimage2

import android.app.Application
import androidx.room.Room
import com.gptimage2.data.local.GptImageDatabase
import com.gptimage2.util.ApiKeyManager

class GptImage2App : Application() {

    lateinit var database: GptImageDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        ApiKeyManager.init(this)
        database = Room.databaseBuilder(
            applicationContext,
            GptImageDatabase::class.java,
            GptImageDatabase.DATABASE_NAME
        ).build()
    }

    companion object {
        lateinit var instance: GptImage2App
            private set
    }
}
