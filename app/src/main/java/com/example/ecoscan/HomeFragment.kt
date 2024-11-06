package com.example.ecoscan

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

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
            replaceFragment(AnonProfileFragment()) // Fragmento para usuarios sin cuenta
        }
    }

    // Función para reemplazar el fragmento
    private fun replaceFragment(fragment: Fragment) {
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameContainer, fragment) // Asegúrate de que 'frameContainer' esté en tu layout
        transaction.addToBackStack(null)  // Opcional: añade el fragmento a la pila de retroceso para poder regresar
        transaction.commit()
    }
}