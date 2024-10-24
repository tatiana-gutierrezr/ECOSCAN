package com.example.ecoscan

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

        val fullNameTextView = view.findViewById<TextView>(R.id.fullnameTextView)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val btnCerrarSesion = view.findViewById<Button>(R.id.btnCerrarSesion)
        val profileImageView = view.findViewById<ImageView>(R.id.ImageView)
        val editNameButton = view.findViewById<ImageButton>(R.id.editName)
        val fullnameInput = view.findViewById<EditText>(R.id.fullnameInput)

        // Obtener el correo electrónico del usuario actual
        val currentUser = FirebaseAuth.getInstance().currentUser
        val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/${currentUser?.uid}") // Mover la inicialización aquí

        currentUser?.let {
            // Obtener los datos del usuario desde Firebase Realtime Database usando el userId
            database.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Obtener y mostrar el nombre completo
                    val fullName = snapshot.child("fullName").getValue(String::class.java)
                    fullNameTextView.text = fullName ?: "Nombre no disponible"

                    // Obtener el nombre de usuario desde la base de datos
                    val username = snapshot.child("username").getValue(String::class.java)
                    usernameTextView.text = if (username != null) "@$username" else "Username no disponible"

                    // Cargar la imagen de perfil desde Firebase Storage usando la URL almacenada
                    val profileImageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.profile_pic) // Imagen de placeholder
                            .error(R.drawable.profile_pic) // Imagen de error si falla la carga
                            .into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.profile_pic) // Mostrar imagen de placeholder
                    }
                } else {
                    fullNameTextView.text = "Usuario no encontrado"
                    usernameTextView.text = "Username no disponible"
                    profileImageView.setImageResource(R.drawable.profile_pic) // Mostrar imagen de placeholder
                }
            }.addOnFailureListener {
                fullNameTextView.text = "Error al cargar el nombre"
                usernameTextView.text = "Error al cargar el username"
                profileImageView.setImageResource(R.drawable.profile_pic) // Mostrar imagen de placeholder
            }
        }

        // Lógica para cerrar sesión
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Cerrar sesión
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            activity?.finish() // Opcional: finaliza la actividad actual si no deseas volver a ella
        }

        // Lógica para editar el nombre completo
        editNameButton.setOnClickListener {
            Log.d("ProfileFragment", "Edit button clicked")
            if (fullnameInput.visibility == View.GONE) {
                Log.d("ProfileFragment", "Showing fullname input")
                fullnameInput.setText(fullNameTextView.text)
                fullnameInput.visibility = View.VISIBLE
                fullNameTextView.visibility = View.GONE // Ocultar el TextView
            } else {
                // Si el EditText es visible, actualizar el nombre y ocultarlo
                val newFullName = fullnameInput.text.toString().trim()
                if (newFullName.isNotEmpty()) {
                    database.child("fullName").setValue(newFullName).addOnSuccessListener {
                        // Actualización exitosa
                        fullNameTextView.text = newFullName
                        fullnameInput.visibility = View.GONE
                        fullNameTextView.visibility = View.VISIBLE // Mostrar el TextView
                    }.addOnFailureListener {
                        // Error al actualizar
                        Toast.makeText(context, "Error al actualizar el nombre", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view // Devolver la vista inflada
    }
}