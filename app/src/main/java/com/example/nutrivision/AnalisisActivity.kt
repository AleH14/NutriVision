package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
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

        btnGuardarAnalisis?.visibility = View.GONE
        layoutResultados?.visibility = View.GONE
    }

    private fun obtenerDatos() {
        val imagePath = intent.getStringExtra("imageFile") ?: ""
        imageFile = File(imagePath)
    }

    private fun cargarFoto() {
        if (imageFile.exists()) {
            try {
                val bitmap = ImageUtils.loadRotatedCircleBitmap(imageFile)
                ivFoto?.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }

    private fun realizarAnalisis() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            irALogin()
            return
        }

        lifecycleScope.launch {
            try {
                progressBarLoading?.visibility = View.VISIBLE
                layoutResultados?.visibility = View.GONE
                btnGuardarAnalisis?.visibility = View.GONE

                val fileBytes = imageFile.readBytes()
                val requestFile = fileBytes.toRequestBody("image/jpeg".toMediaType())
                val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                val response = repository.analyzeFoodImage(token, body)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (!result.isFood || result.analysis == null) {
                        mostrarUICuandoNoComida()
                        isFoodValid = false
                        currentAnalysisResult = null
                    } else {
                        isFoodValid = true
                        mostrarResultados(result)
                        btnGuardarAnalisis?.visibility = View.VISIBLE
                        btnGuardarAnalisis?.isEnabled = true
                    }
                } else {
                    // Si es código 422 (no comida) u otro error, mostramos la misma UI amigable
                    mostrarUICuandoNoComida()
                    isFoodValid = false
                    currentAnalysisResult = null
                }
            } catch (error: Exception) {
                mostrarUICuandoNoComida()
                isFoodValid = false
                currentAnalysisResult = null
            } finally {
                progressBarLoading?.visibility = View.GONE
            }
        }
    }

    private fun mostrarUICuandoNoComida() {
        // Título en naranja
        tvTituloPrincipal?.text = "No se pudo validar la imagen"
        tvTituloPrincipal?.setTextColor(ContextCompat.getColor(this, R.color.orange))

        // Mensaje descriptivo en gris
        tvAnalisisPlato?.text = "La imagen no parece ser de comida. Por favor, toma una foto de tu plato de comida."
        tvAnalisisPlato?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))

        // Valores nutricionales en "--"
        tvCaloriasInfo?.text = "-- kcal"
        tvProteinaInfo?.text = "-- g"
        tvCarbsInfo?.text = "-- g"
        tvGrasasInfo?.text = "-- g"

        tvCaloriasInfo?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))
        tvProteinaInfo?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))
        tvCarbsInfo?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))
        tvGrasasInfo?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))

        // Ocultar disclaimer de IA
        tvAiDisclaimer?.visibility = View.GONE

        // Mostrar layout
        layoutResultados?.visibility = View.VISIBLE
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

        tvTituloPrincipal?.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvAnalisisPlato?.setTextColor(ContextCompat.getColor(this, R.color.gris_claro))
        tvCaloriasInfo?.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvProteinaInfo?.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvCarbsInfo?.setTextColor(ContextCompat.getColor(this, R.color.black))
        tvGrasasInfo?.setTextColor(ContextCompat.getColor(this, R.color.black))

        tvAiDisclaimer?.apply {
            text = "Los valores nutricionales son estimaciones generadas por Inteligencia Artificial y no reemplazan la orientación de un profesional de la salud o nutricionista."
            visibility = View.VISIBLE
            setTextColor(ContextCompat.getColor(this@AnalisisActivity, R.color.gris_claro))
        }

        layoutResultados?.visibility = View.VISIBLE
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
        if (!isFoodValid || currentAnalysisResult == null) return

        val token = TokenManager.getToken(this)
        if (token == null) {
            irALogin()
            return
        }

        btnGuardarAnalisis?.isEnabled = false
        btnGuardarAnalisis?.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val analysis = currentAnalysisResult!!.analysis ?: return@launch
                val imageFilename = imageFile.name

                val now = Calendar.getInstance()
                val localDateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                    timeZone = TimeZone.getDefault()
                }.format(now.time)

                val localDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getDefault()
                }.format(now.time)

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
                    startActivity(Intent(this@AnalisisActivity, InicioActivity::class.java))
                    finish()
                } else {
                    btnGuardarAnalisis?.isEnabled = true
                    btnGuardarAnalisis?.text = "Guardar"
                }
            } catch (error: Exception) {
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

    private fun irALogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}