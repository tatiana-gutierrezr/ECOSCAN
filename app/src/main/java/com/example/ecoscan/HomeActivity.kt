package com.example.ecoscan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigationView = findViewById(R.id.bottom_nav)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.scan -> {
                    replaceFragment(ScanFragment())
                    true
                }
                R.id.profile -> {
                    checkUserStatusAndLoadProfileFragment()
                    true
                }
                else -> false
            }
        }

        // Cargar el fragmento inicial (HomeFragment)
        replaceFragment(HomeFragment())
    }


    // Función para verificar el estado del usuario y cargar el fragmento adecuado
    private fun checkUserStatusAndLoadProfileFragment() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null && !currentUser.isAnonymous) {
            // Usuario autenticado, cargar el perfil con cuenta
            replaceFragment(ProfileFragment()) // Fragmento con perfil completo (usuario autenticado)
        } else {
            // Usuario no autenticado o anónimo, cargar el perfil sin cuenta
            replaceFragment(AnonProfileFragment()) // Fragmento sin cuen
        }
    }

    // Función para reemplazar fragmentos
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.frameContainer, fragment).commit()
    }
}