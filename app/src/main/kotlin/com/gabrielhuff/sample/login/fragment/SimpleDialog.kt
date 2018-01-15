package com.gabrielhuff.sample.login.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.gabrielhuff.sample.login.R
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.dialog.view.*

/**
 * A dialog that displays a title, a description and a confirmation button passed as arguments
 * ([ARG_TITLE], [ARG_DESCRIPTION] and [ARG_BUTTON] respectively)
 */
class SimpleDialog : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate layout
        val view = activity.layoutInflater.inflate(R.layout.dialog, null)

        // Setup view contents
        view.title.text = arguments.getString(ARG_TITLE)
        view.description.text = arguments.getString(ARG_DESCRIPTION)
        view.button.text = arguments.getString(ARG_BUTTON)

        // Dismiss when button is clicked
        view.close.clicks().mergeWith(view.button.clicks()).subscribe { dismiss() }

        // Create dialog
        val dialog = AlertDialog.Builder(context).setView(view).create()

        // Make view background transparent (so the edges become effectively rounded)
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Return dialog
        return dialog
    }

    companion object {

        /**
         * Argument bundle key to be mapped to the dialog title string
         */
        const val ARG_TITLE = "title"

        /**
         * Argument bundle key to be mapped to the dialog description string
         */
        const val ARG_DESCRIPTION = "description"

        /**
         * Argument bundle key to be mapped to the dialog button string
         */
        const val ARG_BUTTON = "button"
    }
}