package com.example.android_proj

import android.graphics.RectF

/**
 * Everything the user changed, as one immutable value.
 * The photo itself is never modified: each frame is rendered from the original + this state,
 * which makes undo/redo a simple list of states and the export identical to the preview.
 */
data class EditState(
    val filter: Filter = Filter.ORIGINAL,
    val filterAmount: Float = 1f,
    val adjust: Map<Adjust, Float> = emptyMap(),
    val effects: Map<Effect, Float> = emptyMap(),
    /** Clockwise quarter turns, 0..3. */
    val quarterTurns: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    /** Fine rotation in degrees, -45..45 (the image is zoomed so no empty corners show). */
    val straighten: Float = 0f,
    /** Crop in the rotated frame, normalised to 0..1. */
    val crop: RectF = FULL,
    val aspect: Aspect = Aspect.FREE,
) {
    fun adjust(a: Adjust) = adjust[a] ?: 0f
    fun effect(e: Effect) = effects[e] ?: 0f

    val isCropped get() = crop != FULL
    val hasGeometry get() = quarterTurns != 0 || flipH || flipV || straighten != 0f || isCropped
    val changeCount
        get() = (if (filter != Filter.ORIGINAL) 1 else 0) + adjust.count { it.value != 0f } +
            effects.count { it.value != 0f } + (if (hasGeometry) 1 else 0)

    /** Rotate the whole picture 90° the way the user sees it, keeping the crop on the same content. */
    fun rotated(clockwise: Boolean): EditState {
        // Flips are applied after the quarter turns, so with an odd number of flips a visual
        // clockwise turn is a counter-clockwise turn in source space.
        val mirrored = flipH xor flipV
        val step = if (clockwise xor mirrored) 1 else 3
        val c = crop
        val newCrop = if (clockwise) RectF(1 - c.bottom, c.left, 1 - c.top, c.right)
        else RectF(c.top, 1 - c.right, c.bottom, 1 - c.left)
        return copy(quarterTurns = (quarterTurns + step) % 4, crop = newCrop, aspect = aspect.turned())
    }

    fun flippedH() = copy(flipH = !flipH, crop = RectF(1 - crop.right, crop.top, 1 - crop.left, crop.bottom))
    fun flippedV() = copy(flipV = !flipV, crop = RectF(crop.left, 1 - crop.bottom, crop.right, 1 - crop.top))

    companion object {
        val FULL = RectF(0f, 0f, 1f, 1f)
    }
}

/** Colour adjustments. [bipolar] ones go -100..100 around zero, the rest 0..100. */
enum class Adjust(val label: String, val bipolar: Boolean = true) {
    EXPOSURE("Експозиція"),
    BRIGHTNESS("Яскравість"),
    CONTRAST("Контраст"),
    SATURATION("Насиченість"),
    WARMTH("Температура"),
    TINT("Відтінок"),
    HUE("Тон"),
    FADE("Вицвітання", bipolar = false),
}

/** Pixel effects, all 0..100. */
enum class Effect(val label: String) {
    VIGNETTE("Віньєтка"),
    GRAIN("Зерно"),
    SHARPEN("Різкість"),
    BLUR("Розмиття"),
    PIXELATE("Пікселі"),
    POSTERIZE("Постеризація"),
    GLITCH("RGB-зсув"),
}

enum class Aspect(val label: String, val ratio: Float?) {
    FREE("Вільно", null),
    ORIGINAL("Оригінал", -1f),
    SQUARE("1:1", 1f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    STORY_9_16("9:16", 9f / 16f);

    /** After a 90° turn a 4:3 crop becomes 3:4. */
    fun turned(): Aspect = when (this) {
        PORTRAIT_3_4 -> LANDSCAPE_4_3
        LANDSCAPE_4_3 -> PORTRAIT_3_4
        LANDSCAPE_16_9 -> STORY_9_16
        STORY_9_16 -> LANDSCAPE_16_9
        PORTRAIT_4_5 -> FREE
        else -> this
    }
}
