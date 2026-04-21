package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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
    private var tvAiDisclaimer: TextView? = null

    // Data
    private lateinit var imageFile: File
    private var currentAnalysisResult: FoodAnalysisResponse? = null
    private var isFoodValid = true

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
        tvAiDisclaimer = findViewById(R.id.tvAiDisclaimer)

        btnGuardarAnalisis?.setOnClickListener { guardarAnalisis() }
        findViewById<TextView>(R.id.btnBackAnalisis)?.setOnClickListener { cancelar() }

        // Ocultar botón guardar inicialmente
        btnGuardarAnalisis?.visibility = View.GONE
    }

    private fun obtenerDatos() {
        val imagePath = intent.getStringExtra("imageFile") ?: ""
        imageFile = File(imagePath)
        Log.d(TAG, "Archivo recibido: $imagePath")
    }

    private fun cargarFoto() {
        if (imageFile.exists()) {
            try {
                val bitmap = ImageUtils.loadRotatedCircleBitmap(imageFile)
                ivFoto?.setImageBitmap(bitmap)
                Log.d(TAG, "Foto cargada y recortada en círculo")
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar foto", e)
            }
        } else {
            Log.e(TAG, "Archivo de foto no existe")
        }
    }

    private fun realizarAnalisis() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            mostrarErrorValidacion("Token no encontrado")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Iniciando análisis...")
                progressBarLoading?.visibility = View.VISIBLE
                layoutResultados?.visibility = View.GONE
                btnGuardarAnalisis?.visibility = View.GONE

                val fileBytes = imageFile.readBytes()
                val requestFile = fileBytes.toRequestBody("image/jpeg".toMediaType())
                val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                val response = repository.analyzeFoodImage(token, body)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    Log.d(TAG, "Respuesta recibida: isFood=${result.isFood}, analysis=${result.analysis != null}")

                    // Verificar si la imagen es comida
                    if (!result.isFood || result.analysis == null) {
                        // No es comida
                        val mensaje = result.message ?: "La imagen no parece ser de comida. Por favor, toma una foto de tu plato de comida."
                        mostrarErrorValidacion(mensaje)
                    } else {
                        // Es comida válida
                        isFoodValid = true
                        mostrarResultados(result)
                        btnGuardarAnalisis?.visibility = View.VISIBLE
                        btnGuardarAnalisis?.isEnabled = true
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    Log.e(TAG, "Error: ${response.code()} - $errorBody")
                    mostrarErrorValidacion("Error al analizar: ${response.code()}")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Excepción", error)
                mostrarErrorValidacion("Error: ${error.message}")
            } finally {
                progressBarLoading?.visibility = View.GONE
            }
        }
    }

    private fun mostrarResultados(analysisResult: FoodAnalysisResponse) {
        currentAnalysisResult = analysisResult

        val analysis = analysisResult.analysis ?: return

        val titleText = analysis.dishes.joinToString(" + ") { it.name }
        tvTituloPrincipal?.text = titleText
        tvAnalisisPlato?.text = analysis.plateAnalysis
        tvCaloriasInfo?.text = "${analysis.nutrition.calories} kcal"
        tvProteinaInfo?.text = "${analysis.nutrition.proteinGrams.toInt()} g"
        tvCarbsInfo?.text = "${analysis.nutrition.carbsGrams.toInt()} g"
        tvGrasasInfo?.text = "${analysis.nutrition.fatGrams.toInt()} g"

        // Restaurar colores normales
        tvTituloPrincipal?.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvAnalisisPlato?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))
        tvCaloriasInfo?.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvProteinaInfo?.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvCarbsInfo?.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvGrasasInfo?.setTextColor(ContextCompat.getColor(this, R.color.black))

        // Aviso IA
        tvAiDisclaimer?.apply {
            text = "Los valores nutricionales son estimaciones generadas por Inteligencia Artificial y no reemplazan la orientación de un profesional de la salud o nutricionista."
            visibility = View.VISIBLE
            setTextColor(ContextCompat.getColor(this@AnalisisActivity, R.color.orange))
        }

        layoutResultados?.visibility = View.VISIBLE
        Log.d(TAG, "Resultados mostrados: ${analysis.nutrition.calories} kcal")
    }

    private fun mostrarErrorValidacion(mensaje: String) {
        isFoodValid = false
        currentAnalysisResult = null

        // Ocultar botón guardar
        btnGuardarAnalisis?.visibility = View.GONE

        // Mostrar solo el mensaje de error y la foto
        tvTituloPrincipal?.text = "No se pudo validar la imagen"
        tvTituloPrincipal?.setTextColor(ContextCompat.getColor(this, R.color.orange))

        tvAnalisisPlato?.text = mensaje
        tvAnalisisPlato?.setTextColor(ContextCompat.getColor(this, R.color.black))

        // Ocultar/Oscurecer los datos nutricionales
        tvCaloriasInfo?.text = "-- kcal"
        tvProteinaInfo?.text = "-- g"
        tvCarbsInfo?.text = "-- g"
        tvGrasasInfo?.text = "-- g"

        tvCaloriasInfo?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))
        tvProteinaInfo?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))
        tvCarbsInfo?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))
        tvGrasasInfo?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))

        // Ocultar aviso IA
        tvAiDisclaimer?.visibility = View.GONE

        layoutResultados?.visibility = View.VISIBLE
        Log.e(TAG, mensaje)
    }

    private fun mostrarError(mensaje: String) {
        Log.e(TAG, mensaje)
        tvTituloPrincipal?.text = "Error al analizar"
        tvAnalisisPlato?.text = mensaje
        layoutResultados?.visibility = View.VISIBLE
        btnGuardarAnalisis?.visibility = View.GONE
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
        if (!isFoodValid) {
            Log.e(TAG, "No se puede guardar: la imagen no es comida válida")
            return
        }

        if (currentAnalysisResult == null) {
            Log.e(TAG, "No hay análisis para guardar")
            return
        }

        val token = TokenManager.getToken(this)
        if (token == null) {
            Log.e(TAG, "Sesión expirada")
            return
        }

        btnGuardarAnalisis?.isEnabled = false
        btnGuardarAnalisis?.text = "Guardando..."

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Guardando análisis...")

                val analysis = currentAnalysisResult!!.analysis ?: return@launch

                val imageFilename = imageFile.name

                val now = Calendar.getInstance()
                val localDateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getDefault()
                }.format(now.time)

                val localDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getDefault()
                }.format(now.time)

                Log.d(TAG, "📅 Fecha local para guardar: $localDateTime")
                Log.d(TAG, "📅 Fecha (solo día): $localDate")
                Log.d(TAG, "📷 Nombre de imagen: $imageFilename")

                val response = repository.saveAnalysis(
                    token = token,
                    imageFilename = imageFilename,
                    dishes = analysis.dishes,
                    nutrition = analysis.nutrition,
                    plateAnalysis = analysis.plateAnalysis,
                    mealType = analysis.mealType,
                    createdAt = localDateTime,
                    date = localDate
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Análisis guardado exitosamente")
                    startActivity(Intent(this@AnalisisActivity, InicioActivity::class.java))
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    Log.e(TAG, "❌ Error al guardar: ${response.code()} - $errorBody")
                    btnGuardarAnalisis?.isEnabled = true
                    btnGuardarAnalisis?.text = "Guardar"
                }
            } catch (error: Exception) {
                Log.e(TAG, "❌ Error al guardar", error)
                btnGuardarAnalisis?.isEnabled = true
                btnGuardarAnalisis?.text = "Guardar"
            }
        }
    }

    private fun cancelar() {
        if (imageFile.exists()) {
            imageFile.delete()
        }
        startActivity(Intent(this, InicioActivity::class.java))
        finish()
    }
}