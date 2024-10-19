package com.example.ecoscan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var nombre: EditText
    private lateinit var usr: EditText
    private lateinit var email: EditText
    private lateinit var passwrd: EditText
    private lateinit var btnCrearCuenta: Button
    private lateinit var btnAtras: ImageButton
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        nombre = findViewById(R.id.fullnameInput)
        usr = findViewById(R.id.usernameInput)
        email = findViewById(R.id.emailInput)
        passwrd = findViewById(R.id.passwordInput)
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta)
        btnAtras = findViewById(R.id.backarrow)

        database = FirebaseDatabase.getInstance()
        reference = database.getReference("users")

        btnAtras.setOnClickListener {
            finish()
        }

        btnCrearCuenta.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = nombre.text.toString().trim()
        val username = usr.text.toString().trim()
        val emailInput = email.text.toString().trim()
        val password = passwrd.text.toString().trim()

        // Validar entradas
        if (name.isEmpty() || username.isEmpty() || emailInput.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear una instancia de la clase auxiliar
        val helperClass = HelperClass(name, emailInput, username, password)

        // Almacenar la información del usuario en la base de datos
        reference.child(username).setValue(helperClass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                clearInputs() // Limpiar los campos
                // NO redirigir a otra actividad
            } else {
                Toast.makeText(this, "El registro falló. Por favor vuelve a intentarlo.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputs() {
        nombre.text.clear()
        usr.text.clear()
        email.text.clear()
        passwrd.text.clear()
    }
}