package com.example.android_proj

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.toBitmap

/** Dark-first: light icons on transparent system bars in every activity. */
fun AppCompatActivity.enableDarkEdgeToEdge() = enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
)

/** Adds the system bar (and keyboard) insets to the given views' existing padding. */
fun applyInsets(root: View, top: View? = null, bottom: View? = null) {
    val topPad = top?.paddingTop ?: 0
    val bottomPad = bottom?.paddingBottom ?: 0
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        top?.updatePadding(top = topPad + bars.top)
        bottom?.updatePadding(bottom = bottomPad + bars.bottom)
        root.updatePadding(left = bars.left, right = bars.right)
        insets
    }
}

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

/**
 * Loads any photo source (https URL, content:// from the picker, the camera file) as a software
 * ARGB bitmap that can be drawn and read pixel by pixel. Coil applies EXIF rotation and
 * downsamples big camera photos so they fit in [maxSize].
 */
suspend fun Context.loadBitmap(source: Any, maxSize: Int): Bitmap? {
    val request = ImageRequest.Builder(this)
        .data(source)
        .size(maxSize)
        .precision(Precision.INEXACT)
        .allowHardware(false)
        .build()
    val result = imageLoader.execute(request)
    return (result as? SuccessResult)?.image?.toBitmap()
}

/** cennge-ui toast: a pill that slides in, waits and slides out. */
fun TextView.flash(message: String, iconRes: Int? = null, duration: Long = 2200) {
    animate().cancel()
    text = message
    setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes ?: 0, 0, 0, 0)
    if (!isVisible) {
        alpha = 0f
        translationY = -dp(12).toFloat()
        isVisible = true
    }
    animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(TactileEase).withEndAction {
        animate().alpha(0f).translationY(-dp(12).toFloat()).setStartDelay(duration).setDuration(180)
            .withEndAction { isVisible = false }.start()
    }.start()
}

private fun View.dp(v: Int) = context.dp(v)

/**
 * A wide button with a start icon: TextView draws the icon at the far edge,
 * so shift both padding sides to keep icon + text centred together.
 */
fun TextView.centerIconWithText() {
    addOnLayoutChangeListener { v, l, _, r, _, _, _, _, _ ->
        val tv = v as TextView
        val icon = tv.compoundDrawablesRelative[0] ?: return@addOnLayoutChangeListener
        val content = icon.intrinsicWidth + tv.compoundDrawablePadding + tv.paint.measureText(tv.text.toString())
        val side = ((r - l - content) / 2).toInt().coerceAtLeast(0)
        if (tv.paddingStart != side) tv.post { tv.setPaddingRelative(side, tv.paddingTop, side, tv.paddingBottom) }
    }
}

/** "1 зміна / 2 зміни / 5 змін" */
fun changesLabel(n: Int): String {
    val word = when {
        n % 10 == 1 && n % 100 != 11 -> "зміна"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "зміни"
        else -> "змін"
    }
    return "$n $word"
}
