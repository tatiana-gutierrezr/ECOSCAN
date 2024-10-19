package com.example.ecoscan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class MapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var btnAtras: ImageButton
    private lateinit var btnInfo: ImageButton
    private lateinit var googleMap: GoogleMap
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)

        autoCompleteTextView = view.findViewById(R.id.autoCompleteTextView)
        btnAtras = view.findViewById(R.id.backarrow)
        btnInfo = view.findViewById(R.id.infoButton)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val options = arrayOf("Bogotá", "Medellín")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        autoCompleteTextView.setAdapter(adapter)

        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedCity = parent.getItemAtPosition(position).toString()
            Log.d("MapsFragment", "Ciudad seleccionada: $selectedCity")
            mostrarCiudadEnMapa(selectedCity)  // Actualizamos el mapa según la ciudad seleccionada
        }

        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.showDropDown()
        }

        firestore = FirebaseFirestore.getInstance()

        btnAtras.setOnClickListener {
            requireActivity().onBackPressed()
        }

        btnInfo.setOnClickListener {
            val infoDialog = InfoDialogFragment()
            infoDialog.show(childFragmentManager, "InfoDialog")
        }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        verificarPermisos()
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mostrarPosicionPredeterminada()  // Mostramos la posición predeterminada
    }

    private fun verificarPermisos() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun mostrarPosicionPredeterminada() {
        val defaultLatLng = LatLng(4.5709, -74.2973)  // Ubicación por defecto
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))
        googleMap.clear()  // Limpiamos los marcadores actuales
        // No cargar marcadores de ciudades en la posición predeterminada
    }

    private fun mostrarCiudadEnMapa(ciudad: String) {
        // Definimos la posición de la ciudad seleccionada
        val latLng: LatLng = when (ciudad) {
            "Bogotá" -> LatLng(4.60971, -74.08175)
            "Medellín" -> LatLng(6.25184, -75.56359)
            else -> LatLng(4.570868, -74.297333)  // Posición por defecto si no se selecciona una ciudad conocida
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
        googleMap.clear()  // Limpiamos los marcadores actuales
        agregarMarcadoresDesdeFirestore(ciudad)  // Cargamos los puntos correspondientes a la ciudad seleccionada
    }

    private fun agregarMarcadoresDesdeFirestore(ciudad: String) {
        Log.d("MapsFragment", "Cargando puntos para la ciudad: $ciudad")

        firestore.collection("cities").document(ciudad).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d("MapsFragment", "Documento obtenido: ${document.data}")

                    for (field in document.data?.entries ?: emptySet()) {
                        val title = field.key
                        val value = field.value

                        // Verificamos si el valor es un GeoPoint
                        if (value is com.google.firebase.firestore.GeoPoint) {
                            val latLng = LatLng(value.latitude, value.longitude)
                            googleMap.addMarker(
                                MarkerOptions().position(latLng).title(title)
                            )
                            Log.d("MapsFragment", "Marcador añadido: $title -> LatLng(${value.latitude}, ${value.longitude})")
                        } else {
                            Log.e("MapsFragment", "El valor de $title no es un GeoPoint")
                        }
                    }
                } else {
                    Log.e("MapsFragment", "El documento no existe para la ciudad: $ciudad")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MapsFragment", "Error obteniendo documento: ", exception)
            }
    }
}