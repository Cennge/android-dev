package com.example.android_proj

import android.graphics.ColorMatrix
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Colour filters as 4x5 colour matrices (R, G, B, A rows; the 5th column is an offset in 0..255).
 * A matrix runs on the GPU-friendly Skia colour-filter path, so a filter costs one draw call.
 */
enum class Filter(val label: String, private val build: () -> ColorMatrix) {
    ORIGINAL("Оригінал", { ColorMatrix() }),
    VIVID("Яскравий", { saturation(1.45f).then(contrast(1.12f)) }),
    DRAMA("Драма", { contrast(1.4f).then(saturation(1.2f)).then(brightness(-12f)) }),
    CINEMA("Кіно", {
        // teal shadows / orange highlights, slightly crushed
        matrix(
            1.10f, 0.05f, -0.10f, 0f, -6f,
            -0.04f, 1.02f, 0.04f, 0f, 2f,
            -0.10f, 0.12f, 0.95f, 0f, 14f,
        ).then(contrast(1.18f)).then(saturation(0.9f))
    }),
    WARM("Тепло", { matrix(1.12f, 0f, 0f, 0f, 10f, 0f, 1.03f, 0f, 0f, 4f, 0f, 0f, 0.86f, 0f, -6f) }),
    COLD("Холод", { matrix(0.9f, 0f, 0f, 0f, -6f, 0f, 1.0f, 0f, 0f, 2f, 0f, 0f, 1.16f, 0f, 12f).then(saturation(1.05f)) }),
    VINTAGE("Вінтаж", {
        matrix(
            0.6279f, 0.3202f, -0.0397f, 0f, 9.65f,
            0.0258f, 0.6441f, 0.0326f, 0f, 7.46f,
            0.0466f, -0.0851f, 0.5242f, 0f, 5.16f,
        ).then(brightness(18f))
    }),
    KODA("Кодахром", {
        matrix(
            1.1286f, -0.3967f, -0.0399f, 0f, 63.73f,
            -0.1640f, 1.0835f, -0.0550f, 0f, 24.73f,
            -0.1679f, -0.5603f, 1.6015f, 0f, 35.63f,
        ).then(saturation(0.85f))
    }),
    TECHNI("Технікольор", {
        matrix(
            1.9125f, -0.8545f, -0.0916f, 0f, 11.79f,
            -0.3088f, 1.7659f, -0.1060f, 0f, -70.35f,
            -0.2311f, -0.7502f, 1.8476f, 0f, 30.95f,
        )
    }),
    POLAROID("Полароїд", {
        matrix(
            1.438f, -0.062f, -0.062f, 0f, 0f,
            -0.122f, 1.378f, -0.122f, 0f, 0f,
            -0.016f, -0.016f, 1.483f, 0f, 0f,
        ).then(contrast(0.9f)).then(brightness(-10f))
    }),
    MATTE("Мат", { contrast(0.78f).then(saturation(0.8f)).then(brightness(14f)) }),
    SEPIA("Сепія", {
        matrix(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
        )
    }),
    MONO("Ч/Б", { saturation(0f).then(contrast(1.08f)) }),
    NOIR("Нуар", { saturation(0f).then(contrast(1.65f)).then(brightness(-18f)) }),
    NIGHT("Нічне бачення", {
        matrix(
            0.1f, 0.4f, 0f, 0f, 0f,
            0.3f, 1.0f, 0.3f, 0f, 20f,
            0f, 0.4f, 0.1f, 0f, 0f,
        ).then(contrast(1.25f))
    }),
    INVERT("Негатив", { matrix(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f) });

    /** The filter blended with "no filter": amount 0 = original, 1 = full filter. */
    fun matrix(amount: Float): ColorMatrix {
        val full = build().array
        if (amount >= 1f) return ColorMatrix(full)
        val id = ColorMatrix().array
        return ColorMatrix(FloatArray(20) { id[it] + (full[it] - id[it]) * amount })
    }
}

object ColorMath {
    /** All adjustments combined into one matrix (applied after the filter). */
    fun adjustments(state: EditState): ColorMatrix {
        val m = ColorMatrix()
        val exposure = state.adjust(Adjust.EXPOSURE)
        if (exposure != 0f) m.postConcat(scale(2f.pow(exposure)))
        val brightness = state.adjust(Adjust.BRIGHTNESS)
        if (brightness != 0f) m.postConcat(brightness(brightness * 70f))
        val contrast = state.adjust(Adjust.CONTRAST)
        if (contrast != 0f) m.postConcat(contrast(if (contrast > 0) 1f + contrast * 0.8f else 1f + contrast * 0.6f))
        val saturation = state.adjust(Adjust.SATURATION)
        if (saturation != 0f) m.postConcat(saturation(1f + saturation))
        val warmth = state.adjust(Adjust.WARMTH)
        if (warmth != 0f) m.postConcat(
            matrix(1f + warmth * 0.15f, 0f, 0f, 0f, warmth * 14f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f - warmth * 0.15f, 0f, -warmth * 14f)
        )
        val tint = state.adjust(Adjust.TINT)
        if (tint != 0f) m.postConcat(
            matrix(1f, 0f, 0f, 0f, tint * 10f, 0f, 1f, 0f, 0f, -tint * 20f, 0f, 0f, 1f, 0f, tint * 10f)
        )
        val hue = state.adjust(Adjust.HUE)
        if (hue != 0f) m.postConcat(hue(hue * 180f))
        val fade = state.adjust(Adjust.FADE)
        if (fade != 0f) {
            val k = fade * 0.35f
            m.postConcat(matrix(1 - k, 0f, 0f, 0f, k * 120f, 0f, 1 - k, 0f, 0f, k * 120f, 0f, 0f, 1 - k, 0f, k * 120f))
        }
        return m
    }
}

// ------------------------------------------------------------------ matrix helpers

private fun matrix(vararg rgb: Float): ColorMatrix = ColorMatrix(rgb + floatArrayOf(0f, 0f, 0f, 1f, 0f))

private fun ColorMatrix.then(next: ColorMatrix): ColorMatrix = apply { postConcat(next) }

private fun saturation(s: Float) = ColorMatrix().apply { setSaturation(s) }

private fun scale(s: Float) = matrix(s, 0f, 0f, 0f, 0f, 0f, s, 0f, 0f, 0f, 0f, 0f, s, 0f, 0f)

private fun brightness(offset: Float) = matrix(1f, 0f, 0f, 0f, offset, 0f, 1f, 0f, 0f, offset, 0f, 0f, 1f, 0f, offset)

/** Contrast around mid-grey. */
private fun contrast(c: Float): ColorMatrix {
    val t = 128f * (1f - c)
    return matrix(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t)
}

/** Luminance-preserving hue rotation. */
private fun hue(degrees: Float): ColorMatrix {
    val r = Math.toRadians(degrees.toDouble())
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    val lr = 0.213f
    val lg = 0.715f
    val lb = 0.072f
    return matrix(
        lr + c * (1 - lr) + s * -lr, lg + c * -lg + s * -lg, lb + c * -lb + s * (1 - lb), 0f, 0f,
        lr + c * -lr + s * 0.143f, lg + c * (1 - lg) + s * 0.140f, lb + c * -lb + s * -0.283f, 0f, 0f,
        lr + c * -lr + s * -(1 - lr), lg + c * -lg + s * lg, lb + c * (1 - lb) + s * lb, 0f, 0f,
    )
}
