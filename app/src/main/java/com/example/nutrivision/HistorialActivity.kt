package com.example.nutrivision

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class HistorialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_historial)

        //menu barra de navegacion
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        // Marcar que estamos en inicio
        bottomNav.selectedItemId = R.id.nav_historial
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {

                R.id.nav_inicio -> {
                    startActivity(Intent(this, InicioActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_historial -> true

                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}