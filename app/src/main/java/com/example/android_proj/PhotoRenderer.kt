package com.example.android_proj

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * The editing pipeline. One pass draws the source through a geometry matrix (90° turns, mirror,
 * straighten + zoom-to-fill, crop) with the combined colour matrix (filter + adjustments);
 * pixel effects run after it. Sizes of effects are relative to the image, so the small preview
 * and the full-resolution export look the same.
 */
object PhotoRenderer {

    class Options(val crop: Boolean = true, val color: Boolean = true, val effects: Boolean = true)

    /** Order matters: blur before sharpen/pixels, vignette and grain always on top. */
    private val EFFECT_ORDER = listOf(
        Effect.BLUR, Effect.SHARPEN, Effect.PIXELATE, Effect.POSTERIZE, Effect.GLITCH, Effect.VIGNETTE, Effect.GRAIN,
    )

    /** Size of the picture after 90° turns, before crop. */
    fun orientedSize(w: Int, h: Int, quarterTurns: Int) = if (quarterTurns % 2 == 1) h to w else w to h

    suspend fun render(src: Bitmap, state: EditState, options: Options = Options()): Bitmap {
        val (ow, oh) = orientedSize(src.width, src.height, state.quarterTurns)
        val crop = if (options.crop) state.crop else EditState.FULL
        val outW = (crop.width() * ow).roundToInt().coerceAtLeast(1)
        val outH = (crop.height() * oh).roundToInt().coerceAtLeast(1)

        val matrix = Matrix().apply {
            setTranslate(-src.width / 2f, -src.height / 2f)
            postRotate(90f * state.quarterTurns)
            postScale(if (state.flipH) -1f else 1f, if (state.flipV) -1f else 1f)
            if (state.straighten != 0f) {
                postRotate(state.straighten)
                val s = coverScale(ow.toFloat(), oh.toFloat(), state.straighten)
                postScale(s, s)
            }
            postTranslate(ow / 2f - crop.left * ow, oh / 2f - crop.top * oh)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        if (options.color) {
            val cm = state.filter.matrix(state.filterAmount)
            cm.postConcat(ColorMath.adjustments(state))
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }
        var out = createBitmap(outW, outH)
        Canvas(out).apply {
            drawColor(Color.BLACK)
            drawBitmap(src, matrix, paint)
        }
        if (!options.effects) return out

        val ctx = currentCoroutineContext()
        for (effect in EFFECT_ORDER) {
            val v = state.effect(effect)
            if (v <= 0f) continue
            ctx.ensureActive()
            out = when (effect) {
                Effect.BLUR -> blur(out, v)
                Effect.SHARPEN -> sharpen(out, v)
                Effect.PIXELATE -> pixelate(out, v)
                Effect.POSTERIZE -> posterize(out, v)
                Effect.GLITCH -> glitch(out, v)
                Effect.VIGNETTE -> vignette(out, v)
                Effect.GRAIN -> grain(out, v)
            }
        }
        return out
    }

    /** Thumbnail of one filter: just the colour matrix, no adjustments or effects. */
    fun filterThumb(src: Bitmap, filter: Filter): Bitmap {
        val out = createBitmap(src.width, src.height)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(filter.matrix(1f)) }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /**
     * Zoom that keeps a w×h frame fully covered after rotating the picture by [degrees]:
     * every frame corner must stay inside the rotated image.
     */
    fun coverScale(w: Float, h: Float, degrees: Float): Float {
        val r = Math.toRadians(abs(degrees).toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        // +0.4% so antialiased edges never show a dark hairline
        return (c + s * max(w / h, h / w)) * 1.004f
    }

    // ------------------------------------------------------------------ effects

    private fun blur(src: Bitmap, v: Float): Bitmap {
        val radius = v * 0.03f * max(src.width, src.height)
        if (radius < 1f) return src
        // Blur a smaller copy (cheap and smooth), then scale back up with bilinear filtering.
        val factor = max(1f, radius / 4f)
        val small = src.scale((src.width / factor).roundToInt().coerceAtLeast(1), (src.height / factor).roundToInt().coerceAtLeast(1))
        val r = (radius / factor).roundToInt().coerceIn(1, 12)
        val w = small.width
        val h = small.height
        val px = IntArray(w * h).also { small.getPixels(it, 0, w, 0, 0, w, h) }
        repeat(3) { // three box blurs ≈ a gaussian
            boxBlur(px, w, h, r, horizontal = true)
            boxBlur(px, w, h, r, horizontal = false)
        }
        small.setPixels(px, 0, w, 0, 0, w, h)
        return small.scale(src.width, src.height)
    }

    private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val len = if (horizontal) w else h
        val lines = if (horizontal) h else w
        val line = IntArray(len)
        val div = 2 * r + 1
        for (l in 0 until lines) {
            for (i in 0 until len) line[i] = px[if (horizontal) l * w + i else i * w + l]
            var sr = 0; var sg = 0; var sb = 0
            for (k in -r..r) {
                val c = line[k.coerceIn(0, len - 1)]
                sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
            }
            for (i in 0 until len) {
                px[if (horizontal) l * w + i else i * w + l] =
                    (0xFF shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = line[(i + r + 1).coerceAtMost(len - 1)]
                val sub = line[(i - r).coerceAtLeast(0)]
                sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                sb += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }

    /** Unsharp mask with a 3×3 Laplacian: out = c + k·(4c − n − s − e − w). */
    private fun sharpen(src: Bitmap, v: Float): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src
        val inp = IntArray(w * h).also { src.getPixels(it, 0, w, 0, 0, w, h) }
        val out = inp.copyOf()
        val k = (v * 1.5f * 256).toInt() // fixed point, /256
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val i = row + x
                val c = inp[i]; val n = inp[i - w]; val s = inp[i + w]; val e = inp[i + 1]; val wv = inp[i - 1]
                fun ch(shift: Int): Int {
                    val cc = (c shr shift) and 0xFF
                    val lap = 4 * cc - ((n shr shift) and 0xFF) - ((s shr shift) and 0xFF) -
                        ((e shr shift) and 0xFF) - ((wv shr shift) and 0xFF)
                    return (cc + (lap * k shr 8)).coerceIn(0, 255)
                }
                out[i] = (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
            }
        }
        val bmp = createBitmap(w, h)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun pixelate(src: Bitmap, v: Float): Bitmap {
        val block = max(2f, v * max(src.width, src.height) / 30f)
        val small = src.scale(
            (src.width / block).roundToInt().coerceAtLeast(1),
            (src.height / block).roundToInt().coerceAtLeast(1),
        )
        return small.scale(src.width, src.height, filter = false)
    }

    private fun posterize(src: Bitmap, v: Float): Bitmap {
        val levels = (2 + (1f - v) * 10).roundToInt().coerceIn(2, 12)
        val step = 255f / (levels - 1)
        val lut = IntArray(256) { ((it / step).roundToInt() * step).roundToInt().coerceIn(0, 255) }
        val w = src.width
        val h = src.height
        val px = IntArray(w * h).also { src.getPixels(it, 0, w, 0, 0, w, h) }
        for (i in px.indices) {
            val c = px[i]
            px[i] = (0xFF shl 24) or (lut[(c shr 16) and 0xFF] shl 16) or (lut[(c shr 8) and 0xFF] shl 8) or lut[c and 0xFF]
        }
        val bmp = createBitmap(w, h)
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return bmp
    }

    /** Chromatic aberration: the R, G and B channels drawn with a horizontal offset and added together. */
    private fun glitch(src: Bitmap, v: Float): Bitmap {
        val shift = v * 0.025f * src.width
        val out = createBitmap(src.width, src.height)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)
        // CLAMP repeats the edge pixels, so shifted channels leave no coloured strips at the borders
        fun channel(r: Float, g: Float, b: Float, dx: Float, dy: Float) = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply { setTranslate(dx, dy) })
            }
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(r, 0f, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        canvas.drawRect(0f, 0f, w, h, channel(1f, 0f, 0f, -shift, 0f))
        canvas.drawRect(0f, 0f, w, h, channel(0f, 1f, 0f, 0f, 0f))
        canvas.drawRect(0f, 0f, w, h, channel(0f, 0f, 1f, shift, shift * 0.25f))
        return out
    }

    private fun vignette(src: Bitmap, v: Float): Bitmap {
        val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val radius = hypot(w, h) / 2f * 0.95f
        val inner = (0.6f - v * 0.3f).coerceIn(0.2f, 0.9f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2, h / 2, radius,
                intArrayOf(
                    Color.TRANSPARENT, Color.TRANSPARENT,
                    Color.argb((v * 120).roundToInt(), 0, 0, 0), Color.argb((v * 250).roundToInt(), 0, 0, 0),
                ),
                floatArrayOf(0f, inner, (inner + 1f) / 2f, 1f), Shader.TileMode.CLAMP,
            )
        }
        Canvas(bmp).drawRect(0f, 0f, w, h, paint)
        return bmp
    }

    private val noise: Bitmap by lazy {
        val size = 256
        val rnd = Random(42)
        val px = IntArray(size * size) {
            val g = (128 + rnd.nextFloat().let { (it + rnd.nextFloat() + rnd.nextFloat()) / 3f - 0.5f } * 220).roundToInt().coerceIn(0, 255)
            Color.rgb(g, g, g)
        }
        createBitmap(size, size).apply { setPixels(px, 0, size, 0, 0, size, size) }
    }

    /** Film grain: a tiled grey noise texture blended in overlay mode (grey 128 = no change). */
    private fun grain(src: Bitmap, v: Float): Bitmap {
        val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
        val grainScale = max(1f, max(bmp.width, bmp.height) / 1100f)
        val paint = Paint().apply {
            shader = BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
                setLocalMatrix(Matrix().apply { setScale(grainScale, grainScale) })
            }
            blendMode = BlendMode.OVERLAY
            alpha = (v * 200).roundToInt()
        }
        Canvas(bmp).drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), paint)
        return bmp
    }

    // ------------------------------------------------------------------ histogram

    /** Per-channel histograms (R, G, B, luma), each 256 bins, sampled on a grid for speed. */
    fun histogram(bmp: Bitmap): Array<IntArray> {
        val hist = Array(4) { IntArray(256) }
        val step = max(1, (max(bmp.width, bmp.height) / 256))
        val w = bmp.width
        val row = IntArray(w)
        var y = 0
        while (y < bmp.height) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            var x = 0
            while (x < w) {
                val c = row[x]
                val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
                hist[0][r]++; hist[1][g]++; hist[2][b]++
                hist[3][(r * 54 + g * 183 + b * 19) shr 8]++
                x += step
            }
            y += step
        }
        return hist
    }
}
