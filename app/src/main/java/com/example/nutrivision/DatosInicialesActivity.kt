package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private var generoSeleccionado: String? = null
    private var actividadSeleccionada: String? = null
    private var objetivoSeleccionado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_iniciales)

        initViews()
        setupListeners()
        setupSelectionListeners()
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
        btnActividadSedentario = findViewById(R.id. btnActividadSedentario)
        btnActividadLigero = findViewById(R.id.btnActividadLigero)
        btnActividadModerado = findViewById(R.id.btnActividadModerado)
        btnActividadIntenso = findViewById(R.id.btnActividadIntenso)
        btnObjetivoMantener = findViewById(R.id.btnObjetivoMantener)
        btnObjetivoAumentar = findViewById(R.id.btnObjetivoAumentar)
        btnObjetivoSubir = findViewById(R.id.btnObjetivoSubir)
        btnObjetivoBajar = findViewById(R.id.btnObjetivoBajar)
        btnGuardarPerfil = findViewById(R.id.btnGuardarPerfil)
        btnBackPerfil = findViewById(R.id.btnBackPerfil)
    }

    private fun setupListeners() {
        btnBackPerfil.setOnClickListener {
            finish()
        }

        btnGuardarPerfil.setOnClickListener {
            guardarPerfil()
        }
    }

    private fun setupSelectionListeners() {
        // Género
        btnGeneroMasculino.setOnClickListener {
            seleccionarGenero("Masculino", btnGeneroMasculino, btnGeneroFemenino)
        }
        btnGeneroFemenino.setOnClickListener {
            seleccionarGenero("Femenino", btnGeneroFemenino, btnGeneroMasculino)
        }

        // Actividad física
        btnActividadSedentario.setOnClickListener {
            seleccionarActividad("Sedentario", btnActividadSedentario,
                listOf(btnActividadLigero, btnActividadModerado, btnActividadIntenso))
        }
        btnActividadLigero.setOnClickListener {
            seleccionarActividad("Ligero", btnActividadLigero,
                listOf(btnActividadSedentario, btnActividadModerado, btnActividadIntenso))
        }
        btnActividadModerado.setOnClickListener {
            seleccionarActividad("Moderado", btnActividadModerado,
                listOf(btnActividadSedentario, btnActividadLigero, btnActividadIntenso))
        }
        btnActividadIntenso.setOnClickListener {
            seleccionarActividad("Intenso", btnActividadIntenso,
                listOf(btnActividadSedentario, btnActividadLigero, btnActividadModerado))
        }

        // Objetivo
        btnObjetivoMantener.setOnClickListener {
            seleccionarObjetivo("Mantener peso", btnObjetivoMantener,
                listOf(btnObjetivoAumentar, btnObjetivoSubir, btnObjetivoBajar))
        }
        btnObjetivoAumentar.setOnClickListener {
            seleccionarObjetivo("Aumentar músculo", btnObjetivoAumentar,
                listOf(btnObjetivoMantener, btnObjetivoSubir, btnObjetivoBajar))
        }
        btnObjetivoSubir.setOnClickListener {
            seleccionarObjetivo("Subir peso", btnObjetivoSubir,
                listOf(btnObjetivoMantener, btnObjetivoAumentar, btnObjetivoBajar))
        }
        btnObjetivoBajar.setOnClickListener {
            seleccionarObjetivo("Bajar peso", btnObjetivoBajar,
                listOf(btnObjetivoMantener, btnObjetivoAumentar, btnObjetivoSubir))
        }
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
        others.forEach {
            it.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
        }
    }

    private fun guardarPerfil() {
        val edad = txtEdad.text.toString().trim()
        val altura = txtAltura.text.toString().trim()
        val pesoActual = txtPesoActual.text.toString().trim()

        var isValid = true

        if (edad.isEmpty()) {
            lblEdad.error = "La edad es requerida"
            isValid = false
        } else if (edad.toIntOrNull() == null || edad.toInt() < 15 || edad.toInt() > 120) {
            lblEdad.error = "Ingresa una edad válida (15-120 años)"
            isValid = false
        } else {
            lblEdad.error = null
        }

        if (altura.isEmpty()) {
            lblAltura.error = "La altura es requerida"
            isValid = false
        } else if (altura.toDoubleOrNull() == null || altura.toDouble() < 50 || altura.toDouble() > 250) {
            lblAltura.error = "Ingresa una altura válida (50-250 cm)"
            isValid = false
        } else {
            lblAltura.error = null
        }

        if (pesoActual.isEmpty()) {
            lblPesoActual.error = "El peso es requerido"
            isValid = false
        } else if (pesoActual.toDoubleOrNull() == null || pesoActual.toDouble() < 20 || pesoActual.toDouble() > 500) {
            lblPesoActual.error = "Ingresa un peso válido (20-500 lb)"
            isValid = false
        } else {
            lblPesoActual.error = null
        }

        if (generoSeleccionado == null) {
            mostrarToastError("Selecciona tu género")
            isValid = false
        }

        if (actividadSeleccionada == null) {
            mostrarToastError("Selecciona tu nivel de actividad física")
            isValid = false
        }

        if (objetivoSeleccionado == null) {
            mostrarToastError("Selecciona tu objetivo personal")
            isValid = false
        }

        if (isValid) {
            // Aquí guardarías los datos en SharedPreferences o Base de Datos
            mostrarToastExitoso("✓ Perfil completado exitosamente")

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, InicioActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }, 1500)
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

        Handler(Looper.getMainLooper()).postDelayed({
            toast.cancel()
        }, 1000)
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

        Handler(Looper.getMainLooper()).postDelayed({
            toast.cancel()
        }, 1500)
    }
}