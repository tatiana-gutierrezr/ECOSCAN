package com.example.ecoscan

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("DEPRECATION")
class ScanFragment : Fragment() {

    private var isUsingBackCamera: Boolean = true
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>
    private lateinit var storageReference: StorageReference
    private var firebaseUser: FirebaseUser? = null

    private val REQUEST_CAMERA_PERMISSION = 200
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        firebaseUser = FirebaseAuth.getInstance().currentUser
        storageReference = FirebaseStorage.getInstance().reference

        previewView = view.findViewById(R.id.previewView)

        val switchCameraButton: ImageButton = view.findViewById(R.id.switchCameraButton)
        switchCameraButton.setOnClickListener { switchCamera() }

        val scanButton: Button = view.findViewById(R.id.Escanear)
        scanButton.setOnClickListener { captureImage() }

        // Inicializa el launcher para la galería
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                val imageUri: Uri? = result.data?.data
                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(imageUri!!))
                classifyImage(bitmap)
            }
        }

        val galleryButton: ImageButton = view.findViewById(R.id.galleryButton)
        galleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK)
            galleryIntent.type = "image/*"
            galleryLauncher.launch(galleryIntent)  // Usa el launcher
        }

        loadModelAndLabels()
        checkCameraPermissions()

        return view
    }

    private fun checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            startCamera()
        }
    }

    private fun switchCamera() {
        isUsingBackCamera = !isUsingBackCamera
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = if (isUsingBackCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            preview.setSurfaceProvider(previewView.surfaceProvider)

            try {
                cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraError", "Error al vincular la cámara: ${e.message}")
                Toast.makeText(requireContext(), "Error al iniciar la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImage() {
        val file = File(requireContext().externalCacheDir, "${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    classifyImage(bitmap)
                } catch (e: Exception) {
                    Log.e("CaptureImage", "Error al clasificar la imagen: ${e.message}")
                    Toast.makeText(requireContext(), "Error al clasificar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CaptureImage", "Error al capturar la imagen: ${exception.message}")
                Toast.makeText(requireContext(), "Error al capturar la imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        Log.d("TensorFlow", "Resized bitmap dimensions: ${resizedBitmap.width}x${resizedBitmap.height}")

        // Crear un ByteBuffer para FLOAT32 de 150528 bytes
        val floatBuffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4) // 602112 bytes para FLOAT32
        floatBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        for (pixelValue in intValues) {
            // Convertir a FLOAT32 directamente
            floatBuffer.putFloat((pixelValue shr 16 and 0xFF) / 255.0f) // Rojo
            floatBuffer.putFloat((pixelValue shr 8 and 0xFF) / 255.0f)  // Verde
            floatBuffer.putFloat((pixelValue and 0xFF) / 255.0f)        // Azul
        }

        floatBuffer.rewind()
        Log.d("TensorFlow", "Input Buffer Size: ${floatBuffer.capacity()}") // Debe ser 602112
        return floatBuffer
    }

    private fun classifyImage(bitmap: Bitmap) {
        try {
            // Obtener el buffer FLOAT32 directamente de la conversión
            val floatBuffer = convertBitmapToByteBuffer(bitmap)

            val outputMap = Array(1) { FloatArray(labels.size) }
            interpreter.run(floatBuffer, outputMap)

            val resultIndex = outputMap[0].indices.maxByOrNull { outputMap[0][it] } ?: -1
            showResultDialog(resultIndex)
        } catch (e: Exception) {
            Log.e("ClassifyImage", "Error durante la clasificación: ${e.message}")
            Toast.makeText(requireContext(), "Error durante la clasificación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResultDialog(resultIndex: Int) {
        val message: String = when (resultIndex) {
            0, 1 -> "Error: No fue posible analizar la imagen, inténtalo de nuevo."
            2, 3, 4, 5, 6 -> "Residuo aprovechable - Contenedor blanco."
            7 -> "Residuo no aprovechable - Contenedor negro."
            8 -> "Residuo orgánico aprovechable - Contenedor verde."
            else -> "Error desconocido."
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Resultado de la Clasificación")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun loadModelAndLabels() {
        try {
            interpreter = Interpreter(loadModelFile("model.tflite"))
            labels = loadLabels("labels.txt")
        } catch (e: Exception) {
            Log.e("ScanFragment", "Error loading model or labels", e)
            Toast.makeText(requireContext(), "Error al cargar el modelo o las etiquetas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadModelFile(modelFilename: String): MappedByteBuffer {
        val fileDescriptor = requireContext().assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(labelFilename: String): List<String> {
        return requireContext().assets.open(labelFilename).bufferedReader().use { it.readLines() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}