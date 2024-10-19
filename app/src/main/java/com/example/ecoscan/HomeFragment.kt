package com.example.ecoscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.content.res.ResourcesCompat // Asegúrate de que este import esté presente
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias a los botones
        val btnInfo = view.findViewById<Button>(R.id.btnInfo)
        val btnUbicacion = view.findViewById<Button>(R.id.btnUbicacion)
        val btnHistorial = view.findViewById<Button>(R.id.btnHistorial)

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
}