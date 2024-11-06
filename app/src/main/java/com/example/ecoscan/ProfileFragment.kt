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
        val btnProductos = view.findViewById<Button>(R.id.btnProductos)
        val btnHistorial = view.findViewById<Button>(R.id.btnHistorial)
        profileImageView = view.findViewById<ImageView>(R.id.ImageView)
        val editNameButton = view.findViewById<ImageButton>(R.id.editName)
        val fullnameInput = view.findViewById<EditText>(R.id.fullnameTextView)

        // Inicializar Firebase Storage
        storageReference = FirebaseStorage.getInstance().reference

        // Obtener el correo electrónico del usuario actual
        val currentUser = FirebaseAuth.getInstance().currentUser
        val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users/${currentUser?.uid}")

        currentUser?.let {
            // Obtener los datos del usuario desde Firebase Realtime Database
            database.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val fullName = snapshot.child("fullName").getValue(String::class.java)
                    fullNameTextView.text = fullName ?: "Nombre no disponible"
                    username = snapshot.child("username").getValue(String::class.java)
                    usernameTextView.text = if (username != null) "@$username" else "Username no disponible"

                    val profileImageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.profile_pic)
                            .error(R.drawable.profile_pic)
                            .into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.profile_pic)
                    }
                } else {
                    fullNameTextView.text = "Usuario no encontrado"
                    usernameTextView.text = "Username no disponible"
                    profileImageView.setImageResource(R.drawable.profile_pic)
                }
            }.addOnFailureListener {
                fullNameTextView.text = "Error al cargar el nombre"
                usernameTextView.text = "Error al cargar el username"
                profileImageView.setImageResource(R.drawable.profile_pic)
            }
        }

        // Lógica para cerrar sesión
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        btnHistorial.setOnClickListener {
            replaceFragment(HistoryFragment())  // Reemplaza el fragmento actual por HistoryFragment
        }

        btnProductos.setOnClickListener {
            replaceFragment(ProductosFragment())  // Reemplaza el fragmento actual por HistoryFragment
        }

        // Lógica para editar el nombre completo
        editNameButton.setOnClickListener {
            Log.d("ProfileFragment", "Edit button clicked")
            if (fullnameInput.visibility == View.GONE) {
                fullnameInput.setText(fullNameTextView.text)
                fullnameInput.visibility = View.VISIBLE
                fullNameTextView.visibility = View.GONE
            } else {
                val newFullName = fullnameInput.text.toString().trim()
                if (newFullName.isNotEmpty()) {
                    database.child("fullName").setValue(newFullName).addOnSuccessListener {
                        fullNameTextView.text = newFullName
                        fullnameInput.visibility = View.GONE
                        fullNameTextView.visibility = View.VISIBLE
                        Toast.makeText(context, "Nombre actualizado exitosamente", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
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

        return view
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameContainer, fragment) // Asegúrate de que 'frameContainer' esté en tu layout
        transaction.addToBackStack(null)  // Opcional: añade a la pila de retroceso
        transaction.commit()
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_change_image -> {
                    changeProfileImage()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun changeProfileImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, requestCodePickImage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodePickImage && resultCode == AppCompatActivity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let { uri ->
                Glide.with(this).load(uri).into(profileImageView)
                uploadImageToFirebase(uri)
            }
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val userFolder = username ?: return
        val fileReference = storageReference.child("profile_pics/$userFolder/${userFolder}_profile.jpg")

        fileReference.putFile(uri).addOnSuccessListener {
            fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
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