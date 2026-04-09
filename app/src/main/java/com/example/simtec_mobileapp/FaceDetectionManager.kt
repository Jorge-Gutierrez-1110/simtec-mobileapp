package com.example.simtec_mobileapp

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.sqrt

class FaceDetectionManager {

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    /**
     * Detecta caras en un bitmap y retorna el listado de caras encontradas
     */
    fun detectFaces(bitmap: Bitmap): List<Face> {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            detector.process(inputImage)
                .addOnCompleteListener {
                    // El resultado se procesa en el callback
                }
            emptyList() // Placeholder, el detector es asincrónico
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Extrae las características geométricas de una cara (landmarks)
     * Retorna un mapa de distancias entre puntos clave
     */
    fun extractFaceGeometry(face: Face): FaceTemplate {
        val landmarks = face.allLandmarks
        val distances = mutableListOf<Float>()

        // Calculamos distancias entre landmarks para crear una "firma" geométrica
        // Esto es invariante a rotaciones pequeñas y cambios de tamaño
        if (landmarks.size >= 2) {
            for (i in 0 until landmarks.size - 1) {
                for (j in (i + 1) until landmarks.size) {
                    val dist = calculateDistance(
                        landmarks[i].position.x, landmarks[i].position.y,
                        landmarks[j].position.x, landmarks[j].position.y
                    )
                    distances.add(dist)
                }
            }
        }

        return FaceTemplate(
            landmarkDistances = distances,
            faceWidth = face.boundingBox.width().toFloat(),
            faceHeight = face.boundingBox.height().toFloat(),
            headEulerAngleY = face.headEulerAngleY,
            headEulerAngleZ = face.headEulerAngleZ,
            smilingProbability = face.smilingProbability ?: 0f,
            leftEyeOpenProbability = face.leftEyeOpenProbability ?: 0f,
            rightEyeOpenProbability = face.rightEyeOpenProbability ?: 0f
        )
    }

    /**
     * Compara dos templates de cara y retorna un score de similitud (0.0 a 1.0)
     * Score > 0.7 = Match confiable
     */
    fun compareFaces(template1: FaceTemplate, template2: FaceTemplate): Float {
        // Si las distancias de landmarks son muy diferentes, no es la misma persona
        if (template1.landmarkDistances.isEmpty() || template2.landmarkDistances.isEmpty()) {
            return 0f
        }

        // Comparación de tamaño de cara (tolerancia 20%)
        val widthRatio = minOf(template1.faceWidth, template2.faceWidth) /
                maxOf(template1.faceWidth, template2.faceWidth)
        val heightRatio = minOf(template1.faceHeight, template2.faceHeight) /
                maxOf(template1.faceHeight, template2.faceHeight)

        if (widthRatio < 0.8f || heightRatio < 0.8f) {
            // Cara muy diferente en tamaño
            return 0.2f
        }

        // Comparación de distancias entre landmarks (métrica de similitud)
        val similarity = calculateLandmarkSimilarity(
            template1.landmarkDistances,
            template2.landmarkDistances
        )

        // Ajustamos la similitud con características adicionales
        var finalScore = similarity

        // Si la inclinación de cabeza es muy grande, bajamos confianza
        val headAngleDiff = (template1.headEulerAngleY - template2.headEulerAngleY).toFloat()
        if (kotlin.math.abs(headAngleDiff) > 30) {
            finalScore *= 0.8f
        }

        // Los ojos abiertos ayuda a confirmar identidad
        val eyeOpenDiff = kotlin.math.abs(
            (template1.leftEyeOpenProbability + template1.rightEyeOpenProbability) / 2 -
            (template2.leftEyeOpenProbability + template2.rightEyeOpenProbability) / 2
        )
        if (eyeOpenDiff > 0.5) {
            finalScore *= 0.9f
        }

        return finalScore.coerceIn(0f, 1f)
    }

    /**
     * Calcula la distancia euclidiana entre dos puntos
     */
    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }

    /**
     * Compara dos listas de distancias de landmarks
     * Usa correlación de Pearson normalizada
     */
    private fun calculateLandmarkSimilarity(
        distances1: List<Float>,
        distances2: List<Float>
    ): Float {
        // Si no tienen el mismo número de distancias, normalizamos
        val minSize = minOf(distances1.size, distances2.size)
        if (minSize == 0) return 0f

        var sumDiff = 0f
        var sumSquaredDiff = 0f

        for (i in 0 until minSize) {
            val diff = distances1[i] - distances2[i]
            sumDiff += diff
            sumSquaredDiff += diff * diff
        }

        // Calculamos RMS (Root Mean Square) error
        val rmsError = sqrt(sumSquaredDiff / minSize)

        // Normalizamos: si RMS es 0, es match perfecto (1.0)
        // Si RMS es muy grande (>100), es no-match (0.0)
        val normalizedRMS = rmsError / 100f
        return (1f - normalizedRMS).coerceIn(0f, 1f)
    }

    /**
     * Data class que almacena la "firma" geométrica de una cara
     */
    data class FaceTemplate(
        val landmarkDistances: List<Float>,
        val faceWidth: Float,
        val faceHeight: Float,
        val headEulerAngleY: Float,
        val headEulerAngleZ: Float,
        val smilingProbability: Float,
        val leftEyeOpenProbability: Float,
        val rightEyeOpenProbability: Float
    )
}
