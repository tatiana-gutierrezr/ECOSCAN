package com.example.ecoscan

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@Suppress("UNCHECKED_CAST")
class ScanFragment : Fragment() {

    private var isUsingBackCamera: Boolean = true
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var interpreter: Interpreter  // TensorFlow Lite Interpreter

    // Cambia la constante a la última versión
    private val requestcodepermissions = 10
    private val requiredpermissions = arrayOf(Manifest.permission.CAMERA)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        previewView = view.findViewById(R.id.previewView)

        // Verificar y solicitar permisos de cámara
        if (allPermissionsGranted()) {
            startCamera(isUsingBackCamera)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), requiredpermissions, requestcodepermissions)
        }

        // Cargar el modelo de TensorFlow Lite
        loadModel()

        // Botón para cambiar de cámara
        val switchCameraButton: ImageButton = view.findViewById(R.id.switchCameraButton)
        switchCameraButton.setOnClickListener {
            isUsingBackCamera = !isUsingBackCamera
            startCamera(isUsingBackCamera)
        }

        // Botón para capturar la imagen y hacer la predicción
        val scanButton: Button = view.findViewById(R.id.Escanear)
        scanButton.setOnClickListener {
            takePhoto()
        }

        return view
    }

    // Método para verificar permisos
    private fun allPermissionsGranted() = requiredpermissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera(isBackCamera: Boolean) {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(requireContext()) as ListenableFuture<ProcessCameraProvider>

        // Usar addListener para manejar el resultado de la obtención del cameraProvider
        cameraProviderFuture.addListener({
            try {
                // Obtener el cameraProvider
                val cameraProvider = cameraProviderFuture.get()  // No es necesario hacer un casting aquí

                // Definir el selector de cámara (frontal o trasera)
                val cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                // Configurar la vista previa
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)  // Cambia fragment_scan por previewView
                }

                // Configurar la captura de imágenes
                imageCapture = ImageCapture.Builder().build()

                // Unir las cámaras
                cameraProvider.unbindAll()  // Desvincular cualquier uso anterior de cámara

                // Vincular la cámara a la vista previa y captura de imágenes
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e("CameraX", "Error al vincular la cámara", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxy.toBitmap()
                    imageProxy.close()  // Liberar recursos
                    runModel(bitmap)  // Ejecutar el modelo en la imagen capturada
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Error capturando imagen", exception)
                }
            }
        )
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    // Función para cargar el modelo desde assets
    private fun loadModel() {
        try {
            val assetManager: AssetManager = requireContext().assets
            val fileDescriptor: AssetFileDescriptor = assetManager.openFd("model.tflite")

            // Usar FileInputStream para obtener el canal
            val inputStream: FileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel: FileChannel = inputStream.channel
            val startOffset: Long = fileDescriptor.startOffset
            val declaredLength: Long = fileDescriptor.declaredLength

            // Mapear el archivo del modelo en la memoria
            val mappedBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(mappedBuffer)

            inputStream.close() // Cerrar el InputStream después de usarlo
        } catch (e: Exception) {
            Log.e("TensorFlowLite", "Error al cargar el modelo", e)
        }
    }

    // Preprocesar la imagen (convertir a ByteBuffer)
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val inputSize = 224  // Ajusta esto según el tamaño de entrada de tu modelo
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // Crear el ByteBuffer para almacenar los valores en formato UINT8
        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in intValues) {
            val r = pixel shr 16 and 0xFF
            val g = pixel shr 8 and 0xFF
            val b = pixel and 0xFF

            // Convertir a tipo UINT8
            inputBuffer.put((r and 0xFF).toByte())
            inputBuffer.put((g and 0xFF).toByte())
            inputBuffer.put((b and 0xFF).toByte())
        }

        return inputBuffer
    }

    // Ejecutar el modelo en la imagen capturada
    private fun runModel(bitmap: Bitmap) {
        val inputBuffer = preprocessImage(bitmap)

        // Define la salida según el número de clases del modelo
        val outputArray = Array(1) { FloatArray(3) }  // Ajusta según el número de clases

        // Ejecutar el modelo
        interpreter.run(inputBuffer, outputArray)

        // Obtener el índice con mayor confianza
        val maxIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] } ?: -1

        // Clasificar y mostrar el resultado
        showResultPopup(maxIndex)
    }

    // Mostrar el resultado en un pop-up
    private fun showResultPopup(classIndex: Int) {
        val message: String = when (classIndex) {
            0 -> "Plástico - Contenedor Blanco (residuos aprovechables)"
            1 -> "Papel - Contenedor Blanco (residuos aprovechables)"
            2 -> "Cartón - Contenedor Blanco (residuos aprovechables)"
            3 -> "Aluminio - Contenedor Blanco (residuos aprovechables)"
            4 -> "Vidrio - Contenedor Blanco (residuos aprovechables)"
            5 -> "Orgánico - Contenedor Verde (residuos orgánicos aprovechables)"
            6 -> "Basura - Contenedor Negro (residuos no aprovechables)"
            else -> "No fue posible analizar el objeto, por favor vuelva a intentarlo"
        }

        // Crear el pop-up
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()  // Cerrar el pop-up al hacer clic en Aceptar
            }

        val alert = dialogBuilder.create()
        alert.setTitle("Resultado del Escaneo")
        alert.show()
    }
}