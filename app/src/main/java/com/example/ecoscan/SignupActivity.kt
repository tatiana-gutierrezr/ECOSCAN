package com.example.ecoscan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var profileImageView: ShapeableImageView
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storageReference = FirebaseStorage.getInstance().reference

        profileImageView = findViewById(R.id.ImageView)
        val fullnameInput = findViewById<EditText>(R.id.fullnameInput)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val signupButton = findViewById<Button>(R.id.btnCrearCuenta)
        val fab = findViewById<ImageButton>(R.id.floatingActionButton)
        val btnAtras = findViewById<ImageButton>(R.id.backarrow)

        btnAtras.setOnClickListener {
            finish()
        }

        fab.setOnClickListener {
            chooseImage()
        }

        signupButton.setOnClickListener {
            val fullname = fullnameInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (fullname.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(fullname, username, email, password)
            }
        }
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            profileImageView.setImageURI(imageUri)
        }
    }

    private fun registerUser(fullname: String, username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    uploadImage(userId, fullname, username, email)
                }
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadImage(userId: String, fullname: String, username: String, email: String) {
        if (imageUri != null) {
            val fileRef = storageReference.child("profileImages/$userId.jpg")
            fileRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        saveUserInfo(userId, fullname, username, email, imageUrl)
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveUserInfo(userId, fullname, username, email, null)
        }
    }

    private fun saveUserInfo(userId: String, fullname: String, username: String, email: String, imageUrl: String?) {
        val user = User(fullname, username, email, imageUrl)
        database.child("users").child(userId).setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar la información: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}