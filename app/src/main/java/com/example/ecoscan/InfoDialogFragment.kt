package com.example.ecoscan

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class InfoDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_info_dialog, container, false)

        val infoTextView = view.findViewById<TextView>(R.id.infoTextView)
        val okButton = view.findViewById<Button>(R.id.okButton)

        infoTextView.text = getString(R.string.info_message)

        okButton.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val window = dialog.window
            if (window != null) {
                val params = window.attributes
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                window.attributes = params
            }
        }
        return dialog
    }
}