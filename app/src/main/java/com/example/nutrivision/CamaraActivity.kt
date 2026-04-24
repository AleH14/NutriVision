package com.example.nutrivision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class CamaraActivity : AppCompatActivity() {

    private val TAG = "CamaraActivity"
    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private lateinit var repository: NutriRepository

    private var isCapturing = false
    private var isOpeningGallery = false
    private var galleryHandled = false // 🔥 evita doble callback

    // Permiso según versión
    private val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Selección de imagen
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        if (galleryHandled) {
            Log.d(TAG, "Resultado duplicado ignorado")
            return@registerForActivityResult
        }

        galleryHandled = true
        isOpeningGallery = false

        if (uri != null) {
            Log.d(TAG, "Imagen seleccionada: $uri")
            copiarUriAArchivo(uri)
        } else {
            Log.d(TAG, "Selección cancelada")
        }
    }

    // Permiso
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            galleryLauncher.launch("image/*")
        } else {
            isOpeningGallery = false
            Toast.makeText(this, "Se necesita permiso para acceder a la galería", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camara)

        repository = NutriRepository(RetrofitClient.instance)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, InicioActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_historial -> true
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        previewView = findViewById(R.id.previewCamara)
        startCamera()

        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnTomarFoto).setOnClickListener {
            if (!isCapturing) captureImage()
        }

        findViewById<ImageButton>(R.id.btnGaleria).setOnClickListener {
            if (!isOpeningGallery) abrirGaleria()
        }
    }

    private fun abrirGaleria() {
        if (isOpeningGallery) return

        isOpeningGallery = true
        galleryHandled = false // 🔥 reset importante

        if (ContextCompat.checkSelfPermission(this, galleryPermission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            galleryLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(galleryPermission)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

                Log.d(TAG, "Cámara iniciada correctamente")

            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar cámara", e)
                Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        isCapturing = true

        val file = createImageFile()

        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    Log.d(TAG, "Imagen capturada: ${file.absolutePath}")
                    navigateToAnalysis(file)
                }

                override fun onError(exc: ImageCaptureException) {
                    isCapturing = false
                    Log.e(TAG, "Error al capturar", exc)
                    Toast.makeText(this@CamaraActivity, "Error al capturar imagen", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun copiarUriAArchivo(uri: Uri) {
        try {
            val file = createImageFile()

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "Imagen copiada: ${file.absolutePath}")
                navigateToAnalysis(file)
            } else {
                Toast.makeText(this, "Error al leer la imagen", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al copiar imagen", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())

        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", cacheDir)
    }

    private fun navigateToAnalysis(file: File) {
        val intent = Intent(this, AnalisisActivity::class.java)
        intent.putExtra("imageFile", file.absolutePath)
        startActivity(intent)
        finish()
    }
}