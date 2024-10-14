package com.example.ecoscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnInfo = view.findViewById<Button>(R.id.btnInfo)
        val btnUbicacion = view.findViewById<Button>(R.id.btnUbicacion)
        val btnHistorial = view.findViewById<Button>(R.id.btnHistorial)

        // Redimensionar y asignar iconos
        btnInfo.setCompoundDrawablesWithIntrinsicBounds(resizeDrawable(R.drawable.informacion, 390, 270), null, null, null)
        btnUbicacion.setCompoundDrawablesWithIntrinsicBounds(resizeDrawable(R.drawable.pinlocacion, 263, 360), null, null, null)
        btnHistorial.setCompoundDrawablesWithIntrinsicBounds(resizeDrawable(R.drawable.historial, 210, 180), null, null, null)

        // Configurar los listeners de clic
        btnInfo.setOnClickListener {
            replaceFragment(InfoFragment())  // Reemplaza el fragmento actual por InfoFragment
        }

        btnUbicacion.setOnClickListener {
            replaceFragment(MapsFragment())  // Reemplaza el fragmento actual por LocationFragment
        }

        btnHistorial.setOnClickListener {
            replaceFragment(HistoryFragment())  // Reemplaza el fragmento actual por HistoryFragment
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameContainer, fragment) // Asegúrate de que 'frameContainer' esté en tu layout
        transaction.addToBackStack(null)  // Opcional: añade a la pila de retroceso
        transaction.commit()
    }

    private fun resizeDrawable(drawableId: Int, width: Int, height: Int): BitmapDrawable? {
        val bitmap = BitmapFactory.decodeResource(resources, drawableId)
        return if (bitmap != null) {
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            BitmapDrawable(resources, resizedBitmap)
        } else {
            Log.e("HomeFragment", "No se pudo cargar el recurso con id: $drawableId")
            null
        }
    }
}