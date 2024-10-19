package com.example.ecoscan

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var passwrd: EditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var btnAtras: ImageButton
    private lateinit var btnClave: TextView
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email = findViewById(R.id.emailInput)
        passwrd = findViewById(R.id.passwordInput)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnAtras= findViewById(R.id.backarrow)
        btnClave = findViewById(R.id.forgotPassword)
        database = FirebaseDatabase.getInstance()
        reference = database.getReference("users")

        btnAtras.setOnClickListener{
            finish()
        }

        btnClave.setOnClickListener {

        }

        btnIniciarSesion.setOnClickListener {
            if (validateUsername() && validatePassword()) {
                loginUser()
            }
        }
    }

    private fun validateUsername(): Boolean {
        val emailText = email.text.toString().trim()
        return if (emailText.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validatePassword(): Boolean {
        val passwordText = passwrd.text.toString().trim()
        return if (passwordText.isEmpty()) {
            Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun loginUser() {
        val emailText = email.text.toString().trim()
        val passwordText = passwrd.text.toString().trim()

        reference.child(emailText).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userData = task.result
                if (userData.exists()) {
                    val storedPassword = userData.child("password").value.toString()
                    if (storedPassword == passwordText) {
                        Toast.makeText(this, "¡Inicio de sesión correcto!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this,"Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "El usuario no existe", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Error al recuperar los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}