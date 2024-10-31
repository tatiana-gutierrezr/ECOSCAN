package com.example.ecoscan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class InfoxEdadFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_infox_edad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val boton4 = view.findViewById<Button>(R.id.btn_organicoo)
        boton4.setOnClickListener {
            val lanzar = Intent(requireActivity(), InfoFragment::class.java)
            startActivity(lanzar)
        }
    }
}