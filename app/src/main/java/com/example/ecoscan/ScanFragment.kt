package com.example.ecoscan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanFragment : Fragment() {

    private val MODEL_PATH = "model_unquant.tflite"
    private lateinit var tflite: Interpreter
    private var outputSize: Int = 8
    private lateinit var previewView: PreviewView
    private lateinit var scanButton: Button
    private lateinit var galleryButton: ImageButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var imageCapture: ImageCapture
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 101
    private var useFrontCamera = false

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewView = view.findViewById(R.id.previewView)
        scanButton = view.findViewById(R.id.Escanear)
        galleryButton = view.findViewById(R.id.galleryButton)
        switchCameraButton = view.findViewById(R.id.switchCameraButton)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        // Mostrar el diálogo de bienvenida (siempre que se carga el fragmento)
        showDialogMessage()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            startCamera()
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST_CODE)
        }

        try {
            tflite = Interpreter(loadModelFile())
        } catch (e: IOException) {
            Log.e("ScanFragment", "Error al cargar el modelo: ${e.message}")
            Toast.makeText(requireContext(), "Error al cargar el modelo", Toast.LENGTH_SHORT).show()
        }

        scanButton.setOnClickListener {
            captureImage()
        }

        galleryButton.setOnClickListener {
            openGallery()
        }

        switchCameraButton.setOnClickListener {
            useFrontCamera = !useFrontCamera
            startCamera()
        }
    }

    private fun showDialogMessage() {
        // Inflar el layout 'fragment_scan_dialog.xml'
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_scan_dialog, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)  // Para que no se pueda cerrar sin interacción
            .create()

        // Configurar el botón de cierre dentro del layout
        val closeButton = dialogView.findViewById<Button>(R.id.okButton)  // Suponiendo que el botón tiene id "closeButton"
        closeButton.setOnClickListener {
            dialog.dismiss()  // Cerrar el cuadro de diálogo cuando el usuario haga clic en el botón
        }

        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GALLERY_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ScanFragment", "Permiso de galería concedido")
                } else {
                    Toast.makeText(requireContext(), "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage: Uri? = data.data
            selectedImage?.let {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                    handleImage(bitmap)
                } catch (e: IOException) {
                    Log.e("ScanFragment", "Error al cargar la imagen: ${e.message}")
                    Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = requireContext().assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun runModel(inputData: ByteBuffer): FloatArray {
        val output = Array(1) { FloatArray(outputSize) }
        tflite.run(inputData, output)
        return output[0]
    }

    private fun handleImage(bitmap: Bitmap) {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val inputData = convertBitmapToByteBuffer(resizedBitmap)

        try {
            val outputData = runModel(inputData)
            val labels = loadLabels()
            val predictedLabel = getPredictedLabel(outputData, labels)

            showResultPopup(predictedLabel)
            saveImageToFirebase(bitmap, predictedLabel)

        } catch (e: Exception) {
            Log.e("ScanFragment", "Error en la inferencia: ${e.message}")
            Toast.makeText(requireContext(), "Error en la inferencia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.rewind()

        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            byteBuffer.putFloat((pixelValue shr 16 and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue shr 8 and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }

        return byteBuffer
    }

    private fun loadLabels(): List<String> {
        val labels = mutableListOf<String>()
        requireContext().assets.open("labels.txt").bufferedReader().useLines { lines ->
            lines.forEach { labels.add(it) }
        }
        return labels
    }

    private fun getPredictedLabel(output: FloatArray, labels: List<String>): String {
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: -1
        return if (maxIndex != -1) labels[maxIndex] else "Unknown"
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImage() {
        val photoFile = File(requireContext().externalMediaDirs.first(), "photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.path)
                handleImage(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("ScanFragment", "Error al capturar la imagen: ${exception.message}")
                Toast.makeText(requireContext(), "Error al capturar la imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showResultPopup(predictedLabel: String) {
        val (objectLabel, resultTextMessage) = when (predictedLabel) {
            "2 organic" -> "Objeto: Orgánico" to "Residuo aprovechable - contenedor verde"
            "3 trash" -> "Objeto: Basura" to "Residuo no aprovechable - contenedor negro"
            "4 glass" -> "Objeto: Vidrio" to "Residuo aprovechable - contenedor blanco"
            "5 paper" -> "Objeto: Papel" to "Residuo aprovechable - contenedor blanco"
            "6 plastic" -> "Objeto: Plástico" to "Residuo aprovechable - contenedor blanco"
            "7 cardboard" -> "Objeto: Cartón" to "Residuo aprovechable - contenedor blanco"
            else -> "Desconocido" to "Lo sentimos, no fue posible hacer el análisis. Por favor vuelve a intentarlo."
        }

        // Inflar el layout 'dialog_result.xml' y mostrarlo con los resultados del escaneo
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_result, null)

        val resultTextView = dialogView.findViewById<TextView>(R.id.messageText)
        resultTextView.text = "$objectLabel\n$resultTextMessage"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)  // Para que no pueda cerrarse sin interacción
            .create()

        // Configurar el botón de cierre dentro de 'dialog_result.xml'
        val closeButton = dialogView.findViewById<Button>(R.id.okButton)  // Suponiendo que el botón tiene id "closeButton"
        closeButton.setOnClickListener {
            dialog.dismiss()  // Cerrar el cuadro de diálogo cuando el usuario hace clic en el botón
        }

        dialog.show()
    }

    private fun saveImageToFirebase(bitmap: Bitmap, predictedLabel: String) {
        val (objectLabel, resultTextMessage) = when (predictedLabel) {
            "2 organic" -> "Objeto: Orgánico" to "Residuo aprovechable - contenedor verde"
            "3 trash" -> "Objeto: Basura" to "Residuo no aprovechable - contenedor negro"
            "4 glass" -> "Objeto: Vidrio" to "Residuo aprovechable - contenedor blanco"
            "5 paper" -> "Objeto: Papel" to "Residuo aprovechable - contenedor blanco"
            "6 plastic" -> "Objeto: Plástico" to "Residuo aprovechable - contenedor blanco"
            "7 cardboard" -> "Objeto: Cartón" to "Residuo aprovechable - contenedor blanco"
            else -> "Desconocido" to "Lo sentimos, no fue posible hacer el análisis. Por favor vuelve a intentarlo."
        }

        val user = auth.currentUser
        if (user == null) {
            Log.e("SaveImage", "Usuario no autenticado")
            return
        }

        val folderName = user.email?.replace(".", "_") ?: "Anonymous"
        val storageRef = storage.reference.child("Analyzed_images/$folderName/${System.currentTimeMillis()}.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            // Una vez que la imagen se sube con éxito, obtenemos la URL pública
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()  // URL pública accesible por HTTP
                saveToDatabase(imageUrl, user.email ?: "Usuario Anónimo", objectLabel, resultTextMessage)
            }.addOnFailureListener { exception ->
                Log.e("ScanFragment", "Error al obtener la URL pública: ${exception.message}")
                Toast.makeText(requireContext(), "Error al obtener la URL pública: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Log.e("ScanFragment", "Error al subir la imagen: ${exception.message}")
            Toast.makeText(requireContext(), "Error al subir la imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToDatabase(imageUrl: String, email: String, objectLabel: String, resultTextMessage: String) {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("history").child(email.replace(".", "_"))

        val currentDate = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val analysisEntry = mapOf(
            "date" to currentDate,
            "imageUrl" to imageUrl,
            "objectLabel" to objectLabel,
            "resultTextMessage" to resultTextMessage
        )

        ref.push().setValue(analysisEntry).addOnSuccessListener {
            Log.d("ScanFragment", "Historial guardado en la base de datos")
        }.addOnFailureListener { exception ->
            Log.e("ScanFragment", "Error al guardar el historial: ${exception.message}")
            Toast.makeText(requireContext(), "Error al guardar el historial: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    data class ScanHistory(
    val imageUrl: String,
    val email: String,
    val objectLabel: String,
    val resultTextMessage: String,
    val timestamp: String
)
}