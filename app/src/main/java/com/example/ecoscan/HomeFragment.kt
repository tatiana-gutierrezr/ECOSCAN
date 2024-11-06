package com.example.ecoscan

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnInfo = view.findViewById<Button>(R.id.btnInfo)
        val btnUbicacion = view.findViewById<Button>(R.id.btnUbicacion)
        val btnHistorial = view.findViewById<Button>(R.id.btnHistorial)

        btnInfo.setOnClickListener {
            replaceFragment(InfoFragment())  // Reemplaza el fragmento actual por InfoFragment
        }

        btnUbicacion.setOnClickListener {
            replaceFragment(MapsFragment())  // Reemplaza el fragmento actual por MapsFragment
        }

        btnHistorial.setOnClickListener {
            checkUserStatusAndLoadHistoryFragment()  // Llama a la función para cargar el fragmento adecuado según el estado del usuario
        }
    }

    // Función para verificar el estado del usuario y cargar el fragmento adecuado
    private fun checkUserStatusAndLoadHistoryFragment() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null && !currentUser.isAnonymous) {
            // Usuario autenticado, cargar el fragmento HistoryFragment
            replaceFragment(HistoryFragment())
        } else {
            // Usuario no autenticado o anónimo, cargar el fragmento AnonProfileFragment
            replaceFragment(AnonProfileFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameContainer, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}