package com.example.ecoscan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog // Importar AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ecoscan.ml.Model
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class ScanFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var model: Model
    private var isUsingFrontCamera = false
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_scan, container, false)
        previewView = view.findViewById(R.id.previewView)
        val scanButton: Button = view.findViewById(R.id.Escanear)
        val switchCameraButton: ImageButton = view.findViewById(R.id.switchCameraButton)
        val galleryButton: ImageButton = view.findViewById(R.id.galleryButton)

        scanButton.setOnClickListener { takePhoto() }
        switchCameraButton.setOnClickListener { switchCamera() }
        galleryButton.setOnClickListener { openGallery() }

        model = Model.newInstance(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Revisar permisos antes de iniciar la cámara
        checkPermissions()

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                    classifyImage(bitmap, File(it.path!!))
                }
            }
        }

        return view
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = if (isUsingFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                Log.d("ScanFragment", "Cámara iniciada correctamente")
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val photoFile = File(
            requireContext().externalMediaDirs.firstOrNull(),
            "${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Error al tomar la foto: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    classifyImage(bitmap, photoFile)
                }
            }
        )
    }

    private fun classifyImage(bitmap: Bitmap, photoFile: File) {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val confidences = outputFeature0.floatArray
        val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: -1

        val labels = arrayOf("Vacio", "Persona", "Vidrio", "Plastico", "Papel", "Carton", "Aluminio", "Basura", "Organico")
        val result = labels[maxIndex]

        Log.d("ClassifyImage", "Confidences: ${confidences.joinToString()}")
        Log.d("ClassifyImage", "Max Index: $maxIndex")
        Log.d("ClassifyImage", "Result: $result")

        // Mostrar el diálogo con el resultado
        showResultDialog(result)

        val timestamp = System.currentTimeMillis()
        uploadImageToFirebase(photoFile, result, timestamp)
    }

    private fun showResultDialog(result: String) { // Nuevo método para mostrar el resultado
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Resultado de clasificación")
        builder.setMessage("El objeto clasificado es: $result")
        builder.setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }

        // Mostrar el diálogo
        val dialog = builder.create()
        dialog.show()
    }

    private fun uploadImageToFirebase(photoFile: File, result: String, timestamp: Long) {
        // Obtener el ID del usuario autenticado
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        // Obtener el nombre de usuario o usar "Anonymous" si no está autenticado
        val username = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous"

        // Definir el nombre de la carpeta en función de si el usuario está autenticado o no
        val userFolder = if (userId != null) {
            username // Carpeta con el nombre de usuario si está autenticado
        } else {
            "Anonymous" // Carpeta "Anonymous" si no está autenticado
        }

        // Crear referencia en Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("Analyzed images/$userFolder/${photoFile.name}")

        val uploadTask = imageRef.putFile(Uri.fromFile(photoFile))
        uploadTask.addOnSuccessListener {
            saveResultToDatabase(result, timestamp, username)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al subir la imagen: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveResultToDatabase(result: String, timestamp: Long, username: String?) {
        val databaseRef = FirebaseDatabase.getInstance().reference
        val resultData = hashMapOf(
            "result" to result,
            "timestamp" to timestamp
        )

        databaseRef.child("Analyzed images").child(username ?: "Anonymous").child(timestamp.toString()).setValue(resultData)
            .addOnSuccessListener {
                Log.d("Database", "Resultado guardado exitosamente")
            }
            .addOnFailureListener {
                Log.e("Database", "Error al guardar resultado: ${it.message}")
            }
    }

    private fun switchCamera() {
        isUsingFrontCamera = !isUsingFrontCamera
        startCamera()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(224 * 224 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val value = intValues[pixel++]
                byteBuffer.put((value shr 16 and 0xFF).toByte())
                byteBuffer.put((value shr 8 and 0xFF).toByte())
                byteBuffer.put((value and 0xFF).toByte())
            }
        }

        return byteBuffer
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
        cameraExecutor.shutdown()
    }
}