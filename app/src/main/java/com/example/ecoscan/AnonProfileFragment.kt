package com.example.ecoscan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class AnonProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_anon_profile, container, false)

        // Referenciar los botones
        val btnIniciarSesion = view.findViewById<Button>(R.id.btnIniciarSesion)
        val btnCrearCuenta = view.findViewById<Button>(R.id.btnCrearCuenta)

        // Establecer los listeners para los botones
        btnIniciarSesion.setOnClickListener {
            // Abrir la primera actividad
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }

        btnCrearCuenta.setOnClickListener {
            // Abrir la segunda actividad
            val intent = Intent(activity, SignupActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
