package com.example.ecoscan.com.example.ecoscan

import android.content.Context
import android.content.res.AssetManager
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier(private val context: Context, modelPath: String) {
    private var interpreter: Interpreter
    private val inputShape: IntArray
    private val outputShape: IntArray

    init {
        // Usamos el context para obtener el AssetManager
        val model = loadModelFile(context.assets, modelPath)
        interpreter = Interpreter(model)
        inputShape = interpreter.getInputTensor(0).shape()
        outputShape = interpreter.getOutputTensor(0).shape()
    }

    data class Classification(
        val index: Int,
        val score: Float
    )

    fun classify(image: TensorImage): List<Classification> {
        val outputBuffer = ByteBuffer.allocateDirect(outputShape[1] * 4)
            .order(ByteOrder.nativeOrder())

        interpreter.run(image.buffer, outputBuffer)

        val scores = FloatArray(outputShape[1])
        outputBuffer.rewind()
        outputBuffer.asFloatBuffer().get(scores)

        return scores.mapIndexed { index, score ->
            Classification(index, score)
        }
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter.close()
    }

}