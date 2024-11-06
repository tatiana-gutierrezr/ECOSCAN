package com.example.ecoscan

import android.os.Bundle
import android.view.View
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
            replaceFragment(ProfileFragment())
        } else {
            // Usuario no autenticado o anónimo, cargar el perfil sin cuenta
            replaceFragment(AnonProfileFragment()) // Fragmento sin cuenta
        }
    }



    // Función para reemplazar fragmentos y controlar la visibilidad del menú inferior
    fun replaceFragment(fragment: Fragment, showBottomNav: Boolean = true) {
        // Oculta o muestra el menú de navegación inferior
        bottomNavigationView.visibility = if (showBottomNav) View.VISIBLE else View.GONE

        // Realiza la transacción de reemplazo de fragmento
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }
}