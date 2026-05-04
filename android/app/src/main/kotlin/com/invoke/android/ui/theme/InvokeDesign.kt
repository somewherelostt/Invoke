package com.invoke.android.ui.theme

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object InvokeColor {
    const val Background = 0xFF080A0E.toInt()
    const val Surface = 0xFF101720.toInt()
    const val SurfaceElevated = 0xFF17212E.toInt()
    const val Input = 0xFF1F2A38.toInt()
    const val Primary = 0xFF6D5BFF.toInt()
    const val PrimarySoft = 0xFF332C72.toInt()
    const val MicPurple = 0xFF8B5CF6.toInt()
    const val TextPrimary = Color.WHITE
    const val TextSecondary = 0xFFC6CFDA.toInt()
    const val TextTertiary = 0xFF8994A3.toInt()
    const val Border = 0xFF2C3746.toInt()
    const val Success = 0xFF34D399.toInt()
    const val Warning = 0xFFFFB020.toInt()
    const val Danger = 0xFFFF5A6A.toInt()
}

object InvokeSpacing {
    const val Xs = 4
    const val Sm = 8
    const val Md = 12
    const val Lg = 16
    const val Xl = 24
    const val Xxl = 32
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun rounded(color: Int, radiusPx: Int, strokeColor: Int? = null, strokeWidthPx: Int = 1): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx.toFloat()
        if (strokeColor != null) setStroke(strokeWidthPx, strokeColor)
    }

fun TextView.titleStyle() {
    textSize = 28f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(InvokeColor.TextPrimary)
    includeFontPadding = false
}

fun TextView.headingStyle() {
    textSize = 20f
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(InvokeColor.TextPrimary)
}

fun TextView.bodyStyle() {
    textSize = 14f
    setTextColor(InvokeColor.TextSecondary)
    setLineSpacing(2f, 1.05f)
}

fun View.matchWidthCardMargins() {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, 0, 0, context.dp(InvokeSpacing.Md)) }
}
