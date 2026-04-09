package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class PerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
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

        findViewById<TextView>(R.id.btnBackPerfil).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnEditarPerfil).setOnClickListener {
            Toast.makeText(this, "Editar perfil", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnCerrarSesion).setOnClickListener {
            Toast.makeText(this, "Cerrar sesión", Toast.LENGTH_SHORT).show()
        }
    }
}