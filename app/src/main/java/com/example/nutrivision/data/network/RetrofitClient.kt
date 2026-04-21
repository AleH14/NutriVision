package com.example.nutrivision.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Al usar un teléfono físico, usamos la IP de tu PC en la red local
    private const val BASE_URL = "http://192.168.1.14:4000/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)      // Timeout de conexión
        .readTimeout(120, TimeUnit.SECONDS)        // Timeout de lectura (OpenAI tarda ~60s)
        .writeTimeout(120, TimeUnit.SECONDS)       // Timeout de escritura
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()

        retrofit.create(ApiService::class.java)
    }
}
