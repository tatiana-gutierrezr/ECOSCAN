package com.example.ecoscan

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.ecoscan.databinding.FragmentProductosBinding

class ProductosFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var _binding: FragmentProductosBinding? = null
    private val binding get() = _binding!!
    private lateinit var productoAdapter: ProductoAdapter
    private val materialCounts = mutableMapOf<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProductosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("history")

        // Configurar el RecyclerView
        productoAdapter = ProductoAdapter(materialCounts)
        binding.productosRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.productosRecyclerView.adapter = productoAdapter

        // Obtener el drawable del divisor
        val dividerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.divider, null)

        // Si el divisor está disponible, aplicamos el decorador personalizado
        dividerDrawable?.let {
            val customDividerDecoration = CustomDividerItemDecoration(requireContext(), it)
            binding.productosRecyclerView.addItemDecoration(customDividerDecoration)
        }

        val itemMargin = ContactMargin()
        binding.productosRecyclerView.addItemDecoration(itemMargin)

        binding.productosRecyclerView.setBackgroundResource(R.drawable.rv_border)

        binding.backarrow.setOnClickListener {
            requireActivity().onBackPressed()
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userEmail = currentUser.email?.replace(".", "_")
            if (userEmail != null) {
                database.child(userEmail).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        materialCounts.clear() // Limpiar los datos anteriores
                        var totalCount = 0 // Contador total de productos

                        for (record in snapshot.children) {
                            val objectLabel = record.child("objectLabel").value as? String
                            objectLabel?.let {
                                // Verificar si tiene el formato esperado "Objeto: [material]"
                                val parts = it.split(":")
                                if (parts.size > 1) {
                                    val material = parts[1].trim() // Obtener lo que sigue después de los dos puntos
                                    if (material.isNotEmpty()) {
                                        materialCounts[material] = materialCounts.getOrDefault(material, 0) + 1
                                        totalCount++
                                    } else {
                                        Log.e("ProductosFragment", "El campo 'objectLabel' no contiene material válido: $it")
                                    }
                                } else {
                                    Log.e("ProductosFragment", "Formato incorrecto en 'objectLabel': $it")
                                }
                            } ?: run {
                                Log.e("ProductosFragment", "El campo 'objectLabel' es nulo o vacío en este registro.")
                            }
                        }

                        // Actualizar el adaptador
                        productoAdapter.notifyDataSetChanged()

                        binding.totalProductosTextView.text = "Desde que creaste tu cuenta has escaneado $totalCount productos"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        binding.productosRecyclerView.visibility = View.GONE
                    }
                })
            }
        } else {
            binding.productosRecyclerView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
