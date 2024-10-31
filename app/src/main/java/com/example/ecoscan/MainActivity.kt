package com.example.ecoscan

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar si el usuario ya está autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Si el usuario está autenticado, redirigir a HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Finalizar MainActivity para que no esté en el stack
        } else {
            // Mostrar botones de inicio de sesión y registro solo si no hay usuario autenticado
            setupAuthButtons()
        }
    }

    private fun setupAuthButtons() {
        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)
        btnIniciarSesion.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val btnCrearCuenta = findViewById<Button>(R.id.btnCrearCuenta)
        btnCrearCuenta.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        val btnContinuarSinCuenta = findViewById<TextView>(R.id.btnContinuarSinCuenta)
        btnContinuarSinCuenta.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }
}