package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var lblEmail: TextInputLayout
    private lateinit var lblPassword: TextInputLayout
    private lateinit var txtEmail: TextInputEditText
    private lateinit var txtPassword: TextInputEditText
    private lateinit var btnIniciarSesion: MaterialButton
    private lateinit var txtOlvidarPassword: TextView
    private lateinit var btnIrRegistro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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
        } else {
            lblPassword.error = null
        }

        if (isValid) {
            val intent = Intent(this, InicioActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}