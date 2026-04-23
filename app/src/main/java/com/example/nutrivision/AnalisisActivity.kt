package com.example.nutrivision

import android.content.Intent
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
import com.example.nutrivision.data.model.FoodAnalysisResponse
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AnalisisActivity : AppCompatActivity() {

    private val TAG = "AnalisisActivity"
    private lateinit var repository: NutriRepository

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

    private lateinit var imageFile: File
    private var currentAnalysisResult: FoodAnalysisResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analisis)
        repository = NutriRepository(RetrofitClient.instance)
        initViews()
        obtenerDatos()
        setupNavigation()
        cargarFoto()
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
        btnGuardarAnalisis?.visibility = View.GONE
    }

    private fun obtenerDatos() {
        val imagePath = intent.getStringExtra("imageFile") ?: ""
        imageFile = File(imagePath)
    }

    private fun cargarFoto() {
        if (imageFile.exists()) {
            ivFoto?.setImageBitmap(ImageUtils.loadRotatedCircleBitmap(imageFile))
        }
    }

    private fun realizarAnalisis() {
        val token = TokenManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                progressBarLoading?.visibility = View.VISIBLE
                
                // OPTIMIZACIÓN: Comprimir imagen antes de subir (Reduce tiempo de red)
                val compressedFile = ImageUtils.compressImageForApi(imageFile)
                
                val body = MultipartBody.Part.createFormData(
                    "image", 
                    compressedFile.name, 
                    compressedFile.readBytes().toRequestBody("image/jpeg".toMediaType())
                )
                
                val response = repository.analyzeFoodImage(token, body)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.isFood && result.analysis != null) {
                        currentAnalysisResult = result
                        mostrarResultados(result)
                        btnGuardarAnalisis?.visibility = View.VISIBLE
                    } else {
                        tvTituloPrincipal?.text = "Imagen no válida"
                        tvAnalisisPlato?.text = result.message ?: "Intenta con otra foto"
                        layoutResultados?.visibility = View.VISIBLE
                    }
                }
                
                // Borrar archivo temporal de compresión
                if (compressedFile.exists()) compressedFile.delete()
                
            } catch (e: Exception) { 
                Log.e(TAG, "Error en análisis", e)
                Toast.makeText(this@AnalisisActivity, "Error al procesar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            finally { progressBarLoading?.visibility = View.GONE }
        }
    }

    private fun mostrarResultados(res: FoodAnalysisResponse) {
        val analysis = res.analysis!!
        tvTituloPrincipal?.text = analysis.dishes.joinToString(" + ") { it.name }
        tvAnalisisPlato?.text = analysis.plateAnalysis
        tvCaloriasInfo?.text = "${analysis.nutrition.calories} kcal"
        tvProteinaInfo?.text = "${analysis.nutrition.proteinGrams.toInt()} g"
        tvCarbsInfo?.text = "${analysis.nutrition.carbsGrams.toInt()} g"
        tvGrasasInfo?.text = "${analysis.nutrition.fatGrams.toInt()} g"
        layoutResultados?.visibility = View.VISIBLE
    }

    private fun guardarAnalisis() {
        val result = currentAnalysisResult?.analysis ?: return
        val token = TokenManager.getToken(this) ?: return
        btnGuardarAnalisis?.isEnabled = false
        btnGuardarAnalisis?.text = "Guardando..."

        lifecycleScope.launch {
            try {
                // Usar hora local del dispositivo (sin convertir a UTC)
                val now = Calendar.getInstance()
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(now.time)
                
                // Enviar timestamp UNIX en milisegundos para referencia
                val createdAtStr = now.timeInMillis.toString()

                val response = repository.saveAnalysis(
                    token, imageFile.name, result.dishes, result.nutrition, 
                    result.plateAnalysis, result.mealType, createdAtStr, dateStr, timeStr
                )

                if (response.isSuccessful) {
                    // Limpiar caché para forzar actualización de la UI en Inicio
                    DataCacheManager.clearCache(this@AnalisisActivity)
                    Toast.makeText(this@AnalisisActivity, "¡Plato guardado!", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this@AnalisisActivity, InicioActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    finish()
                } else {
                    btnGuardarAnalisis?.isEnabled = true
                    btnGuardarAnalisis?.text = "Guardar"
                    Toast.makeText(this@AnalisisActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { 
                btnGuardarAnalisis?.isEnabled = true 
                btnGuardarAnalisis?.text = "Guardar"
                Log.e(TAG, "Error guardando", e)
            }
        }
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> { cancelar(); true }
                R.id.nav_historial -> { 
                    startActivity(Intent(this, HistorialActivity::class.java))
                    finish()
                    true 
                }
                R.id.nav_perfil -> { 
                    startActivity(Intent(this, PerfilActivity::class.java))
                    finish()
                    true 
                }
                else -> false
            }
        }
    }

    private fun cancelar() {
        if (imageFile.exists()) imageFile.delete()
        finish()
    }
}