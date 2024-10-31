package com.example.ecoscan

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class ProfileFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var storageReference: StorageReference
    private val requestCodePickImage = 1
    private var username: String? = null

    @SuppressLint("MissingInflatedId", "CutPasteId", "SetTextI18n")
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
        profileImageView = view.findViewById<ImageView>(R.id.ImageView)
        val editNameButton = view.findViewById<ImageButton>(R.id.editName)
        val fullnameInput = view.findViewById<EditText>(R.id.fullnameTextView) // Asumiendo que fullnameTextView es un EditText

        // Inicializar Firebase Storage
        storageReference = FirebaseStorage.getInstance().reference

        // Obtener el correo electrónico del usuario actual
        val currentUser = FirebaseAuth.getInstance().currentUser
        val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/${currentUser?.uid}")

        currentUser?.let {
            // Obtener los datos del usuario desde Firebase Realtime Database usando el userId
            database.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Obtener y mostrar el nombre completo
                    val fullName = snapshot.child("fullName").getValue(String::class.java)
                    fullNameTextView.text = fullName ?: "Nombre no disponible"

                    // Obtener el nombre de usuario desde la base de datos
                    username = snapshot.child("username").getValue(String::class.java)
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
                fullnameInput.setText(fullNameTextView.text) // Cargar el nombre actual en el EditText
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
                        Toast.makeText(context, "Nombre actualizado exitosamente", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        // Error al actualizar
                        Toast.makeText(context, "Error al actualizar el nombre", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Lógica para mostrar el menú al hacer clic en la imagen de perfil
        profileImageView.setOnClickListener {
            showPopupMenu(it)
        }

        return view // Devolver la vista inflada
    }

    private fun showPopupMenu(view: View) {
        // Crear un PopupMenu
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)

        // Manejar el clic en los elementos del menú
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_view_image -> {
                    // Lógica para ver la imagen en pantalla completa
                    viewImageInFullScreen()
                    true
                }
                R.id.menu_change_image -> {
                    // Lógica para cambiar la imagen de perfil
                    changeProfileImage()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun viewImageInFullScreen() {
        // Implementa la lógica para abrir la imagen en pantalla completa
        val profileImageUrl = "url_de_tu_imagen" // Cambia esto por la URL de tu imagen
        val intent = Intent(requireContext(), FullScreenImageActivity::class.java)
        intent.putExtra("imageUrl", profileImageUrl)
        startActivity(intent)
    }

    private fun changeProfileImage() {
        // Llama a un Intent para seleccionar una imagen de la galería
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, requestCodePickImage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodePickImage && resultCode == AppCompatActivity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            // Actualiza el ImageView con la nueva imagen seleccionada
            imageUri?.let { uri ->
                // Usa Glide para cargar la imagen seleccionada
                Glide.with(this).load(uri).into(profileImageView)
                // Lógica para subir la imagen a Firebase
                uploadImageToFirebase(uri)
            }
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        // Verifica que username no sea nulo
        val userFolder = username ?: return
        val fileReference = storageReference.child("profile_pics/$userFolder/${userFolder}_profile.jpg")

        // Subir la imagen a Firebase Storage
        fileReference.putFile(uri).addOnSuccessListener {
            // Imagen subida exitosamente
            fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                // Actualiza la URL de la imagen en la base de datos
                updateProfileImageUrl(downloadUri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfileImageUrl(imageUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/${currentUser?.uid}")

        database.child("imageUrl").setValue(imageUrl).addOnSuccessListener {
            Toast.makeText(context, "Imagen de perfil actualizada", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Error al actualizar la URL de la imagen", Toast.LENGTH_SHORT).show()
        }
    }
}