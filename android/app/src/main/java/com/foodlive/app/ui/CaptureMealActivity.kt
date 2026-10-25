package com.foodlive.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.foodlive.app.data.repository.MealRepository
import com.foodlive.app.databinding.ActivityCaptureMealBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CaptureMealActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaptureMealBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val repository = MealRepository()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera & Location permissions are required for Foodlive", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureMealBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        binding.btnCapture.setOnClickListener {
            captureAndPublishMeal()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to bind camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingPermission")
    private fun captureAndPublishMeal() {
        val capture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        binding.btnCapture.isEnabled = false
        Toast.makeText(this, "Capturing meal...", Toast.LENGTH_SHORT).show()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                uploadMeal(photoFile, location.latitude, location.longitude)
                            } else {
                                binding.btnCapture.isEnabled = true
                                Toast.makeText(this@CaptureMealActivity, "Could not acquire GPS position", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            binding.btnCapture.isEnabled = true
                            Toast.makeText(this@CaptureMealActivity, "Location lookup failed", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.btnCapture.isEnabled = true
                    Toast.makeText(this@CaptureMealActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun uploadMeal(file: File, lat: Double, lng: Double) {
        val token = "YOUR_JWT_AUTH_TOKEN" // Feed from session/DataStore
        val mealType = "lunch"             // Replace with active selection toggle
        val spotName = "Street Stall #12"  // Replace with user input or reverse-geocoded place

        lifecycleScope.launch {
            val result = repository.uploadAndCreateMeal(token, file, mealType, spotName, lat, lng)
            binding.btnCapture.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@CaptureMealActivity, "Meal posted live!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(this@CaptureMealActivity, "Upload failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val outputDirectory: File
        get() {
            val mediaDir = externalMediaDirs.firstOrNull()?.let {
                File(it, "Foodlive").apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
        }

    private fun allPermissionsGranted() = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}