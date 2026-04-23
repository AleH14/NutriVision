package com.example.nutrivision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.model.User
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class InicioActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "InicioActivity"
        private const val CACHE_KEY = "user_profile_cache"
        private const val FECHA_CACHE_KEY = "ultima_fecha_registrada"
    }

    private var tvSaludo: TextView? = null
    private var tvMetaDiaria: TextView? = null
    private var tvProteinasHoy: TextView? = null
    private var tvCarbsHoy: TextView? = null
    private var tvGrasasHoy: TextView? = null
    private var tvCaloriasConsumidas: TextView? = null
    private var tvCaloriasMeta: TextView? = null
    private var tvPorcentaje: TextView? = null
    private var tvRestante: TextView? = null
    private var progressCalorias: ProgressBar? = null
    private var progressProteinas: ProgressBar? = null
    private var progressCarbos: ProgressBar? = null
    private var progressGrasas: ProgressBar? = null

    private lateinit var repository: NutriRepository

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fechaCheckRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)
        repository = NutriRepository(RetrofitClient.instance)

        initViews()
        setupNavigation()
        setupCameraButton()

        iniciarVerificacionFecha()
        cargarDatosInicio()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Verificando cambio de fecha...")
        verificarYReiniciarSiFechaCambio()
        cargarDatosInicio(forceRefresh = true)
    }

    override fun onPause() {
        super.onPause()
        guardarFechaActualEnPrefs()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(fechaCheckRunnable)
    }

    private fun iniciarVerificacionFecha() {
        fechaCheckRunnable = object : Runnable {
            override fun run() {
                verificarYReiniciarSiFechaCambio()
                handler.postDelayed(this, 60000) // Cada minuto
            }
        }
        handler.postDelayed(fechaCheckRunnable, 1000)
    }

    private fun obtenerFechaActualLocal(): String {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getDefault() }
        return sdf.format(now.time)
    }

    private fun obtenerUltimaFechaRegistrada(): String {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getString(FECHA_CACHE_KEY, "") ?: ""
    }

    private fun guardarFechaActualEnPrefs() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putString(FECHA_CACHE_KEY, obtenerFechaActualLocal()).apply()
    }

    private fun verificarYReiniciarSiFechaCambio() {
        val fechaActual = obtenerFechaActualLocal()
        val ultimaFecha = obtenerUltimaFechaRegistrada()

        if (ultimaFecha.isNotEmpty() && ultimaFecha != fechaActual) {
            Log.d(TAG, "🔥 ¡FECHA CAMBIADA! $ultimaFecha -> $fechaActual")
            guardarFechaActualEnPrefs()

            // ✅ FORZAR REINICIO EN EL BACKEND
            lifecycleScope.launch {
                try {
                    val token = TokenManager.getToken(this@InicioActivity)
                    if (token != null) {
                        // Llamar a un endpoint específico para reiniciar
                        val resetResponse = repository.resetDailySummary(token)
                        if (resetResponse.isSuccessful) {
                            Log.d(TAG, "✅ Resumen reiniciado en backend")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al reiniciar resumen", e)
                }

                delay(100)
                cargarDatosInicio(forceRefresh = true)
            }
        } else if (ultimaFecha.isEmpty()) {
            guardarFechaActualEnPrefs()
        }
    }

    private fun initViews() {
        tvSaludo = findViewById(R.id.tvSaludo)
        tvMetaDiaria = findViewById(R.id.tvMetaDiaria)
        tvProteinasHoy = findViewById(R.id.tvProteinasHoy)
        tvCarbsHoy = findViewById(R.id.tvCarbsHoy)
        tvGrasasHoy = findViewById(R.id.tvGrasasHoy)
        tvCaloriasConsumidas = findViewById(R.id.tv_calorias_consumidas)
        tvCaloriasMeta = findViewById(R.id.tv_calorias_meta)
        tvPorcentaje = findViewById(R.id.tv_porcentaje)
        tvRestante = findViewById(R.id.tv_restante)
        progressCalorias = findViewById(R.id.progress_calorias)
        progressProteinas = findViewById(R.id.progress_proteinas)
        progressCarbos = findViewById(R.id.progress_carbos)
        progressGrasas = findViewById(R.id.progress_grasas)
    }

    private fun cargarDatosInicio(forceRefresh: Boolean = false) {
        val token = TokenManager.getToken(this) ?: return irALogin()
        val nombreUsuario = TokenManager.getUserName(this)
        tvSaludo?.text = "¡Hola, $nombreUsuario!"

        lifecycleScope.launch {
            try {
                val fechaActual = obtenerFechaActualLocal()

                Log.d(TAG, "Solicitando perfil para fecha: $fechaActual")

                if (forceRefresh) {
                    DataCacheManager.clearCache(this@InicioActivity)
                }

                val response = repository.getProfile(token, fechaActual)

                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!
                    // El backend ya reinició si era necesario
                    actualizarInterfaz(usuario)
                    DataCacheManager.saveCache(this@InicioActivity, CACHE_KEY, usuario)
                } else {
                    Log.e(TAG, "Error en respuesta: ${response.code()}")
                    val cachedUser = DataCacheManager.getCache(this@InicioActivity, CACHE_KEY, User::class.java)
                    cachedUser?.let { mostrarValoresEnCeroConMetas(it) }
                }
            } catch (error: Exception) {
                if (error !is kotlinx.coroutines.CancellationException) {
                    Log.e(TAG, "Error de red", error)
                    val cachedUser = DataCacheManager.getCache(this@InicioActivity, CACHE_KEY, User::class.java)
                    cachedUser?.let { mostrarValoresEnCeroConMetas(it) }
                }
            }
        }
    }

    private fun mostrarValoresEnCeroConMetas(usuario: User) {
        runOnUiThread {
            val metaCalorias = usuario.dailyCalorieGoalKcal.toInt()
            val metaProteinas = usuario.dailyProteinGoalGrams
            val metaCarbos = usuario.dailyCarbsGoalGrams
            val metaGrasas = usuario.dailyFatGoalGrams

            tvMetaDiaria?.text = "Meta Diaria: $metaCalorias kcal"
            tvCaloriasConsumidas?.text = "0 /"
            tvCaloriasMeta?.text = " $metaCalorias kcal"
            tvPorcentaje?.text = "0%"
            tvRestante?.text = "Te quedan $metaCalorias kcal para completar el día"
            progressCalorias?.progress = 0

            tvProteinasHoy?.text = "0 / $metaProteinas g"
            tvCarbsHoy?.text = "0 / $metaCarbos g"
            tvGrasasHoy?.text = "0 / $metaGrasas g"

            progressProteinas?.max = metaProteinas
            progressProteinas?.progress = 0
            progressCarbos?.max = metaCarbos
            progressCarbos?.progress = 0
            progressGrasas?.max = metaGrasas
            progressGrasas?.progress = 0
        }
    }

    private fun actualizarInterfaz(usuario: User) {
        val metaCaloriasDiarias = usuario.dailyCalorieGoalKcal.toInt()
        tvMetaDiaria?.text = "Meta Diaria: $metaCaloriasDiarias kcal"

        val resumen = usuario.todayNutritionSummary
        if (resumen != null && resumen.date == obtenerFechaActualLocal()) {
            val p = resumen.proteinGramsConsumed
            val c = resumen.carbsGramsConsumed
            val g = resumen.fatGramsConsumed

            // ✅ USAR LAS CALORÍAS GUARDADAS EN BD (NO RECALCULAR)
            val caloriasConsumidas = if (resumen.caloriesConsumed > 0) {
                resumen.caloriesConsumed.toInt()
            } else {
                // Fallback: calcular si no existe (para datos antiguos)
                (p * 4 + c * 4 + g * 9).toInt()
            }

            val porcentaje = if (metaCaloriasDiarias > 0) ((caloriasConsumidas.toFloat() / metaCaloriasDiarias) * 100).toInt() else 0
            val caloriasRestantes = (metaCaloriasDiarias - caloriasConsumidas).coerceAtLeast(0)

            Log.d(TAG, "📊 Calorías en BD: $caloriasConsumidas, Meta: $metaCaloriasDiarias")

            tvCaloriasConsumidas?.text = "$caloriasConsumidas /"
            tvCaloriasMeta?.text = " $metaCaloriasDiarias kcal"
            tvPorcentaje?.text = "$porcentaje%"
            tvRestante?.text = "Te quedan $caloriasRestantes kcal para completar el día"
            progressCalorias?.progress = porcentaje.coerceAtMost(100)

            tvProteinasHoy?.text = "${p.toInt()} / ${usuario.dailyProteinGoalGrams} g"
            tvCarbsHoy?.text = "${c.toInt()} / ${usuario.dailyCarbsGoalGrams} g"
            tvGrasasHoy?.text = "${g.toInt()} / ${usuario.dailyFatGoalGrams} g"

            progressProteinas?.max = usuario.dailyProteinGoalGrams
            progressProteinas?.progress = p.toInt()
            progressCarbos?.max = usuario.dailyCarbsGoalGrams
            progressCarbos?.progress = c.toInt()
            progressGrasas?.max = usuario.dailyFatGoalGrams
            progressGrasas?.progress = g.toInt()
        } else {
            mostrarValoresEnCeroConMetas(usuario)
        }
    }
    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_historial -> {
                    startActivity(Intent(this, HistorialActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupCameraButton() {
        findViewById<CardView>(R.id.btn_camara).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(this, CamaraActivity::class.java))
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            }
        }
    }

    private fun irALogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}