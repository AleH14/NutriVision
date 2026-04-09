package com.example.nutrivision

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class AnalisisActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_DESCRIPCION = "extra_descripcion"
        const val EXTRA_CALORIAS = "extra_calorias"
        const val EXTRA_CARBS = "extra_carbs"
        const val EXTRA_PROTEINAS = "extra_proteinas"
        const val EXTRA_GRASAS = "extra_grasas"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analisis)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_inicio

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> true

                R.id.nav_historial -> {
                    startActivity(Intent(this, HistorialActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                else -> false
            }
        }

        findViewById<TextView>(R.id.btnBackAnalisis).setOnClickListener {
            finish()
        }

        val nombre = intent.getStringExtra(EXTRA_NOMBRE) ?: "Pizza Casera"
        val descripcion = intent.getStringExtra(EXTRA_DESCRIPCION)
            ?: "Esta pizza cuenta con una base de masa madre, queso mozarela derretidos, aceitunas con hueso y chile rojo dulce...todos los ingredientes"
        val calorias = intent.getStringExtra(EXTRA_CALORIAS) ?: "800"
        val carbs = intent.getStringExtra(EXTRA_CARBS) ?: "600g"
        val proteinas = intent.getStringExtra(EXTRA_PROTEINAS) ?: "80g"
        val grasas = intent.getStringExtra(EXTRA_GRASAS) ?: "120g"
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)

        findViewById<TextView>(R.id.tvFoodName).text = nombre
        findViewById<TextView>(R.id.tvFoodDescription).text = descripcion
        findViewById<TextView>(R.id.tvCalories).text = calorias
        findViewById<TextView>(R.id.tvCarbs).text = carbs
        findViewById<TextView>(R.id.tvProteins).text = proteinas
        findViewById<TextView>(R.id.tvFats).text = grasas

        val ivFood = findViewById<ImageView>(R.id.ivFoodImage)
        if (!imageUriString.isNullOrBlank()) {
            ivFood.setImageURI(Uri.parse(imageUriString))
        }

        findViewById<MaterialButton>(R.id.btnGuardar).setOnClickListener {
            Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
        }
    }
}