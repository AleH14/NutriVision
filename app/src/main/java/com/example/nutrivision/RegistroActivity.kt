package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegistroActivity : AppCompatActivity() {

    private lateinit var lblNombre: TextInputLayout
    private lateinit var lblEmail: TextInputLayout
    private lateinit var lblPassword: TextInputLayout
    private lateinit var lblConfirmPassword: TextInputLayout
    private lateinit var txtNombre: TextInputEditText
    private lateinit var txtEmail: TextInputEditText
    private lateinit var txtPassword: TextInputEditText
    private lateinit var txtConfirmPassword: TextInputEditText
    private lateinit var btnRegistrarse: MaterialButton
    private lateinit var btnBackRegistro: TextView
    private lateinit var btnIrLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        lblNombre = findViewById(R.id.lblNombre)
        lblEmail = findViewById(R.id.lblEmail)
        lblPassword = findViewById(R.id.lblPassword)
        lblConfirmPassword = findViewById(R.id.lblConfirmPassword)
        txtNombre = findViewById(R.id.txtNombre)
        txtEmail = findViewById(R.id.txtEmail)
        txtPassword = findViewById(R.id.txtPassword)
        txtConfirmPassword = findViewById(R.id.txtConfirmPassword)
        btnRegistrarse = findViewById(R.id.btnRegistrarse)
        btnBackRegistro = findViewById(R.id.btnBackRegistro)
        btnIrLogin = findViewById(R.id.btnIrLogin)
    }

    private fun setupListeners() {
        btnRegistrarse.setOnClickListener {
            irADatosIniciales()
        }

        btnBackRegistro.setOnClickListener {
            finish()
        }

        btnIrLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun irADatosIniciales() {
        val nombre = txtNombre.text.toString().trim()
        val email = txtEmail.text.toString().trim()
        val password = txtPassword.text.toString().trim()
        val confirmPassword = txtConfirmPassword.text.toString().trim()

        var isValid = true

        if (nombre.isEmpty()) {
            lblNombre.error = "El nombre es requerido"
            isValid = false
        } else lblNombre.error = null

        if (email.isEmpty()) {
            lblEmail.error = "El correo es requerido"
            isValid = false
        } else lblEmail.error = null

        if (password.isEmpty() || password.length < 6) {
            lblPassword.error = "Contraseña inválida (mín. 6 caracteres)"
            isValid = false
        } else lblPassword.error = null

        if (password != confirmPassword) {
            lblConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else lblConfirmPassword.error = null

        if (isValid) {
            // Pasamos los datos a la siguiente pantalla sin registrar todavía
            val intent = Intent(this, DatosInicialesActivity::class.java)
            intent.putExtra("EXTRA_NOMBRE", nombre)
            intent.putExtra("EXTRA_EMAIL", email)
            intent.putExtra("EXTRA_PASSWORD", password)
            startActivity(intent)
        }
    }
}
