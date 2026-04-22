package com.example.nutrivision

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class CambiarPasswordActivity : AppCompatActivity() {

    private lateinit var lblEmail: TextInputLayout
    private lateinit var lblNewPassword: TextInputLayout
    private lateinit var lblConfirmPassword: TextInputLayout
    private lateinit var txtEmail: TextInputEditText
    private lateinit var txtNewPassword: TextInputEditText
    private lateinit var txtConfirmPassword: TextInputEditText
    private lateinit var btnCambiarPassword: MaterialButton
    private lateinit var btnBack: TextView

    private lateinit var repository: NutriRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_password)

        repository = NutriRepository(RetrofitClient.instance)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        lblEmail = findViewById(R.id.lblEmail)
        lblNewPassword = findViewById(R.id.lblNewPassword)
        lblConfirmPassword = findViewById(R.id.lblConfirmPassword)
        txtEmail = findViewById(R.id.txtEmail)
        txtNewPassword = findViewById(R.id.txtNewPassword)
        txtConfirmPassword = findViewById(R.id.txtConfirmPassword)
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnCambiarPassword.setOnClickListener {
            cambiarPassword()
        }
    }

    private fun cambiarPassword() {
        val email = txtEmail.text.toString().trim()
        val newPassword = txtNewPassword.text.toString().trim()
        val confirmPassword = txtConfirmPassword.text.toString().trim()

        var isValid = true

        // EMAIL
        if (email.isEmpty()) {
            lblEmail.error = "Ingresa tu correo"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            lblEmail.error = "Correo inválido"
            isValid = false
        } else {
            lblEmail.error = null
        }

        // PASSWORD
        if (newPassword.length < 6) {
            lblNewPassword.error = "Mínimo 6 caracteres"
            isValid = false
        } else {
            lblNewPassword.error = null
        }

        // CONFIRM
        if (newPassword != confirmPassword) {
            lblConfirmPassword.error = "No coinciden"
            isValid = false
        } else {
            lblConfirmPassword.error = null
        }

        if (isValid) {
            realizarCambioPassword(email, newPassword)
        }
    }

    private fun realizarCambioPassword(email: String, newPassword: String) {
        btnCambiarPassword.isEnabled = false
        btnCambiarPassword.text = "Cambiando..."

        lifecycleScope.launch {
            try {
                val response = repository.changePassword(email, newPassword)

                if (response.isSuccessful) {
                    mostrarToastExitoso("Contraseña actualizada")
                    finish()
                } else {
                    val mensaje = when (response.code()) {
                        404 -> "Error al cambiar contraseña"
                        400 -> "Datos inválidos"
                        else -> "Error al cambiar contraseña"
                    }
                    mostrarToastError(mensaje)
                }

            } catch (e: Exception) {
                mostrarToastError("Error de conexión")
            } finally {
                btnCambiarPassword.isEnabled = true
                btnCambiarPassword.text = "Cambiar Contraseña"
            }
        }
    }

    // ✅ TOAST VERDE
    @Suppress("DEPRECATION")
    private fun mostrarToastExitoso(mensaje: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(60, 20, 60, 20)
        }

        val background = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@CambiarPasswordActivity, R.color.success_green))
            cornerRadius = 32f
        }

        layout.background = background

        val textView = TextView(this).apply {
            text = mensaje
            setTextColor(ContextCompat.getColor(this@CambiarPasswordActivity, android.R.color.white))
            textSize = 14f
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
        }

        layout.addView(textView)

        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 80)
        toast.show()
    }

    // ❌ TOAST ROJO
    @Suppress("DEPRECATION")
    private fun mostrarToastError(mensaje: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(60, 20, 60, 20)
        }

        val background = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@CambiarPasswordActivity, android.R.color.holo_red_dark))
            cornerRadius = 32f
        }

        layout.background = background

        val textView = TextView(this).apply {
            text = mensaje
            setTextColor(ContextCompat.getColor(this@CambiarPasswordActivity, android.R.color.white))
            textSize = 14f
            setTypeface(Typeface.DEFAULT_BOLD)
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