package com.github.retrofitx.android.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.getSystemService

fun Context.showToast(@StringRes message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Context.hideKeyboard() {
    val imm = getSystemService<InputMethodManager>()!!
    imm.hideSoftInputFromWindow(getEnclosingActivity().window.decorView.windowToken, 0)
}

private fun Context.getEnclosingActivity(): Activity = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getEnclosingActivity()
    else -> throw IllegalArgumentException("Unsupported context subtype")
}