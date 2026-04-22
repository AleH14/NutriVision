package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.nutrivision.data.model.RegisterRequest
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.example.nutrivision.ui.viewmodel.AuthViewModel
import com.example.nutrivision.ui.viewmodel.ViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DatosInicialesActivity : AppCompatActivity() {

    private lateinit var lblEdad: TextInputLayout
    private lateinit var lblAltura: TextInputLayout
    private lateinit var lblPesoActual: TextInputLayout
    private lateinit var txtEdad: TextInputEditText
    private lateinit var txtAltura: TextInputEditText
    private lateinit var txtPesoActual: TextInputEditText

    private lateinit var btnGeneroMasculino: TextView
    private lateinit var btnGeneroFemenino: TextView
    private lateinit var btnActividadSedentario: TextView
    private lateinit var btnActividadLigero: TextView
    private lateinit var btnActividadModerado: TextView
    private lateinit var btnActividadIntenso: TextView
    private lateinit var btnObjetivoMantener: TextView
    private lateinit var btnObjetivoAumentar: TextView
    private lateinit var btnObjetivoSubir: TextView
    private lateinit var btnObjetivoBajar: TextView
    private lateinit var btnGuardarPerfil: MaterialButton
    private lateinit var btnBackPerfil: TextView
    private lateinit var progressBar: ProgressBar

    private var generoSeleccionado: String? = null
    private var actividadSeleccionada: String? = null
    private var objetivoSeleccionado: String? = null

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_iniciales)

        initViewModel()
        initViews()
        setupListeners()
        setupSelectionListeners()
        observeViewModel()
    }

    private fun initViewModel() {
        val repository = NutriRepository(RetrofitClient.instance)
        val factory = ViewModelFactory(repository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
    }

    private fun initViews() {
        lblEdad = findViewById(R.id.lblEdad)
        lblAltura = findViewById(R.id.lblAltura)
        lblPesoActual = findViewById(R.id.lblPesoActual)
        txtEdad = findViewById(R.id.txtEdad)
        txtAltura = findViewById(R.id.txtAltura)
        txtPesoActual = findViewById(R.id.txtPesoActual)

        btnGeneroMasculino = findViewById(R.id.btnGeneroMasculino)
        btnGeneroFemenino = findViewById(R.id.btnGeneroFemenino)
        btnActividadSedentario = findViewById(R.id.btnActividadSedentario)
        btnActividadLigero = findViewById(R.id.btnActividadLigero)
        btnActividadModerado = findViewById(R.id.btnActividadModerado)
        btnActividadIntenso = findViewById(R.id.btnActividadIntenso)
        btnObjetivoMantener = findViewById(R.id.btnObjetivoMantener)
        btnObjetivoAumentar = findViewById(R.id.btnObjetivoAumentar)
        btnObjetivoSubir = findViewById(R.id.btnObjetivoSubir)
        btnObjetivoBajar = findViewById(R.id.btnObjetivoBajar)
        btnGuardarPerfil = findViewById(R.id.btnGuardarPerfil)
        btnBackPerfil = findViewById(R.id.btnBackPerfil)

        progressBar = findViewById(R.id.progressBarDatos) ?: ProgressBar(this)
    }

    private fun setupListeners() {
        btnBackPerfil.setOnClickListener {
            finish()
        }

        btnGuardarPerfil.setOnClickListener {
            validarYEnviar()
        }
    }

    private fun observeViewModel() {
        authViewModel.authResult.observe(this) { result ->
            // Restaurar botón
            btnGuardarPerfil.isEnabled = true
            btnGuardarPerfil.text = "Guardar"

            result.onSuccess { authResponse ->
                // Guardar token
                authResponse.token?.let { token ->
                    TokenManager.saveToken(this@DatosInicialesActivity, token)
                }

                // Guardar info del usuario
                authResponse.user?.let { user ->
                    TokenManager.saveUserInfo(
                        this@DatosInicialesActivity,
                        user.id ?: "",
                        user.email,
                        user.fullName
                    )
                }

                mostrarToastExitoso("✓ Registro completo. ¡Bienvenido!")
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, InicioActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                }, 1500)
            }.onFailure { exception ->
                mostrarToastError("Error al registrar: ${exception.message}")
            }
        }

        authViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                btnGuardarPerfil.isEnabled = false
                btnGuardarPerfil.text = "Cargando..."
            } else {
                // No restaurar aquí porque se restaura en el resultado
            }
        }
    }

    private fun setupSelectionListeners() {
        btnGeneroMasculino.setOnClickListener { seleccionarGenero("masculino", btnGeneroMasculino, btnGeneroFemenino) }
        btnGeneroFemenino.setOnClickListener { seleccionarGenero("femenino", btnGeneroFemenino, btnGeneroMasculino) }

        btnActividadSedentario.setOnClickListener { seleccionarActividad("sedentario", btnActividadSedentario, listOf(btnActividadLigero, btnActividadModerado, btnActividadIntenso)) }
        btnActividadLigero.setOnClickListener { seleccionarActividad("ligero", btnActividadLigero, listOf(btnActividadSedentario, btnActividadModerado, btnActividadIntenso)) }
        btnActividadModerado.setOnClickListener { seleccionarActividad("moderado", btnActividadModerado, listOf(btnActividadSedentario, btnActividadLigero, btnActividadIntenso)) }
        btnActividadIntenso.setOnClickListener { seleccionarActividad("intenso", btnActividadIntenso, listOf(btnActividadSedentario, btnActividadLigero, btnActividadModerado)) }

        btnObjetivoMantener.setOnClickListener { seleccionarObjetivo("mantener peso", btnObjetivoMantener, listOf(btnObjetivoAumentar, btnObjetivoSubir, btnObjetivoBajar)) }
        btnObjetivoAumentar.setOnClickListener { seleccionarObjetivo("aumentar musculo", btnObjetivoAumentar, listOf(btnObjetivoMantener, btnObjetivoSubir, btnObjetivoBajar)) }
        btnObjetivoSubir.setOnClickListener { seleccionarObjetivo("subir peso", btnObjetivoSubir, listOf(btnObjetivoMantener, btnObjetivoAumentar, btnObjetivoBajar)) }
        btnObjetivoBajar.setOnClickListener { seleccionarObjetivo("bajar peso", btnObjetivoBajar, listOf(btnObjetivoMantener, btnObjetivoAumentar, btnObjetivoSubir)) }
    }

    private fun seleccionarGenero(genero: String, selected: TextView, other: TextView) {
        generoSeleccionado = genero
        aplicarEstiloSeleccionado(selected, other)
    }

    private fun seleccionarActividad(actividad: String, selected: TextView, others: List<TextView>) {
        actividadSeleccionada = actividad
        aplicarEstiloSeleccionadoMultiple(selected, others)
    }

    private fun seleccionarObjetivo(objetivo: String, selected: TextView, others: List<TextView>) {
        objetivoSeleccionado = objetivo
        aplicarEstiloSeleccionadoMultiple(selected, others)
    }

    private fun aplicarEstiloSeleccionado(selected: TextView, other: TextView) {
        selected.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
        other.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
    }

    private fun aplicarEstiloSeleccionadoMultiple(selected: TextView, others: List<TextView>) {
        selected.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
        others.forEach { it.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip) }
    }

    private fun validarYEnviar() {
        val edad = txtEdad.text.toString().trim()
        val altura = txtAltura.text.toString().trim()
        val pesoActual = txtPesoActual.text.toString().trim()

        var isValid = true

        if (edad.isEmpty()) { lblEdad.error = "Requerido"; isValid = false } else lblEdad.error = null
        if (altura.isEmpty()) { lblAltura.error = "Requerido"; isValid = false } else lblAltura.error = null
        if (pesoActual.isEmpty()) { lblPesoActual.error = "Requerido"; isValid = false } else lblPesoActual.error = null

        if (generoSeleccionado == null || actividadSeleccionada == null || objetivoSeleccionado == null) {
            mostrarToastError("Completa todas las selecciones")
            isValid = false
        }

        if (isValid) {
            val nombre = intent.getStringExtra("EXTRA_NOMBRE") ?: ""
            val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
            val password = intent.getStringExtra("EXTRA_PASSWORD") ?: ""

            val caloriasBase = 2000.0

            val request = RegisterRequest(
                fullName = nombre,
                email = email,
                password = password,
                age = edad.toInt(),
                heightCm = altura.toInt(),
                currentWeightLb = pesoActual.toInt(),
                gender = generoSeleccionado!!,
                physicalActivity = actividadSeleccionada!!,
                personalGoal = objetivoSeleccionado!!,
                dailyCalorieGoalKcal = caloriasBase
            )

            // Cambiar texto del botón a "Cargando..."
            btnGuardarPerfil.isEnabled = false
            btnGuardarPerfil.text = "Cargando..."

            authViewModel.register(request)
        }
    }

    @Suppress("DEPRECATION")
    private fun mostrarToastExitoso(mensaje: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(60, 20, 60, 20)
        }
        val background = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@DatosInicialesActivity, R.color.success_green))
            cornerRadius = 32f
        }
        layout.background = background
        val textView = TextView(this).apply {
            text = mensaje
            setTextColor(ContextCompat.getColor(this@DatosInicialesActivity, android.R.color.white))
            textSize = 14f
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
        }
        layout.addView(textView)
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 80)
        toast.show()
    }

    @Suppress("DEPRECATION")
    private fun mostrarToastError(mensaje: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(60, 20, 60, 20)
        }
        val background = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@DatosInicialesActivity, android.R.color.holo_red_dark))
            cornerRadius = 32f
        }
        layout.background = background
        val textView = TextView(this).apply {
            text = mensaje
            setTextColor(ContextCompat.getColor(this@DatosInicialesActivity, android.R.color.white))
            textSize = 14f
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
        }
        layout.addView(textView)
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 80)
        toast.show()
    }
}