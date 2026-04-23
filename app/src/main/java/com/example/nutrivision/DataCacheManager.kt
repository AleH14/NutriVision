package com.example.nutrivision

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

object DataCacheManager {
    private const val PREF_NAME = "nutrivision_cache"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Guardar cualquier objeto como JSON
    fun <T> saveCache(context: Context, key: String, data: T) {
        val json = gson.toJson(data)
        getPrefs(context).edit().putString(key, json).apply()
    }

    // Recuperar un objeto desde JSON
    fun <T> getCache(context: Context, key: String, clazz: Class<T>): T? {
        val json = getPrefs(context).getString(key, null) ?: return null
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    // ========== para INICIOKT==========

    // Limpiar una clave específica
    fun clearCacheKey(context: Context, key: String) {
        getPrefs(context).edit().remove(key).apply()
    }

    // Verificar si existe una clave en caché
    fun containsKey(context: Context, key: String): Boolean {
        return getPrefs(context).contains(key)
    }
}