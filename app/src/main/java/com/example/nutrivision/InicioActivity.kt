package com.example.nutrivision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.model.User
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class InicioActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "InicioActivity"
        private const val CACHE_KEY = "user_profile_cache"
    }
    
    private var tvSaludo: TextView? = null
    private var tvMetaDiaria: TextView? = null
    private var tvProteinasHoy: TextView? = null
    private var tvCarbsHoy: TextView? = null
    private var tvGrasasHoy: TextView? = null
    private var tvCaloriasHoy: TextView? = null
    private var tvCaloriasConsumidas: TextView? = null
    private var tvCaloriasMeta: TextView? = null
    private var tvPorcentaje: TextView? = null
    private var tvRestante: TextView? = null
    private var progressCalorias: ProgressBar? = null
    private var progressProteinas: ProgressBar? = null
    private var progressCarbos: ProgressBar? = null
    private var progressGrasas: ProgressBar? = null
    
    private lateinit var repository: NutriRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)
        repository = NutriRepository(RetrofitClient.instance)
        
        initViews()
        setupNavigation()
        setupCameraButton()
        
        cargarDesdeCache()
        cargarDatosInicio()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refrescando datos...")
        cargarDatosInicio()
    }
    
    private fun initViews() {
        tvSaludo = findViewById(R.id.tvSaludo)
        tvMetaDiaria = findViewById(R.id.tvMetaDiaria)
        tvProteinasHoy = findViewById(R.id.tvProteinasHoy)
        tvCarbsHoy = findViewById(R.id.tvCarbsHoy)
        tvGrasasHoy = findViewById(R.id.tvGrasasHoy)
        tvCaloriasHoy = findViewById(R.id.tvCaloriasHoy)
        tvCaloriasConsumidas = findViewById(R.id.tv_calorias_consumidas)
        tvCaloriasMeta = findViewById(R.id.tv_calorias_meta)
        tvPorcentaje = findViewById(R.id.tv_porcentaje)
        tvRestante = findViewById(R.id.tv_restante)
        progressCalorias = findViewById(R.id.progress_calorias)
        progressProteinas = findViewById(R.id.progress_proteinas)
        progressCarbos = findViewById(R.id.progress_carbos)
        progressGrasas = findViewById(R.id.progress_grasas)
    }

    private fun cargarDesdeCache() {
        val cachedUser = DataCacheManager.getCache(this, CACHE_KEY, User::class.java)
        cachedUser?.let {
            Log.d(TAG, "Caché encontrada, renderizando...")
            actualizarInterfaz(it)
        }
    }

    private fun cargarDatosInicio() {
        val token = TokenManager.getToken(this) ?: return irALogin()
        val nombreUsuario = TokenManager.getUserName(this)
        tvSaludo?.text = "¡Hola, $nombreUsuario!"

        lifecycleScope.launch {
            try {
                // Generar fecha local exacta
                val now = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getDefault() }
                val todayStr = sdf.format(now.time)
                
                Log.d(TAG, "Solicitando perfil para fecha: $todayStr")
                val response = repository.getProfile(token, todayStr)

                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!
                    Log.d(TAG, "Datos recibidos. Resumen hoy: ${usuario.todayNutritionSummary}")
                    
                    DataCacheManager.saveCache(this@InicioActivity, CACHE_KEY, usuario)
                    actualizarInterfaz(usuario)
                } else {
                    Log.e(TAG, "Error en respuesta: ${response.code()}")
                }
            } catch (error: Exception) {
                // No mostrar error si es una cancelación por rotación
                if (error !is kotlinx.coroutines.CancellationException) {
                    Log.e(TAG, "Error de red", error)
                }
            }
        }
    }

    private fun actualizarInterfaz(usuario: User) {
        val metaCaloriasDiarias = usuario.dailyCalorieGoalKcal.toInt()
        tvMetaDiaria?.text = "Meta Diaria: $metaCaloriasDiarias kcal"

        val resumen = usuario.todayNutritionSummary
        if (resumen != null) {
            val p = resumen.proteinGramsConsumed
            val c = resumen.carbsGramsConsumed
            val g = resumen.fatGramsConsumed
            
            val caloriasConsumidas = (p * 4 + c * 4 + g * 9).toInt()
            val porcentaje = if (metaCaloriasDiarias > 0) ((caloriasConsumidas.toFloat() / metaCaloriasDiarias) * 100).toInt() else 0
            val caloriasRestantes = (metaCaloriasDiarias - caloriasConsumidas).coerceAtLeast(0)

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
            Log.d(TAG, "No hay resumen para hoy, reseteando a 0")
            resetValores(metaCaloriasDiarias, usuario.dailyProteinGoalGrams, usuario.dailyCarbsGoalGrams, usuario.dailyFatGoalGrams)
        }
    }

    private fun resetValores(kcal: Int, p: Int, c: Int, g: Int) {
        tvCaloriasConsumidas?.text = "0 /"
        tvCaloriasMeta?.text = " $kcal kcal"
        tvPorcentaje?.text = "0%"
        tvRestante?.text = "Te quedan $kcal kcal para completar el día"
        progressCalorias?.progress = 0
        tvProteinasHoy?.text = "0 / $p g"
        tvCarbsHoy?.text = "0 / $c g"
        tvGrasasHoy?.text = "0 / $g g"
        progressProteinas?.progress = 0
        progressCarbos?.progress = 0
        progressGrasas?.progress = 0
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