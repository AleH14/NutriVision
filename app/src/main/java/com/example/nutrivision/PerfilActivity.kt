package com.example.nutrivision

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.model.UpdateUserRequest
import com.example.nutrivision.data.model.User
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PerfilActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PerfilActivity"
        private const val CACHE_KEY = "user_profile_cache"
    }

    // Views principales
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnBackPerfil: TextView
    private lateinit var tvNombreUsuario: TextView
    private lateinit var btnEditarPerfil: TextView
    private lateinit var btnCerrarSesion: TextView
    private lateinit var btnCambiarPassword: TextView
    private lateinit var btnGuardarCambios: MaterialButton

    // EditTexts
    private lateinit var editEdad: EditText
    private lateinit var editAltura: EditText
    private lateinit var editPesoActual: EditText

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
    private lateinit var repository: NutriRepository

    // Datos del usuario (Estado local)
    private var generoActual = ""
    private var objetivoActual = ""
    private var dailyCalorieGoalKcal = 0

    // View para meta diaria
    private var tvMetaDiariaKcal: TextView? = null
    private var btnCalcularMetaDiaria: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        repository = NutriRepository(RetrofitClient.instance)

        initViews()
        setupNavigation()
        setupListeners()
        setupChipListeners()
        setupGeneroChips()
        
        // 1. Cargar caché inmediatamente (0ms lag)
        cargarDesdeCache()
        
        // 2. Refrescar desde el backend en segundo plano
        cargarDatosDelBackend()
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottomNavigation)
        btnBackPerfil = findViewById(R.id.btnBackPerfil)
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)

        editEdad = findViewById(R.id.editEdad)
        editAltura = findViewById(R.id.editAltura)
        editPesoActual = findViewById(R.id.editPesoObjetivo)

        tvGenero = findViewById(R.id.tvGenero)
        llGeneroEdicion = findViewById(R.id.llGeneroEdicion)
        chipGeneroFemenino = findViewById(R.id.chipGeneroFemenino)
        chipGeneroMasculino = findViewById(R.id.chipGeneroMasculino)

        chipMantenerPeso = findViewById(R.id.chipMantenerPeso)
        chipAumentarMusculo = findViewById(R.id.chipAumentarMusculo)
        chipSubirPeso = findViewById(R.id.chipSubirPeso)
        chipBajarPeso = findViewById(R.id.chipBajarPeso)

        tvMetaDiariaKcal = findViewById(R.id.tvMetaDiariaKcal)
        btnCalcularMetaDiaria = findViewById(R.id.btnCalcularMetaDiaria)
    }

    private fun cargarDesdeCache() {
        val cachedUser = DataCacheManager.getCache(this, CACHE_KEY, User::class.java)
        cachedUser?.let {
            Log.d(TAG, "Mostrando perfil desde caché")
            renderizarUsuario(it)
        }
    }

    private fun cargarDatosDelBackend() {
        val token = TokenManager.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                val response = repository.getProfile(token)
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!
                    
                    // Actualizar caché
                    DataCacheManager.saveCache(this@PerfilActivity, CACHE_KEY, usuario)
                    
                    renderizarUsuario(usuario)
                }
            } catch (error: Exception) {
                if (error !is CancellationException) {
                    Log.e(TAG, "Error de red al cargar perfil", error)
                }
            }
        }
    }

    private fun renderizarUsuario(usuario: User) {
        tvNombreUsuario.text = usuario.fullName
        editEdad.setText(usuario.age.toString())
        editAltura.setText(usuario.heightCm.toString())
        editPesoActual.setText(usuario.currentWeightLb.toString())
        
        generoActual = usuario.gender
        objetivoActual = usuario.personalGoal
        dailyCalorieGoalKcal = usuario.dailyCalorieGoalKcal.toInt()
        
        tvGenero.text = mapearGeneroAUI(generoActual)
        tvMetaDiariaKcal?.text = dailyCalorieGoalKcal.toString()

        actualizarChipSeleccionado()
        actualizarChipGenero()
    }

    private fun mapearGeneroAUI(genero: String): String {
        return when (genero.lowercase()) {
            "masculino" -> "Masculino"
            "femenino" -> "Femenino"
            else -> genero
        }
    }

    private fun actualizarChipGenero() {
        if (generoActual.lowercase() == "femenino") {
            chipGeneroFemenino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
            chipGeneroMasculino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
        } else if (generoActual.isNotEmpty()) {
            chipGeneroFemenino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
            chipGeneroMasculino.background = ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
        }
    }

    private fun actualizarChipSeleccionado() {
        val chips = mapOf(
            chipMantenerPeso to "mantener peso",
            chipAumentarMusculo to "aumentar musculo",
            chipSubirPeso to "subir peso",
            chipBajarPeso to "bajar peso"
        )
        chips.forEach { (chip, texto) ->
            chip.background = if (texto == objetivoActual.lowercase()) {
                ContextCompat.getDrawable(this, R.drawable.bg_profile_chip_selected)
            } else {
                ContextCompat.getDrawable(this, R.drawable.bg_profile_chip)
            }
        }
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
        btnBackPerfil.setOnClickListener { finish() }
        btnEditarPerfil.setOnClickListener {
            if (isEditing) cancelarEdicion() else entrarModoEdicion()
        }
        btnGuardarCambios.setOnClickListener { guardarCambios() }
        btnCambiarPassword.setOnClickListener {
            startActivity(Intent(this, CambiarPasswordActivity::class.java))
        }
        btnCerrarSesion.setOnClickListener {
            TokenManager.logout(this)
            DataCacheManager.clearCache(this) // Limpiar caché al cerrar sesión
            irALogin()
        }
        btnCalcularMetaDiaria?.setOnClickListener { calcularMetaDiaria() }
    }

    private fun setupGeneroChips() {
        chipGeneroFemenino.setOnClickListener { if (isEditing) seleccionarGenero("femenino") }
        chipGeneroMasculino.setOnClickListener { if (isEditing) seleccionarGenero("masculino") }
    }

    private fun seleccionarGenero(genero: String) {
        generoActual = genero
        actualizarChipGenero()
    }

    private fun setupChipListeners() {
        chipMantenerPeso.setOnClickListener { if (isEditing) seleccionarChip(chipMantenerPeso, "mantener peso") }
        chipAumentarMusculo.setOnClickListener { if (isEditing) seleccionarChip(chipAumentarMusculo, "aumentar musculo") }
        chipSubirPeso.setOnClickListener { if (isEditing) seleccionarChip(chipSubirPeso, "subir peso") }
        chipBajarPeso.setOnClickListener { if (isEditing) seleccionarChip(chipBajarPeso, "bajar peso") }
    }

    private fun seleccionarChip(chipSeleccionado: TextView, objetivo: String) {
        objetivoActual = objetivo
        actualizarChipSeleccionado()
    }

    private fun entrarModoEdicion() {
        isEditing = true
        btnEditarPerfil.text = "Cancelar"
        btnEditarPerfil.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        habilitarEditText(editEdad)
        habilitarEditText(editAltura)
        habilitarEditText(editPesoActual)
        tvGenero.visibility = View.GONE
        llGeneroEdicion.visibility = View.VISIBLE
        btnGuardarCambios.visibility = View.VISIBLE
    }

    private fun habilitarEditText(editText: EditText) {
        editText.isEnabled = true
        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@PerfilActivity, R.color.card_background))
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
    }

    private fun guardarCambios() {
        val nuevaEdad = editEdad.text.toString().trim()
        val nuevaAltura = editAltura.text.toString().trim()
        val nuevoPeso = editPesoActual.text.toString().trim()

        if (nuevaEdad.isEmpty() || nuevaAltura.isEmpty() || nuevoPeso.isEmpty()) {
            mostrarToastError("Por favor completa todos los campos")
            return
        }

        val edadInt = nuevaEdad.toInt()
        val alturaInt = nuevaAltura.toInt()
        val pesoInt = nuevoPeso.toInt()

        guardarCambiosEnBackend(edadInt, alturaInt, pesoInt)
    }

    private fun guardarCambiosEnBackend(edad: Int, altura: Int, peso: Int) {
        val token = TokenManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val updateRequest = UpdateUserRequest(
                    age = edad,
                    heightCm = altura,
                    currentWeightLb = peso,
                    gender = generoActual,
                    personalGoal = objetivoActual
                )
                val response = repository.updateProfile(token, updateRequest)
                if (response.isSuccessful && response.body() != null) {
                    val userUpdated = response.body()!!
                    DataCacheManager.saveCache(this@PerfilActivity, CACHE_KEY, userUpdated)
                    salirModoEdicion()
                    renderizarUsuario(userUpdated)
                    mostrarToastExito("Perfil actualizado")
                } else {
                    mostrarToastError("Error al actualizar perfil")
                }
            } catch (error: Exception) {
                if (error !is CancellationException) {
                    mostrarToastError("Error: ${error.message}")
                }
            }
        }
    }

    private fun salirModoEdicion() {
        isEditing = false
        btnEditarPerfil.text = "✎ Editar"
        btnEditarPerfil.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        deshabilitarEditText(editEdad)
        deshabilitarEditText(editAltura)
        deshabilitarEditText(editPesoActual)
        tvGenero.visibility = View.VISIBLE
        llGeneroEdicion.visibility = View.GONE
        btnGuardarCambios.visibility = View.GONE
    }

    private fun cancelarEdicion() {
        salirModoEdicion()
        cargarDesdeCache()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun mostrarToastExito(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun mostrarToastError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun calcularMetaDiaria() {
        val token = TokenManager.getToken(this) ?: return
        btnCalcularMetaDiaria?.isEnabled = false
        btnCalcularMetaDiaria?.text = "Calculando..."

        lifecycleScope.launch {
            try {
                val response = repository.calculateDailyGoal(token)
                if (response.isSuccessful && response.body() != null) {
                    val newGoal = response.body()!!.dailyCalorieGoalKcal ?: 0
                    tvMetaDiariaKcal?.text = newGoal.toString()
                    
                    // Actualizar caché forzosamente tras el cálculo
                    cargarDatosDelBackend() 
                    
                    mostrarToastExito("Meta calculada: $newGoal kcal")
                }
            } catch (error: Exception) {
                if (error !is CancellationException) {
                    mostrarToastError("Error: ${error.message}")
                }
            } finally {
                btnCalcularMetaDiaria?.isEnabled = true
                btnCalcularMetaDiaria?.text = "Calcular Meta"
            }
        }
    }
}