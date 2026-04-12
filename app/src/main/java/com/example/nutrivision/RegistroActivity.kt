package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
            realizarRegistro()
        }

        btnBackRegistro.setOnClickListener {
            finish()
        }

        btnIrLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun realizarRegistro() {
        val nombre = txtNombre.text.toString().trim()
        val email = txtEmail.text.toString().trim()
        val password = txtPassword.text.toString().trim()
        val confirmPassword = txtConfirmPassword.text.toString().trim()

        var isValid = true

        if (nombre.isEmpty()) {
            lblNombre.error = "El nombre es requerido"
            isValid = false
        } else {
            lblNombre.error = null
        }

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

        if (confirmPassword.isEmpty()) {
            lblConfirmPassword.error = "Confirma tu contraseña"
            isValid = false
        } else if (password != confirmPassword) {
            lblConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            lblConfirmPassword.error = null
        }

        // Navegación a datos iniciales de usuario

        if (isValid) {
            mostrarToastExitoso()

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, DatosInicialesActivity::class.java)
                startActivity(intent)
                finish()
            }, 1500)
        }
    }

    @Suppress("DEPRECATION")
    private fun mostrarToastExitoso() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(50, 30, 50, 30)
            elevation = 12f
        }

        // Fondo
        val background = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@RegistroActivity, R.color.success_green))
            cornerRadius = 100f
        }
        layout.background = background

        // Icono check
        val icon = TextView(this).apply {
            text = "✔"
            setTextColor(ContextCompat.getColor(this@RegistroActivity, android.R.color.white))
            textSize = 18f
            setPadding(0, 0, 20, 0)
        }

        // Texto principal
        val textView = TextView(this).apply {
            text = "Cuenta creada exitosamente"
            setTextColor(ContextCompat.getColor(this@RegistroActivity, android.R.color.white))
            textSize = 15f
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        }

        layout.addView(icon)
        layout.addView(textView)

        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout

        // Posición tipo notificación flotante
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 120)
        toast.show()

        Handler(Looper.getMainLooper()).postDelayed({
            toast.cancel()
        }, 1200)
    }
}