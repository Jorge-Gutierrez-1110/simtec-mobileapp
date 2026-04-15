package com.example.simtec_mobileapp

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.BitmapFactory
import android.util.Base64

class CameraActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOCATION = "extra_location"
        const val RESULT_LOCATION = "result_location"
        const val RESULT_PHOTO = "result_photo"
    }

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetectionManager: FaceDetectionManager
    private lateinit var sessionManager: SessionManager

    private var currentFrame: Bitmap? = null
    private var lastDetectionTime = 0L
    private var isProcessing = false

    private lateinit var tvFaceStatus: TextView
    private lateinit var tvConfidence: TextView
    private lateinit var btnConfirm: Button

    private var lastDetectedFace: FaceDetectionManager.FaceTemplate? = null
    private var confirmationCount = 0
    private var currentLocation: String = ""
    private var capturedPhotoBase64: String? = null

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        tvFaceStatus = findViewById(R.id.tvFaceStatus)
        tvConfidence = findViewById(R.id.tvConfidence)
        btnConfirm = findViewById(R.id.btnConfirm)

        currentLocation = intent.getStringExtra(EXTRA_LOCATION) ?: ""

        faceDetectionManager = FaceDetectionManager()
        sessionManager = SessionManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        btnConfirm.setOnClickListener {
            if (lastDetectedFace != null && confirmationCount >= 3) {
                confirmAndExit()
            } else {
                Toast.makeText(
                    this,
                    "Por favor, espera a que la cara sea verificada",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Configuramos ImageAnalysis para detección de cara en tiempo real
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        detectFaceInFrame(imageProxy)
                    }
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture
                )

            } catch (e: Exception) {
                Log.e("CameraActivity", "Error iniciando cámara: ${e.message}")
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Analiza cada frame para detectar caras
     */
    @OptIn(ExperimentalGetImage::class)
    private fun detectFaceInFrame(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDetectionTime < 500) { // Procesar cada 500ms para no sobrecargar
            imageProxy.close()
            return
        }

        isProcessing = true
        lastDetectionTime = currentTime

        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                val detector = FaceDetection.getClient(
                    FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build()
                )

                detector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        handleDetectedFaces(faces)
                    }
                    .addOnFailureListener { e ->
                        Log.e("CameraActivity", "Error en detección: ${e.message}")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                        isProcessing = false
                    }
            } else {
                imageProxy.close()
                isProcessing = false
            }
        } catch (e: Exception) {
            Log.e("CameraActivity", "Error procesando frame: ${e.message}")
            imageProxy.close()
            isProcessing = false
        }
    }

    /**
     * Procesa las caras detectadas en un frame
     */
    private fun handleDetectedFaces(faces: List<com.google.mlkit.vision.face.Face>) {
        runOnUiThread {
            if (faces.isEmpty()) {
                tvFaceStatus.text = "No se detectó cara"
                tvFaceStatus.setTextColor(getColor(android.R.color.holo_red_light))
                tvConfidence.text = "Confianza: 0%"
                confirmationCount = 0
                return@runOnUiThread
            }

            val detectedFace = faces.first()
            val currentTemplate = faceDetectionManager.extractFaceGeometry(detectedFace)

            // Verificamos si el usuario ya tiene un template guardado
            val savedTemplate = sessionManager.getFaceTemplate()

            if (savedTemplate == null) {
                // Primera vez: guardar el template
                sessionManager.saveFaceTemplate(currentTemplate)
                lastDetectedFace = currentTemplate
                tvFaceStatus.text = "Cara registrada. Confirma para continuar."
                tvFaceStatus.setTextColor(getColor(android.R.color.holo_green_light))
                tvConfidence.text = "Confianza: 100%"
                confirmationCount++
            } else {
                // Comparar con template guardado
                val confidence = faceDetectionManager.compareFaces(savedTemplate, currentTemplate)
                lastDetectedFace = currentTemplate

                val percentConfidence = (confidence * 100).toInt()
                tvConfidence.text = "Confianza: $percentConfidence%"

                when {
                    confidence >= 0.65f -> {
                        tvFaceStatus.text = "✓ Cara identificada correctamente"
                        tvFaceStatus.setTextColor(getColor(android.R.color.holo_green_light))
                        confirmationCount++
                    }
                    confidence >= 0.50f -> {
                        tvFaceStatus.text = "~ Cara similar, pero no es seguro"
                        tvFaceStatus.setTextColor(getColor(android.R.color.holo_orange_light))
                        confirmationCount = maxOf(0, confirmationCount - 1)
                    }
                    else -> {
                        tvFaceStatus.text = "✗ Cara no reconocida"
                        tvFaceStatus.setTextColor(getColor(android.R.color.holo_red_light))
                        confirmationCount = 0
                    }
                }

                // Si lleva 4 detecciones positivas, permitimos confirmar
                if (confirmationCount >= 4) {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Confirmar (Listo)"
                } else {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Confirmar (${confirmationCount}/4)"
                }
            }
        }
    }

    /**
     * Confirma la identidad y retorna al HomeActivity
     */
    private fun confirmAndExit() {
        val imageCapture = this.imageCapture
        if (imageCapture == null) {
            // No hay cámara, salir sin foto
            val returnIntent = intent
            returnIntent.putExtra(RESULT_LOCATION, currentLocation)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
            return
        }

        // Capturar foto SINCRÓNICAMENTE antes de salir
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()

                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 480, 640, true)
                        val outputStream = java.io.ByteArrayOutputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        capturedPhotoBase64 = "data:image/jpeg;base64,$base64"
                        Log.d("CameraActivity", "Foto capturada: ${capturedPhotoBase64?.length} caracteres")
                    }

                    // Ahora sí, retornar al HomeActivity
                    val returnIntent = intent
                    returnIntent.putExtra(RESULT_LOCATION, currentLocation)
                    capturedPhotoBase64?.let { returnIntent.putExtra(RESULT_PHOTO, it) }
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraActivity", "Error capturando foto: ${exception.message}")
                    // Salir sin foto aunque falle
                    val returnIntent = intent
                    returnIntent.putExtra(RESULT_LOCATION, currentLocation)
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
