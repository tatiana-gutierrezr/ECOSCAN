package com.example.ecoscan

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecoscan.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storageMetadata
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private lateinit var btnAtras: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val historyItems = mutableListOf<HistoryItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar la vista del fragmento
        val binding = FragmentHistoryBinding.inflate(inflater, container, false)
        recyclerView = binding.recyclerView2
        adapter = HistoryAdapter(requireContext(), historyItems)
        recyclerView.adapter = adapter


        // Ocultar el BottomNavigationView
        (activity as? HomeActivity)?.replaceFragment(this, showBottomNav = false)
        btnAtras = binding.backarrow


        // Configuración del LayoutManager
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Verificar si el usuario está autenticado
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // El usuario está autenticado, cargamos los datos
            loadHistoryData(user.email ?: "")
        } else {
            // El usuario no está autenticado, mostrar mensaje o tomar acción adecuada
            Log.e("HistoryFragment", "Usuario no autenticado")
        }

        btnAtras.setOnClickListener{
            requireActivity().onBackPressed()
        }

        return binding.root
    }

    private fun loadHistoryData(userEmail: String) {
        // Reemplazar los puntos en el correo para que coincida con la estructura de Firebase
        val formattedEmail = userEmail.replace(".", "_")

        // Referencia a la base de datos para el usuario específico
        val databaseReference = FirebaseDatabase.getInstance().getReference("history")
        val userHistoryRef = databaseReference.child(formattedEmail)

        // Leer los datos de la base de datos
        userHistoryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<HistoryItem>()
                val imagesToLoad = mutableListOf<HistoryItem>()

                // Iterar a través de los registros del usuario
                for (dataSnapshot in snapshot.children) {
                    val historyItem = dataSnapshot.getValue(HistoryItem::class.java)

                    // Filtrar registros con el mensaje de error
                    if (historyItem != null && historyItem.resultTextMessage != "Lo sentimos, no fue posible hacer el análisis. Por favor vuelve a intentarlo.") {
                        // Procesamos la fecha (solo la fecha, sin la hora)
                        val processedDate = processDate(historyItem.date)

                        // Creamos un nuevo item con la fecha procesada
                        val newItem = historyItem.copy(date = processedDate)

                        // Verificar si la URL de la imagen es válida
                        if (newItem.imageUrl.isNotBlank()) {
                            imagesToLoad.add(newItem)
                        }
                    }
                }

                // Si hay imágenes para cargar, obtendremos sus URLs de Firebase Storage
                if (imagesToLoad.isNotEmpty()) {
                    // Usamos un contador para saber cuándo todas las imágenes se hayan cargado
                    var loadedImagesCount = 0
                    for (item in imagesToLoad) {
                        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(item.imageUrl)

                        // Obtener la URL pública de la imagen
                        storageReference.downloadUrl.addOnSuccessListener { uri ->
                            // Asignar la URL pública (https://) al objeto
                            item.imageUrl = uri.toString()

                            // Agregar el item a la lista de elementos
                            items.add(item)
                            loadedImagesCount++

                            // Verificar si todas las imágenes han sido cargadas
                            if (loadedImagesCount == imagesToLoad.size) {
                                // Actualizamos el RecyclerView solo cuando todas las imágenes se hayan cargado
                                adapter.updateList(items)
                            }
                        }.addOnFailureListener { exception ->
                            Log.e("HistoryFragment", "Error al obtener la URL pública: ${exception.message}")
                        }
                    }
                } else {
                    // Si no hay elementos para cargar
                    Log.e("HistoryFragment", "No hay imágenes para cargar.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar error si ocurre
                Log.e("HistoryFragment", "Error al cargar los datos: ${error.message}")
            }
        })
    }

    // Método para procesar la fecha (solo dd-MM-yyyy)
    private fun processDate(date: String): String {
        return try {
            val originalFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val parsedDate = originalFormat.parse(date)
            val targetFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            targetFormat.format(parsedDate ?: Date())
        } catch (e: ParseException) {
            Log.e("HistoryFragment", "Error al procesar la fecha: ${e.message}")
            date // Devolver la fecha original si hay un error
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restaurar el BottomNavigationView cuando se salga del MapsFragment
        (activity as? HomeActivity)?.replaceFragment(HomeFragment())
    }
}