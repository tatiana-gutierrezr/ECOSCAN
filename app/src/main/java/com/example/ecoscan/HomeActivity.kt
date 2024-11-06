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


    // Función para verificar el estado del usuario (autenticado o no) y cargar el fragmento adecuado
    private fun checkUserStatusAndLoadProfileFragment() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null && !currentUser.isAnonymous) {
            // Usuario autenticado, cargar el perfil con cuenta
            replaceFragment(ProfileFragment())
        } else {
            // Usuario no autenticado o anónimo, cargar el perfil sin cuenta
            replaceFragment(AnonProfileFragment())
        }
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            // Si hay fragmentos en la pila de retroceso, hacer un pop
            fragmentManager.popBackStack()
        } else {
            // Si no hay fragmentos en la pila de retroceso, cerrar la actividad
            super.onBackPressed()
        }
    }



    fun replaceFragment(fragment: Fragment, showBottomNav: Boolean = true) {
        // Ocultar o mostrar el menú de navegación inferior
        bottomNavigationView.visibility = if (showBottomNav) View.VISIBLE else View.GONE

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameContainer, fragment)

        // Agregar el fragmento a la pila de retroceso
        transaction.addToBackStack(null)

        transaction.commit()
    }

    fun setBottomNavigationVisibility(isVisible: Boolean) {
        bottomNavigationView.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

}