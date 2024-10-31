package com.example.ecoscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.FragmentManager

class InfoFragment : Fragment() {

    private lateinit var btnAtras: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout para este fragmento
        val view = inflater.inflate(R.layout.fragment_info, container, false)
        btnAtras = view.findViewById(R.id.backarrow)

        // Llamar a la función para configurar los botones
        setupButtons(view)

        btnAtras.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return view
    }

    private fun resizeImage(resourceId: Int, height: Int): Bitmap {
        // Cargar la imagen original como Bitmap
        val originalBitmap = BitmapFactory.decodeResource(resources, resourceId)

        // Calcular el nuevo ancho manteniendo la relación de aspecto
        val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val newWidth = (height * aspectRatio).toInt()

        // Redimensionar la imagen
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, height, false)
    }

    private fun setupButtons(view: View) {
        val btnNino: Button = view.findViewById(R.id.btn_nino)
        val btnAdolescente: Button = view.findViewById(R.id.btn_adolescente)
        val btnAdulto: Button = view.findViewById(R.id.btn_adulto)

        // Establecer la altura deseada
        val desiredHeight = 300 // Cambia este valor a la altura que necesites

        // Redimensionar imágenes para cada botón manteniendo la relación de aspecto
        val ninoBitmap = resizeImage(R.drawable.menor, desiredHeight)
        val adolescenteBitmap = resizeImage(R.drawable.adolescente, desiredHeight)
        val adultoBitmap = resizeImage(R.drawable.adulto, desiredHeight)

        // Convertir Bitmap a Drawable
        val ninoDrawable = BitmapDrawable(resources, ninoBitmap)
        val adolescenteDrawable = BitmapDrawable(resources, adolescenteBitmap)
        val adultoDrawable = BitmapDrawable(resources, adultoBitmap)

        // Establecer las imágenes redimensionadas en los botones
        btnNino.setCompoundDrawablesWithIntrinsicBounds(ninoDrawable, null, null, null)
        btnAdolescente.setCompoundDrawablesWithIntrinsicBounds(adolescenteDrawable, null, null, null)
        btnAdulto.setCompoundDrawablesWithIntrinsicBounds(adultoDrawable, null, null, null)

        // Configurar el clic en los botones para abrir nuevos fragmentos
        btnNino.setOnClickListener {
            openFragment(InfoxEdadFragment())
        }

        btnAdolescente.setOnClickListener {
            openFragment(InfoxEdadFragment())
        }

        btnAdulto.setOnClickListener {
            openFragment(InfoxEdadFragment())
        }
    }

    private fun openFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = parentFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment) // Asegúrate de que R.id.fragment_container es el contenedor correcto
        transaction.addToBackStack(null) // Permite regresar al fragmento anterior
        transaction.commit()
    }
}