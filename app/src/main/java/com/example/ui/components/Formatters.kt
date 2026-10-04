package com.example.ui.components

import java.text.NumberFormat
import java.util.Locale

fun formatNaira(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "₦" + formatter.format(amount)
}
