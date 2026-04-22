package com.example.nutrivision

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.model.LoginRequest
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var lblEmail: TextInputLayout
    private lateinit var lblPassword: TextInputLayout
    private lateinit var txtEmail: TextInputEditText
    private lateinit var txtPassword: TextInputEditText
    private lateinit var btnIniciarSesion: MaterialButton
    private lateinit var txtOlvidarPassword: TextView
    private lateinit var btnIrRegistro: TextView

    private lateinit var repository: NutriRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        repository = NutriRepository(RetrofitClient.instance)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        lblEmail = findViewById(R.id.lblEmail)
        lblPassword = findViewById(R.id.lblPassword)
        txtEmail = findViewById(R.id.txtEmail)
        txtPassword = findViewById(R.id.txtPassword)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        txtOlvidarPassword = findViewById(R.id.txtOlvidarPassword)
        btnIrRegistro = findViewById(R.id.btnIrRegistro)
    }

    private fun setupListeners() {
        btnIniciarSesion.setOnClickListener {
            realizarLogin()
        }

        txtOlvidarPassword.setOnClickListener {
            val intent = Intent(this, CambiarPasswordActivity::class.java)
            startActivity(intent)
        }

        btnIrRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun realizarLogin() {
        val email = txtEmail.text.toString().trim()
        val password = txtPassword.text.toString().trim()

        var isValid = true

        if (email.isEmpty()) {
            lblEmail.error = "El correo es requerido"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            lblEmail.error = "Correo electrónico inválido"
            isValid = false
        } else {
            lblEmail.error = null
        }

        if (password.isEmpty()) {
            lblPassword.error = "La contraseña es requerida"
            isValid = false
        } else if (password.length < 6) {
            lblPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        } else {
            lblPassword.error = null
        }

        if (isValid) {
            realizarLoginAlBackend(email, password)
        }
    }

    private fun realizarLoginAlBackend(email: String, password: String) {
        btnIniciarSesion.isEnabled = false
        btnIniciarSesion.text = "Iniciando sesión..."

        lifecycleScope.launch {
            try {
                val response = repository.login(LoginRequest(email, password))

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!

                    // Guardar token
                    authResponse.token?.let { token ->
                        TokenManager.saveToken(this@LoginActivity, token)
                    }

                    // Guardar info del usuario
                    authResponse.user?.let { user ->
                        TokenManager.saveUserInfo(
                            this@LoginActivity,
                            user.id ?: "",
                            user.email,
                            user.fullName
                        )
                    }

                    // Ir directamente a InicioActivity sin toast
                    val intent = Intent(this@LoginActivity, InicioActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // Solo mostrar error de credenciales inválidas
                    mostrarToastError("Credenciales inválidas")
                }
            } catch (error: Exception) {
                mostrarToastError("Credenciales inválidas")
            } finally {
                btnIniciarSesion.isEnabled = true
                btnIniciarSesion.text = "Iniciar Sesión"
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun mostrarToastError(mensaje: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(60, 20, 60, 20)
        }
        val background = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@LoginActivity, android.R.color.holo_red_dark))
            cornerRadius = 32f
        }
        layout.background = background
        val textView = TextView(this).apply {
            text = mensaje
            setTextColor(ContextCompat.getColor(this@LoginActivity, android.R.color.white))
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