package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PerfilActivity : AppCompatActivity() {

    // Views principales
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnBackPerfil: TextView
    private lateinit var tvNombreUsuario: TextView
    private lateinit var btnEditarPerfil: TextView
    private lateinit var btnCerrarSesion: TextView
    private lateinit var btnGuardarCambios: MaterialButton

    // EditTexts
    private lateinit var editEdad: EditText
    private lateinit var editAltura: EditText
    private lateinit var editPesoObjetivo: EditText

    // Género
    private lateinit var tvGenero: TextView
    private lateinit var llGeneroEdicion: LinearLayout
    private lateinit var chipGeneroFemenino: TextView
    private lateinit var chipGeneroMasculino: TextView

    // Chips de objetivo
    private lateinit var chipMantenerPeso: TextView
    private lateinit var chipAumentarMusculo: TextView
    private lateinit var chipSubirPeso: TextView
    private lateinit var chipBajarPeso: TextView

    // Variables de estado
    private var isEditing = false

    // Datos del usuario quemados
    private var edadActual = "21"
    private var alturaActual = "150"
    private var generoActual = "Femenino"
    private var objetivoActual = "Mantener Peso"
    private var pesoObjetivoActual = "110"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        initViews()
        setupNavigation()
        setupListeners()
        setupChipListeners()
        setupGeneroChips()
        cargarDatosPerfil()
    }

    private fun initViews() {
        // Views principales
        bottomNav = findViewById(R.id.bottomNavigation)
        btnBackPerfil = findViewById(R.id.btnBackPerfil)
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)

        // EditTexts
        editEdad = findViewById(R.id.editEdad)
        editAltura = findViewById(R.id.editAltura)
        editPesoObjetivo = findViewById(R.id.editPesoObjetivo)

        // Género
        tvGenero = findViewById(R.id.tvGenero)
        llGeneroEdicion = findViewById(R.id.llGeneroEdicion)
        chipGeneroFemenino = findViewById(R.id.chipGeneroFemenino)
        chipGeneroMasculino = findViewById(R.id.chipGeneroMasculino)

        // Chips de objetivo
        chipMantenerPeso = findViewById(R.id.chipMantenerPeso)
        chipAumentarMusculo = findViewById(R.id.chipAumentarMusculo)
        chipSubirPeso = findViewById(R.id.chipSubirPeso)
        chipBajarPeso = findViewById(R.id.chipBajarPeso)
    }

    private fun setupNavigation() {
        bottomNav.selectedItemId = R.id.nav_perfil

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, InicioActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_historial -> {
                    startActivity(Intent(this, HistorialActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_perfil -> true
                else -> false
            }
        }
    }

    private fun setupListeners() {
        btnBackPerfil.setOnClickListener {
            finish()
        }

        btnEditarPerfil.setOnClickListener {
            if (isEditing) {
                cancelarEdicion()
            } else {
                entrarModoEdicion()
            }
        }

        btnGuardarCambios.setOnClickListener {
            guardarCambios()
        }

        btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun setupGeneroChips() {
        chipGeneroFemenino.setOnClickListener {
            if (isEditing) {
                seleccionarGenero("Femenino")
            }
        }

        chipGeneroMasculino.setOnClickListener {
            if (isEditing) {
                seleccionarGenero("Masculino")
            }
        }
    }

    private fun seleccionarGenero(genero: String) {
        generoActual = genero

        if (genero == "Femenino") {
            chipGeneroFemenino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
            chipGeneroMasculino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
        } else {
            chipGeneroFemenino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
            chipGeneroMasculino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
        }
    }

    private fun setupChipListeners() {
        chipMantenerPeso.setOnClickListener {
            if (isEditing) seleccionarChip(chipMantenerPeso, "Mantener Peso")
        }
        chipAumentarMusculo.setOnClickListener {
            if (isEditing) seleccionarChip(chipAumentarMusculo, "Aumentar músculo")
        }
        chipSubirPeso.setOnClickListener {
            if (isEditing) seleccionarChip(chipSubirPeso, "Subir Peso")
        }
        chipBajarPeso.setOnClickListener {
            if (isEditing) seleccionarChip(chipBajarPeso, "Bajar Peso")
        }
    }

    private fun seleccionarChip(chipSeleccionado: TextView, objetivo: String) {
        val chips = listOf(chipMantenerPeso, chipAumentarMusculo, chipSubirPeso, chipBajarPeso)
        chips.forEach { chip ->
            chip.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
        }
        chipSeleccionado.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
        objetivoActual = objetivo
    }

    private fun cargarDatosPerfil() {
        tvNombreUsuario.text = "Usuario NutriVision"
        editEdad.setText(edadActual)
        editAltura.setText(alturaActual)
        editPesoObjetivo.setText(pesoObjetivoActual)
        tvGenero.text = generoActual

        actualizarChipSeleccionado()
        actualizarChipGenero()
    }

    private fun actualizarChipGenero() {
        if (generoActual == "Femenino") {
            chipGeneroFemenino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
            chipGeneroMasculino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
        } else {
            chipGeneroFemenino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
            chipGeneroMasculino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
        }
    }

    private fun actualizarChipSeleccionado() {
        val chips = mapOf(
            chipMantenerPeso to "Mantener Peso",
            chipAumentarMusculo to "Aumentar músculo",
            chipSubirPeso to "Subir Peso",
            chipBajarPeso to "Bajar Peso"
        )
        chips.forEach { (chip, texto) ->
            chip.background = if (texto == objetivoActual) {
                ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
            } else {
                ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
            }
        }
    }

    private fun entrarModoEdicion() {
        isEditing = true
        btnEditarPerfil.text = "Cancelar"
        btnEditarPerfil.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

        // Habilitar EditTexts con borde
        habilitarEditText(editEdad)
        habilitarEditText(editAltura)
        habilitarEditText(editPesoObjetivo)

        // Ocultar TextView, mostrar chips de género (vertical)
        tvGenero.visibility = View.GONE
        llGeneroEdicion.visibility = View.VISIBLE

        // Mostrar botón guardar
        btnGuardarCambios.visibility = View.VISIBLE
    }

    private fun habilitarEditText(editText: EditText) {
        editText.isEnabled = true
        editText.isFocusable = true
        editText.isFocusableInTouchMode = true

        val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@PerfilActivity, android.R.color.white))
            setStroke(dpToPx(1), ContextCompat.getColor(this@PerfilActivity, R.color.gris_claro))
            cornerRadius = dpToPx(8).toFloat()
        }
        editText.background = borderDrawable
        editText.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
    }

    private fun deshabilitarEditText(editText: EditText) {
        editText.isEnabled = false
        editText.isFocusable = false
        editText.isFocusableInTouchMode = false
        editText.background = null
        editText.setPadding(0, 0, 0, 0)
        editText.error = null
    }

    private fun guardarCambios() {
        val nuevaEdad = editEdad.text.toString().trim()
        val nuevaAltura = editAltura.text.toString().trim()
        val nuevoPesoObjetivo = editPesoObjetivo.text.toString().trim()

        var isValid = true

        // Validar edad
        if (nuevaEdad.isEmpty()) {
            editEdad.error = "Edad requerida"
            isValid = false
        } else if (nuevaEdad.toIntOrNull() == null) {
            editEdad.error = "Número válido"
            isValid = false
        } else if (nuevaEdad.toInt() < 15 || nuevaEdad.toInt() > 120) {
            editEdad.error = "15-120 años"
            isValid = false
        } else {
            editEdad.error = null
        }

        // Validar altura
        if (nuevaAltura.isEmpty()) {
            editAltura.error = "Altura requerida"
            isValid = false
        } else if (nuevaAltura.toDoubleOrNull() == null) {
            editAltura.error = "Número válido"
            isValid = false
        } else if (nuevaAltura.toDouble() < 50 || nuevaAltura.toDouble() > 250) {
            editAltura.error = "50-250 cm"
            isValid = false
        } else {
            editAltura.error = null
        }

        // Validar peso objetivo
        if (nuevoPesoObjetivo.isEmpty()) {
            editPesoObjetivo.error = "Peso requerido"
            isValid = false
        } else if (nuevoPesoObjetivo.toDoubleOrNull() == null) {
            editPesoObjetivo.error = "Número válido"
            isValid = false
        } else if (nuevoPesoObjetivo.toDouble() < 20 || nuevoPesoObjetivo.toDouble() > 500) {
            editPesoObjetivo.error = "20-500 lb"
            isValid = false
        } else {
            editPesoObjetivo.error = null
        }

        if (isValid) {
            // Actualizar valores
            edadActual = nuevaEdad
            alturaActual = nuevaAltura
            pesoObjetivoActual = nuevoPesoObjetivo

            salirModoEdicion()
            cargarDatosPerfil()
        }
    }

    private fun salirModoEdicion() {
        isEditing = false
        btnEditarPerfil.text = "✎ Editar"
        btnEditarPerfil.setTextColor(ContextCompat.getColor(this, R.color.success_green))

        // Deshabilitar EditTexts
        deshabilitarEditText(editEdad)
        deshabilitarEditText(editAltura)
        deshabilitarEditText(editPesoObjetivo)

        // Ocultar chips, mostrar TextView de género
        tvGenero.visibility = View.VISIBLE
        llGeneroEdicion.visibility = View.GONE

        // Ocultar botón guardar
        btnGuardarCambios.visibility = View.GONE
    }

    private fun cancelarEdicion() {
        salirModoEdicion()
        cargarDatosPerfil()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}