package com.example.ecoscan

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.ImageDecoder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ecoscan.com.example.ecoscan.Classifier
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.util.UUID
import com.example.ecoscan.databinding.FragmentScanBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ScanFragment : Fragment() {
    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProvider: ProcessCameraProvider
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private val labels = listOf(
        "Vacio", "Persona", "Vidrio", "Plastico", "Papel",
        "Carton", "Aluminio", "Basura", "Organico"
    )

    private val classifier by lazy {
        Classifier(requireContext(), "model.tflite")
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Se requieren permisos de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { processImageFromGallery(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.apply {
            Escanear.setOnClickListener { takePhoto() }
            switchCameraButton.setOnClickListener { switchCamera() }
            galleryButton.setOnClickListener { openGallery() }
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val rotation = binding.previewView.display.rotation

        preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(rotation)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            preview?.setSurfaceProvider(binding.previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Error al vincular casos de uso de cámara", e)
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processImageProxy(image)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Error al capturar foto", exception)
                }
            }
        )
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // Analizar con TFLite
        val result = classifyImage(resizedBitmap)

        // Subir a Firebase Storage
        uploadToFirebase(resizedBitmap) { downloadUrl ->
            // Mostrar resultado en un dialog
            showResultDialog(result, downloadUrl)
        }
    }

    private fun processImageFromGallery(uri: Uri) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true) // Convertir a ARGB_8888
        } else {
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true) // Convertir a ARGB_8888
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val result = classifyImage(resizedBitmap)

        // Aquí llamamos a uploadToFirebase solo con los dos argumentos requeridos
        uploadToFirebase(resizedBitmap) { downloadUrl ->
            showResultDialog(result, downloadUrl)
            // Guardar el resultado en Firestore después de obtener el downloadUrl
            val username = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous"
            saveAnalysisResult(username, result, downloadUrl)
        }
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        bindCameraUseCases()
    }

    private fun openGallery() {
        pickImage.launch("image/*")
    }

    private fun classifyImage(bitmap: Bitmap): String {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val results = classifier.classify(tensorImage)
        val maxResult = results.maxByOrNull { it.score }

        return when (maxResult?.index) {
            in 2..6 -> "Residuo aprovechable - Contenedor blanco"
            7 -> "Residuo no aprovechable - Contenedor negro"
            8 -> "Residuo orgánico aprovechable - Contenedor verde"
            else -> "Lo sentimos, la imagen no pudo ser analizada. Por favor vuelva a intentarlo"
        }.also {
            Log.d(TAG, "Clasificación: ${maxResult?.index}, Resultado: $it")
        }
    }

    private fun uploadToFirebase(bitmap: Bitmap, onComplete: (String) -> Unit) {
        val storage = Firebase.storage
        val storageRef = storage.reference

        // Obtén el usuario autenticado
        val user = FirebaseAuth.getInstance().currentUser

        // Usa el uid como nombre de carpeta si displayName es nulo
        val userFolder = user?.displayName ?: user?.uid ?: "Anonymous"

        // Ruta base para las imágenes analizadas
        val baseRef = storageRef.child("Analized_images/$userFolder")

        // Crea la referencia de la imagen con un ID único
        val imageRef = baseRef.child("${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        imageRef.putBytes(data)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onComplete(uri.toString())
                }
            }
            .addOnFailureListener {
                showResultDialog("Error al subir la imagen", null)
            }

        Log.d(TAG, "Usuario autenticado: ${user?.displayName ?: "Ninguno"}")
        Log.d(TAG, "Carpeta de usuario: $userFolder")
    }

    private fun showResultDialog(result: String, downloadUrl: String?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_result, null)

        val resultText = dialogView.findViewById<TextView>(R.id.resultText)
        val messageText = dialogView.findViewById<TextView>(R.id.messageText)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        // Actualizar el texto del resultado según la clasificación
        resultText.text = result

        // Aquí puedes incluir el URL de la imagen subida si es necesario
        if (downloadUrl != null) {
            messageText.text = "Imagen subida exitosamente a Firebase Storage\nURL: $downloadUrl"
        } else {
            messageText.text = "El análisis de la imagen resultó en: $result"
        }

        // Crear el AlertDialog con el diseño personalizado
        val dialog = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setView(dialogView)
            .create()

        // Acciones del botón OK
        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun saveAnalysisResult(username: String, result: String, imageUrl: String) {
        val firestore = FirebaseFirestore.getInstance()
        val analysisData = hashMapOf(
            "result" to result,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("analysis_results")
            .document(username)
            .collection("results")
            .add(analysisData)
            .addOnSuccessListener {
                Log.d(TAG, "Resultado guardado correctamente en Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar resultado en Firestore", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ScanFragment"
    }
}

// Extension function para convertir ImageProxy a Bitmap
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}