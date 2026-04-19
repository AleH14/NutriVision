package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            Toast.makeText(this, "Recuperar contraseña", Toast.LENGTH_SHORT).show()
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
                    
                    mostrarToastExito("¡Sesión iniciada exitosamente!")
                    
                    val intent = Intent(this@LoginActivity, InicioActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = response.body()?.message ?: "Error en el login"
                    mostrarToastError(errorMessage)
                }
            } catch (error: Exception) {
                mostrarToastError("Error de conexión: ${error.message}")
            } finally {
                btnIniciarSesion.isEnabled = true
                btnIniciarSesion.text = "Iniciar Sesión"
            }
        }
    }

    private fun mostrarToastExito(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun mostrarToastError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}