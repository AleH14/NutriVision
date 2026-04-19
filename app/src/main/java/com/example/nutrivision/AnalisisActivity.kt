package com.example.nutrivision

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.model.FoodAnalysis
import com.example.nutrivision.data.model.FoodAnalysisResponse
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AnalisisActivity : AppCompatActivity() {

    private val TAG = "AnalisisActivity"
    private lateinit var repository: NutriRepository
    
    // UI Elements
    private var ivFoto: ImageView? = null
    private var tvTituloPrincipal: TextView? = null
    private var tvAnalisisPlato: TextView? = null
    private var tvCaloriasInfo: TextView? = null
    private var tvProteinaInfo: TextView? = null
    private var tvCarbsInfo: TextView? = null
    private var tvGrasasInfo: TextView? = null
    private var btnGuardarAnalisis: MaterialButton? = null
    private var progressBarLoading: ProgressBar? = null
    private var layoutResultados: LinearLayout? = null
    
    // Data
    private lateinit var imageFile: File
    private var currentAnalysisResult: FoodAnalysisResponse? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analisis)
        
        repository = NutriRepository(RetrofitClient.instance)
        
        initViews()
        obtenerDatos()
        setupNavigation()
        
        // Mostrar foto inmediatamente
        cargarFoto()
        
        // Iniciar análisis en segundo plano
        realizarAnalisis()
    }
    
    private fun initViews() {
        ivFoto = findViewById(R.id.ivFoodImage)
        tvTituloPrincipal = findViewById(R.id.tvFoodName)
        tvAnalisisPlato = findViewById(R.id.tvFoodDescription)
        tvCaloriasInfo = findViewById(R.id.tvCalories)
        tvProteinaInfo = findViewById(R.id.tvProteins)
        tvCarbsInfo = findViewById(R.id.tvCarbs)
        tvGrasasInfo = findViewById(R.id.tvFats)
        btnGuardarAnalisis = findViewById(R.id.btnGuardar)
        progressBarLoading = findViewById(R.id.progressBarAnalisis)
        layoutResultados = findViewById(R.id.layoutResultados)
        
        btnGuardarAnalisis?.setOnClickListener { guardarAnalisis() }
        
        findViewById<TextView>(R.id.btnBackAnalisis)?.setOnClickListener { cancelar() }
    }
    
    private fun obtenerDatos() {
        val imagePath = intent.getStringExtra("imageFile") ?: ""
        imageFile = File(imagePath)
        
        Log.d(TAG, "Archivo recibido: $imagePath")
    }
    
    private fun cargarFoto() {
        if (imageFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ivFoto?.setImageBitmap(bitmap)
            Log.d(TAG, "Foto cargada")
        } else {
            Log.e(TAG, "Archivo de foto no existe")
            Toast.makeText(this, "Error: no se pudo cargar la foto", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun realizarAnalisis() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            mostrarError("Token no encontrado")
            return
        }
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Iniciando análisis en segundo plano...")
                progressBarLoading?.visibility = View.VISIBLE
                layoutResultados?.visibility = View.GONE
                
                // Crear multipart
                val fileBytes = imageFile.readBytes()
                val requestFile = fileBytes.toRequestBody("image/jpeg".toMediaType())
                val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                
                Log.d(TAG, "Enviando imagen a OpenAI (${fileBytes.size} bytes)...")
                val response = repository.analyzeFoodImage(token, body)
                
                Log.d(TAG, "Respuesta recibida: código=${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Análisis exitoso ✓")
                    val analysisResult = response.body()!!
                    mostrarResultados(analysisResult)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    Log.e(TAG, "Error: ${response.code()}")
                    Log.e(TAG, "Body: $errorBody")
                    mostrarError("Error al analizar: ${response.code()}")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Excepción", error)
                error.printStackTrace()
                mostrarError("Error: ${error.message}")
            } finally {
                progressBarLoading?.visibility = View.GONE
            }
        }
    }
    
    private fun mostrarResultados(analysisResult: FoodAnalysisResponse) {
        // Guardar para uso posterior (al guardar)
        currentAnalysisResult = analysisResult
        
        val analysis = analysisResult.analysis
        
        // Títulos de platos
        val titleText = analysis.dishes.joinToString(" + ") { it.name }
        tvTituloPrincipal?.text = titleText
        Log.d(TAG, "Título: $titleText")
        
        // Análisis del plato
        tvAnalisisPlato?.text = analysis.plateAnalysis
        
        // Información nutricional
        tvCaloriasInfo?.text = "${analysis.nutrition.calories} kcal"
        tvProteinaInfo?.text = "${analysis.nutrition.proteinGrams.toInt()} g"
        tvCarbsInfo?.text = "${analysis.nutrition.carbsGrams.toInt()} g"
        tvGrasasInfo?.text = "${analysis.nutrition.fatGrams.toInt()} g"
        
        // Mostrar resultados
        layoutResultados?.visibility = View.VISIBLE
        
        Log.d(TAG, "Resultados mostrados: ${analysis.nutrition.calories} kcal")
    }
    
    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        tvTituloPrincipal?.text = "Error al analizar"
        tvAnalisisPlato?.text = mensaje
        layoutResultados?.visibility = View.VISIBLE
        btnGuardarAnalisis?.isEnabled = false
    }
    
    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> {
                    cancelar()
                    true
                }
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
    
    private fun guardarAnalisis() {
        if (currentAnalysisResult == null) {
            Toast.makeText(
                this,
                "Error: No hay análisis para guardar",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        val token = TokenManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, "Token no encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        
        btnGuardarAnalisis?.isEnabled = false
        btnGuardarAnalisis?.text = "Guardando..."
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Guardando análisis...")
                
                val analysis = currentAnalysisResult!!.analysis
                val imageFilename = currentAnalysisResult!!.imageData.filename
                
                val response = repository.saveAnalysis(
                    token = token,
                    imageFilename = imageFilename,
                    dishes = analysis.dishes,
                    nutrition = analysis.nutrition,
                    plateAnalysis = analysis.plateAnalysis,
                    mealType = analysis.mealType
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Análisis guardado exitosamente ✓")
                    Toast.makeText(
                        this@AnalisisActivity,
                        "✓ ${response.body()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Esperar un poco antes de regresar
                    kotlinx.coroutines.delay(1000)
                    
                    // Ir a Inicio
                    startActivity(Intent(this@AnalisisActivity, InicioActivity::class.java))
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    Log.e(TAG, "Error: ${response.code()}")
                    Toast.makeText(
                        this@AnalisisActivity,
                        "Error al guardar: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (error: Exception) {
                Log.e(TAG, "Error al guardar", error)
                error.printStackTrace()
                Toast.makeText(
                    this@AnalisisActivity,
                    "Error al guardar: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                btnGuardarAnalisis?.isEnabled = true
                btnGuardarAnalisis?.text = "Guardar"
            }
        }
    }
    
    private fun cancelar() {
        // Eliminar imagen temporal
        if (imageFile.exists()) {
            imageFile.delete()
        }
        startActivity(Intent(this, InicioActivity::class.java))
        finish()
    }
}