package com.example.ecoscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import android.widget.TextView

class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_info, container, false)

        val cardNinos: CardView = rootView.findViewById(R.id.card_ninos)
        val cardAdolescentes: CardView = rootView.findViewById(R.id.card_adolescentes)
        val cardAdultos: CardView = rootView.findViewById(R.id.card_adultos)

        val reciclajeNinos: TextView = rootView.findViewById(R.id.reciclaje_ninos)
        val textNinos: TextView = rootView.findViewById(R.id.text_ninos)
        val tipsNinos: TextView = rootView.findViewById(R.id.tips_ninos)
        val tipsNinos1: TextView = rootView.findViewById(R.id.tips_ninos_1)
        val tipsNinos2: TextView = rootView.findViewById(R.id.tips_ninos_2)

        val reciclajeAdolescentes: TextView = rootView.findViewById(R.id.reciclaje_adolescentes)
        val textAdolescentes: TextView = rootView.findViewById(R.id.text_adolescentes)
        val tipsAdolescentes: TextView = rootView.findViewById(R.id.tips_adolescentes)
        val tipsAdolescentes1: TextView = rootView.findViewById(R.id.tips_adolescentes_1)
        val tipsAdolescentes2: TextView = rootView.findViewById(R.id.tips_adolescentes_2)

        val reciclajeAdultos: TextView = rootView.findViewById(R.id.reciclaje_adultos)
        val textAdultos: TextView = rootView.findViewById(R.id.text_adultos)
        val tipsAdultos: TextView = rootView.findViewById(R.id.tips_adultos)
        val tipsAdultos1: TextView = rootView.findViewById(R.id.tips_adultos_1)
        val tipsAdultos2: TextView = rootView.findViewById(R.id.tips_adultos_2)

        // Establecer los textos
        reciclajeNinos.text = getString(R.string.reciclaje_ninos)
        textNinos.text = getString(R.string.text_ninos)
        tipsNinos.text = getString(R.string.tips_general)
        tipsNinos1.text = getString(R.string.tips_ninos_1)
        tipsNinos2.text = getString(R.string.tips_ninos_2)

        reciclajeAdolescentes.text = getString(R.string.reciclaje_adolescentes)
        textAdolescentes.text = getString(R.string.text_adolescentes)
        tipsAdolescentes.text = getString(R.string.tips_general)
        tipsAdolescentes1.text = getString(R.string.tips_adolescentes_1)
        tipsAdolescentes2.text = getString(R.string.tips_adolescentes_2)

        reciclajeAdultos.text = getString(R.string.reciclaje_adultos)
        textAdultos.text = getString(R.string.text_adultos)
        tipsAdultos.text = getString(R.string.tips_general)
        tipsAdultos1.text = getString(R.string.tips_adultos_1)
        tipsAdultos2.text = getString(R.string.tips_adultos_2)

        return rootView
    }
}