package com.example.ecoscan

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class ScanDialogFragment : DialogFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
            val view = inflater.inflate(R.layout.fragment_info_dialog, container, false)

            val infoTextView = view.findViewById<TextView>(R.id.infoTextView)
            val okButton = view.findViewById<Button>(R.id.okButton)

            infoTextView.text = getString(R.string.scan_dialog)

            okButton.setOnClickListener {
            dismiss()
    }

    return view
    }

}