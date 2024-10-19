package com.example.ecoscan

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class StartGameDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("En el mapa se encuentran las ubicaciones de Ecobot, donde se pueden depositar envases plásticos, latas, botellas llenas y Tetra Paks. Estas máquinas están diseñadas para facilitar el reciclaje y promover prácticas sostenibles.")
                .setPositiveButton("OK") { dialog, id ->
                    // Cerrar el diálogo.
                    dialog.dismiss()
                }
            // Crear y retornar el AlertDialog.
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}