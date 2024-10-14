package com.example.ecoscan

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout para este fragmento
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Obtener las referencias a los TextViews del perfil
        val fullNameTextView = view.findViewById<TextView>(R.id.fullNameTextView)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)

        // Obtener el correo electrónico del usuario actual
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val email = it.email
            val userId = it.uid

            // Obtener el nombre completo desde Firestore usando el userId
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null) {
                    // Obtener y mostrar el nombre completo
                    val fullName = document.getString("fullName")
                    fullNameTextView.text = fullName ?: "Nombre no disponible"

                    // Generar el username a partir del email
                    if (email != null) {
                        val username = email.substringBefore("@")
                        usernameTextView.text = "@$username"
                    }
                }
            }.addOnFailureListener {
                // Manejo de errores, en caso de que no se pueda obtener el documento
                fullNameTextView.text = "Nombre no disponible"
                usernameTextView.text = "Username no disponible"
            }
        }

        return view // Devolver la vista inflada
    }
}