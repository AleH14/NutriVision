package com.example.nutrivision

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nutrivision.data.network.RetrofitClient
import com.example.nutrivision.data.repository.NutriRepository
import com.example.nutrivision.data.model.FoodAnalysisResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CamaraActivity : AppCompatActivity() {

    private val TAG = "CamaraActivity"
    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private lateinit var repository: NutriRepository
    private var isCapturing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camara)

        repository = NutriRepository(RetrofitClient.instance)

        // Setup navigation
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

        // Back button
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)
        btnVolver.setOnClickListener {
            overridePendingTransition(0, 0)
            finish()
        }

        // Capture button
        val btnCapturar = findViewById<ImageButton>(R.id.btnTomarFoto)
        btnCapturar.setOnClickListener {
            if (!isCapturing) {
                captureImage()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                Log.d(TAG, "Cámara iniciada correctamente")
            } catch (exc: Exception) {
                Log.e(TAG, "Error al iniciar cámara", exc)
                Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        isCapturing = true

        val outputFile = createImageFile()
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Imagen capturada: ${outputFile.absolutePath}")
                    analizarImagen(outputFile)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Error al capturar imagen", exc)
                    isCapturing = false
                    Toast.makeText(this@CamaraActivity, "Error al capturar imagen", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = cacheDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun analizarImagen(imageFile: File) {
        Log.d(TAG, "Archivo: ${imageFile.name}, tamaño: ${imageFile.length()} bytes")
        Log.d(TAG, "Existe: ${imageFile.exists()}")
        
        if (!imageFile.exists()) {
            Log.e(TAG, "Archivo no existe")
            Toast.makeText(this@CamaraActivity, "Error: archivo no se guardó correctamente", Toast.LENGTH_SHORT).show()
            isCapturing = false
            return
        }
        
        // Navegar a AnalisisActivity inmediatamente con la foto
        // AnalisisActivity hará el análisis en segundo plano
        Log.d(TAG, "Navegando a AnalisisActivity...")
        navigateToAnalysis(imageFile)
    }

    private fun navigateToAnalysis(imageFile: File) {
        val intent = Intent(this, AnalisisActivity::class.java).apply {
            putExtra("imageFile", imageFile.absolutePath)
        }
        startActivity(intent)
        finish()
    }
}
