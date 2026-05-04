package com.invoke.android.ui.theme

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object InvokeColor {
    const val Background = 0xFFF5EAF7.toInt()
    const val Surface = 0xFFFFF8FF.toInt()
    const val SurfaceElevated = 0xFFF9F0FB.toInt()
    const val Input = 0xFFF0E2F3.toInt()
    const val Primary = 0xFF4B294F.toInt()
    const val PrimaryDark = 0xFF321B35.toInt()
    const val PrimaryLight = 0xFFC78DD0.toInt()
    const val PrimarySoft = 0xFFE9D5EE.toInt()
    const val Cyan = 0xFF6D8C91.toInt()
    const val MicPurple = 0xFF4B294F.toInt()
    const val TextPrimary = 0xFF351E38.toInt()
    const val TextSecondary = 0xFF66516A.toInt()
    const val TextTertiary = 0xFF8F7A93.toInt()
    const val Border = 0xFFE6D3EA.toInt()
    const val Success = 0xFF40745B.toInt()
    const val Warning = 0xFF956A19.toInt()
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
