package com.example.ecoscan

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPassActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnAtras: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        btnAtras = findViewById(R.id.backarrow)

        btnAtras.setOnClickListener {
            finish()  //Esto destruye la actividad actual y regresa a la que habia antes
        }

        val emailInput: EditText = findViewById(R.id.emailInput)
        val sendButton: Button = findViewById(R.id.send)

        sendButton.setOnClickListener {
            val email = emailInput.text.toString()

            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Por favor ingresa un correo electrónico válido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Función para enviar el correo de restablecimiento de contraseña
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Si el correo fue enviado con éxito
                    Toast.makeText(this, "Se ha enviado un enlace de restablecimiento a tu correo.", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error al enviar el enlace. Verifica tu correo o intenta de nuevo.", Toast.LENGTH_LONG).show()
                }
            }
    }
}