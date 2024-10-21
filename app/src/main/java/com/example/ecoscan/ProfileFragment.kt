package com.example.ecoscan

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.bumptech.glide.Glide

class ProfileFragment : Fragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout para este fragmento
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Obtener las referencias a los TextViews y al ImageView del perfil
        val fullNameTextView = view.findViewById<TextView>(R.id.fullnameTextView)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val btnCerrarSesion = view.findViewById<Button>(R.id.btnCerrarSesion) // Referencia al botón
        val profileImageView = view.findViewById<ImageView>(R.id.ImageView) // Referencia al ImageView

        // Obtener el correo electrónico del usuario actual
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            val userId = it.uid

            // Obtener los datos del usuario desde Firebase Realtime Database usando el userId
            val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/$userId")
            database.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Obtener y mostrar el nombre completo
                    val fullName = snapshot.child("fullName").getValue(String::class.java)
                    fullNameTextView.text = fullName ?: "Nombre no disponible"

                    // Obtener el nombre de usuario desde la base de datos
                    val username = snapshot.child("username").getValue(String::class.java)
                    usernameTextView.text = if (username != null) "@$username" else "Username no disponible"

                    // Cargar la imagen de perfil desde Firebase Storage usando la URL almacenada
                    val profileImageUrl = snapshot.child("imageUrl").getValue(String::class.java) // Cambiado a "imageUrl"
                    if (!profileImageUrl.isNullOrEmpty()) {
                        // Usar Glide para cargar la imagen
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.profile_pic) // Imagen de placeholder
                            .error(R.drawable.profile_pic) // Imagen de error si falla la carga
                            .into(profileImageView)
                    } else {
                        // Manejo si la URL de la imagen no está disponible
                        profileImageView.setImageResource(R.drawable.profile_pic) // Mostrar imagen de placeholder
                    }
                } else {
                    // Manejo si el usuario no existe
                    fullNameTextView.text = "Usuario no encontrado"
                    usernameTextView.text = "Username no disponible"
                    profileImageView.setImageResource(R.drawable.profile_pic) // Mostrar imagen de placeholder
                }
            }.addOnFailureListener {
                // Manejo de errores, en caso de que no se pueda obtener el documento
                fullNameTextView.text = "Error al cargar el nombre"
                usernameTextView.text = "Error al cargar el username"
                profileImageView.setImageResource(R.drawable.profile_pic) // Mostrar imagen de placeholder
            }
        }

        // Lógica para cerrar sesión
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Cerrar sesión
            // Intentar pasar a MainActivity
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            activity?.finish() // Opcional: finaliza la actividad actual si no deseas volver a ella
        }

        return view // Devolver la vista inflada
    }
}