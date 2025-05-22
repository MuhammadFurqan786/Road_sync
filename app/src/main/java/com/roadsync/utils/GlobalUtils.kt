package com.roadsync.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.roadsync.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GlobalUtils {

    fun showMessage(rootView: View, message: String, length: Int = Snackbar.LENGTH_SHORT) {
        val sb = Snackbar.make(rootView, message, length)
        sb.setBackgroundTint(
            ContextCompat.getColor(
                rootView.context,
                R.color.primary_blue
            )
        ) // Set background color)
        sb.setTextColor(Color.WHITE)

        val snackBarView: View = sb.view
        val params = snackBarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM
        snackBarView.layoutParams = params
        params.setMargins(32, 0, 32, 32)
        // Create a rounded background drawable
        val backgroundDrawable = GradientDrawable()
        backgroundDrawable.setColor(
            ContextCompat.getColor(
                rootView.context,
                R.color.primary_blue
            )
        ) // Set background color
        backgroundDrawable.cornerRadius = 24f // Set corner radius (adjust as needed)

        // Apply the drawable as the background of the Snackbar view
        snackBarView.background = backgroundDrawable

        sb.show()
    }


    @SuppressLint("ClickableViewAccessibility")
    fun setupPasswordToggle(
        context: Context,
        editText: AppCompatEditText,
        showPasswordIcon: Int,
        hidePasswordIcon: Int
    ) {
        var isPasswordVisible = false

        fun setPasswordToggleDrawable() {
            // Check if there's an error
            val errorIcon = if (editText.error != null) {
                editText.compoundDrawablesRelative[0]  // Use system-generated error icon
            } else {
                null
            }

            // If there's an error, we'll show both the error icon and the password visibility toggle icon
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                errorIcon,  // Start drawable (error icon if present)
                null,       // Top drawable (not needed)
                if (errorIcon == null) {
                    // No error, show the password toggle icon
                    ContextCompat.getDrawable(
                        context,
                        if (isPasswordVisible) showPasswordIcon else hidePasswordIcon
                    )
                } else {
                    null
                },  // End drawable (password toggle icon only when no error)
                null        // Bottom drawable (not needed)
            )
        }

        fun togglePasswordVisibility() {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // Show password
                editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                // Hide password
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            // Set custom font to avoid resetting the typeface
            val customTypeface: Typeface? =
                ResourcesCompat.getFont(context, R.font.poppins_regular)
            editText.typeface = customTypeface

            // Update the toggle icon
            setPasswordToggleDrawable()

            // Keep cursor at the end
            editText.text?.let { editText.setSelection(it.length) }
        }

        // Set initial toggle icon
        setPasswordToggleDrawable()

        // Add touch listener for toggling password visibility
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd =
                    editText.compoundDrawablesRelative[2] // End drawable (password toggle icon)

                if (drawableEnd != null) {
                    val drawableWidth = drawableEnd.bounds.width()
                    val clickArea = editText.width - editText.paddingEnd - drawableWidth

                    // Check if click is on the password toggle icon (on the right side)
                    if (event.x >= clickArea && editText.error == null) {
                        togglePasswordVisibility()
                        return@setOnTouchListener true // Consume the event and stop further processing
                    }
                }
            }
            false
        }

        // Observe error state and update the toggle icon accordingly
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Re-enable toggle icon when error is cleared
                setPasswordToggleDrawable()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }




    fun Long.toReadableDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(this))
    }



}