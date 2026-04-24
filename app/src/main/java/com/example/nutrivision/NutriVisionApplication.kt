package com.example.nutrivision

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

class NutriVisionApplication : Application() {

    companion object {
        private const val TAG = "NutriVisionApp"
        lateinit var instance: NutriVisionApplication
            private set
    }

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    Log.d(TAG, "📅 Cambio de fecha/hora detectado")
                    // Limpiar toda la caché cuando cambia la fecha
                    context?.let {
                        DataCacheManager.clearCache(it)
                        Log.d(TAG, "✅ Caché limpiada por cambio de fecha")
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(dateChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(dateChangeReceiver, filter)
            }
            Log.d(TAG, "✅ BroadcastReceiver registrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar receiver", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            unregisterReceiver(dateChangeReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar receiver", e)
        }
    }
}