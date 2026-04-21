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
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import kotlin.math.round

class InicioActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_CODE = 100
    private val TAG = "InicioActivity"
    
    // Views (nullable para flexibilidad)
    private var tvSaludo: TextView? = null
    private var tvMetaDiaria: TextView? = null
    private var tvProteinasHoy: TextView? = null
    private var tvCarbsHoy: TextView? = null
    private var tvGrasasHoy: TextView? = null
    private var tvCaloriasHoy: TextView? = null
    
    // Views de calorías
    private var tvCaloriasConsumidas: TextView? = null
    private var tvCaloriasMeta: TextView? = null
    private var tvPorcentaje: TextView? = null
    private var tvRestante: TextView? = null
    private var progressCalorias: ProgressBar? = null
    
    // ProgressBars de macronutrientes
    private var progressProteinas: ProgressBar? = null
    private var progressCarbos: ProgressBar? = null
    private var progressGrasas: ProgressBar? = null
    
    private lateinit var repository: NutriRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_inicio)

        Log.d(TAG, "onCreate: Activity iniciada")

        repository = NutriRepository(RetrofitClient.instance)
        
        initViews()
        setupNavigation()
        setupCameraButton()
        cargarDatosInicio()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Recargando datos...")
        // Solo recargar si la actividad está activa
        if (!isFinishing && !isDestroyed) {
            cargarDatosInicio()
        }
    }
    
    private fun initViews() {
        Log.d(TAG, "initViews: Buscando vistas...")
        // TextViews básicos
        tvSaludo = findViewById(R.id.tvSaludo)
        tvMetaDiaria = findViewById(R.id.tvMetaDiaria)
        tvProteinasHoy = findViewById(R.id.tvProteinasHoy)
        tvCarbsHoy = findViewById(R.id.tvCarbsHoy)
        tvGrasasHoy = findViewById(R.id.tvGrasasHoy)
        tvCaloriasHoy = findViewById(R.id.tvCaloriasHoy)
        
        // TextViews de calorías
        tvCaloriasConsumidas = findViewById(R.id.tv_calorias_consumidas)
        tvCaloriasMeta = findViewById(R.id.tv_calorias_meta)
        tvPorcentaje = findViewById(R.id.tv_porcentaje)
        tvRestante = findViewById(R.id.tv_restante)
        
        // ProgressBars
        progressCalorias = findViewById(R.id.progress_calorias)
        progressProteinas = findViewById(R.id.progress_proteinas)
        progressCarbos = findViewById(R.id.progress_carbos)
        progressGrasas = findViewById(R.id.progress_grasas)
        
        Log.d(TAG, "initViews: tvSaludo=${tvSaludo != null}, progressCalorias=${progressCalorias != null}")
    }
    
    private fun cargarDatosInicio() {
        Log.d(TAG, "cargarDatosInicio: Iniciando carga de datos")
        
        val token = TokenManager.getToken(this)
        val nombreUsuario = TokenManager.getUserName(this)
        
        Log.d(TAG, "Token obtenido: ${token != null}")
        Log.d(TAG, "Nombre usuario: $nombreUsuario")
        
        if (token == null) {
            Log.e(TAG, "Token es null, redirigiendo a login")
            irALogin()
            return
        }

        // Mostrar saludo inicial
        tvSaludo?.text = "¡Hola, $nombreUsuario!"

        lifecycleScope.launch {
            try {
                val response = repository.getProfile(token)
                
                Log.d(TAG, "Respuesta recibida: isSuccessful=${response.isSuccessful}, code=${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!
                    Log.d(TAG, "dailyCalorieGoalKcal: ${usuario.dailyCalorieGoalKcal}")
                    
                    // Metas diarias desde el perfil del usuario (IA o calculadas)
                    val metaCaloriasDiarias = usuario.dailyCalorieGoalKcal.toInt()
                    val proteinasMax = usuario.dailyProteinGoalGrams
                    val carbosMax = usuario.dailyCarbsGoalGrams
                    val grasasMax = usuario.dailyFatGoalGrams

                    tvMetaDiaria?.text = "Meta Diaria: $metaCaloriasDiarias kcal"
                    Log.d(TAG, "Metas actualizadas: $metaCaloriasDiarias kcal, P: $proteinasMax, C: $carbosMax, G: $grasasMax")
                    
                    // Resumen nutricional de hoy
                    val resumen = usuario.todayNutritionSummary
                    if (resumen != null) {
                        // Calorías consumidas (proteína + carbos + grasas)
                        val caloriasConsumidas = (resumen.proteinGramsConsumed * 4 + 
                                                  resumen.carbsGramsConsumed * 4 + 
                                                  resumen.fatGramsConsumed * 9).toInt()
                        
                        // Calcular porcentaje
                        val porcentaje = if (metaCaloriasDiarias > 0) {
                            ((caloriasConsumidas.toFloat() / metaCaloriasDiarias) * 100).toInt()
                        } else {
                            0
                        }
                        
                        // Calorías restantes
                        val caloriasRestantes = (metaCaloriasDiarias - caloriasConsumidas).coerceAtLeast(0)
                        
                        Log.d(TAG, "Calorías: $caloriasConsumidas / $metaCaloriasDiarias kcal ($porcentaje%)")
                        
                        // Actualizar TextViews de calorías
                        tvCaloriasConsumidas?.text = "$caloriasConsumidas /"
                        tvCaloriasMeta?.text = " $metaCaloriasDiarias kcal"
                        tvPorcentaje?.text = "$porcentaje%"
                        tvRestante?.text = "Te quedan $caloriasRestantes kcal para completar el día"
                        
                        // Actualizar ProgressBar de calorías (max 100%)
                        progressCalorias?.progress = porcentaje.coerceAtMost(100)
                        
                        // Actualizar macronutrientes con valores del usuario
                        tvProteinasHoy?.text = "${resumen.proteinGramsConsumed.toInt()} / $proteinasMax g"
                        tvCarbsHoy?.text = "${resumen.carbsGramsConsumed.toInt()} / $carbosMax g"
                        tvGrasasHoy?.text = "${resumen.fatGramsConsumed.toInt()} / $grasasMax g"
                        
                        progressProteinas?.max = proteinasMax
                        progressProteinas?.progress = resumen.proteinGramsConsumed.toInt().coerceAtMost(proteinasMax)
                        
                        progressCarbos?.max = carbosMax
                        progressCarbos?.progress = resumen.carbsGramsConsumed.toInt().coerceAtMost(carbosMax)
                        
                        progressGrasas?.max = grasasMax
                        progressGrasas?.progress = resumen.fatGramsConsumed.toInt().coerceAtMost(grasasMax)
                        
                        Log.d(TAG, "Macronutrientes actualizados:")
                        Log.d(TAG, "  Proteína: ${resumen.proteinGramsConsumed.toInt()} / $proteinasMax g")
                        Log.d(TAG, "  Carbos: ${resumen.carbsGramsConsumed.toInt()} / $carbosMax g")
                        Log.d(TAG, "  Grasas: ${resumen.fatGramsConsumed.toInt()} / $grasasMax g")
                        
                    } else {
                        // Si no hay resumen aún, mostrar 0 con metas del usuario
                        tvCaloriasConsumidas?.text = "0 /"
                        tvCaloriasMeta?.text = " $metaCaloriasDiarias kcal"
                        tvPorcentaje?.text = "0%"
                        tvRestante?.text = "Te quedan $metaCaloriasDiarias kcal para completar el día"
                        
                        progressCalorias?.progress = 0
                        
                        tvProteinasHoy?.text = "0 / $proteinasMax g"
                        tvCarbsHoy?.text = "0 / $carbosMax g"
                        tvGrasasHoy?.text = "0 / $grasasMax g"
                        
                        progressProteinas?.max = proteinasMax
                        progressProteinas?.progress = 0
                        progressCarbos?.max = carbosMax
                        progressCarbos?.progress = 0
                        progressGrasas?.max = grasasMax
                        progressGrasas?.progress = 0
                    }
                    
                } else {
                    val errorMsg = "Error al cargar datos: ${response.code()}"
                    Log.e(TAG, errorMsg)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Excepción durante llamada a API", error)
            }
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
        val btnCamara = findViewById<CardView>(R.id.btn_camara)
        btnCamara.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            abrirCamara()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun abrirCamara() {
        val intent = Intent(this, CamaraActivity::class.java)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara()
            }
        }
    }
    
    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

}
