package com.example.ecoscan

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ecoscan.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var model: Model

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

        startCamera()

        return view
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
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
                    val savedUri = Uri.fromFile(photoFile)
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    classifyImage(bitmap)
                }
            }
        )
    }

    private fun switchCamera() {
        // Implementar lógica para cambiar entre cámara frontal y trasera
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                classifyImage(bitmap)
            }
        }
    }

    private fun classifyImage(bitmap: Bitmap) {
        // Redimensionar el bitmap a 224x224
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // Crear un TensorBuffer con la forma correcta y tipo de datos FLOAT32
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val confidences = outputFeature0.floatArray
        val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: -1

        val labels = arrayOf("Vacio", "Persona", "Vidrio", "Plastico", "Papel", "Carton", "Aluminio", "Basura", "Organico")
        val result = labels[maxIndex]

        Toast.makeText(requireContext(), "Resultado: $result", Toast.LENGTH_SHORT).show()
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // Crear un ByteBuffer del tamaño correcto para FLOAT32
        val byteBuffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4) // 4 bytes por float
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val value = intValues[pixel++]
                // Normalizar los valores de los píxeles
                val r = (value shr 16 and 0xFF) / 255.0f
                val g = (value shr 8 and 0xFF) / 255.0f
                val b = (value and 0xFF) / 255.0f
                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }
        return byteBuffer
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_GALLERY = 1
    }
}