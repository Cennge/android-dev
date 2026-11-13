package com.example.android_proj

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private fun View.dp(v: Float) = v * resources.displayMetrics.density

/** cennge-ui ease-out, used for every motion in the app. */
val TactileEase = PathInterpolator(0.23f, 1f, 0.32f, 1f)

/**
 * Crop frame drawn over the (uncropped) photo. Drag a corner to resize, the inside to move.
 * Works in view coordinates and reports the crop normalised to the photo (0..1).
 */
class CropOverlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    /** Where the photo is drawn inside this view. */
    private val image = RectF()
    private val frame = RectF()
    private var ratio: Float? = null
    var onCropChanged: ((RectF) -> Unit)? = null

    private val scrim = Paint().apply { color = ContextCompat.getColor(context, R.color.cennge_scrim) }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1.5f); color = Color.argb(230, 255, 255, 255)
    }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1f); color = Color.argb(90, 255, 255, 255)
    }
    private val handle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(4f); strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.cennge_primary)
    }

    private enum class Drag { NONE, MOVE, TL, TR, BL, BR }
    private var drag = Drag.NONE
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    fun setImageBounds(bounds: RectF, crop: RectF, aspectRatio: Float?) {
        image.set(bounds)
        ratio = aspectRatio
        frame.set(
            image.left + crop.left * image.width(), image.top + crop.top * image.height(),
            image.left + crop.right * image.width(), image.top + crop.bottom * image.height(),
        )
        invalidate()
    }

    /** Largest centred frame with the given pixel ratio (w/h); null keeps the current frame. */
    fun applyRatio(aspectRatio: Float?): RectF {
        ratio = aspectRatio
        if (aspectRatio != null) {
            var w = image.width()
            var h = w / aspectRatio
            if (h > image.height()) { h = image.height(); w = h * aspectRatio }
            frame.set(image.centerX() - w / 2, image.centerY() - h / 2, image.centerX() + w / 2, image.centerY() + h / 2)
            invalidate()
        }
        return normalised()
    }

    private fun normalised() = RectF(
        ((frame.left - image.left) / image.width()).coerceIn(0f, 1f),
        ((frame.top - image.top) / image.height()).coerceIn(0f, 1f),
        ((frame.right - image.left) / image.width()).coerceIn(0f, 1f),
        ((frame.bottom - image.top) / image.height()).coerceIn(0f, 1f),
    )

    override fun onDraw(canvas: Canvas) {
        if (image.isEmpty) return
        // scrim outside the frame
        canvas.drawRect(image.left, image.top, image.right, frame.top, scrim)
        canvas.drawRect(image.left, frame.bottom, image.right, image.bottom, scrim)
        canvas.drawRect(image.left, frame.top, frame.left, frame.bottom, scrim)
        canvas.drawRect(frame.right, frame.top, image.right, frame.bottom, scrim)
        // rule of thirds
        grid.alpha = if (dragging) 140 else 70
        for (i in 1..2) {
            val x = frame.left + frame.width() * i / 3
            val y = frame.top + frame.height() * i / 3
            canvas.drawLine(x, frame.top, x, frame.bottom, grid)
            canvas.drawLine(frame.left, y, frame.right, y, grid)
        }
        canvas.drawRect(frame, border)
        // L-shaped corner handles
        val l = dp(18f)
        val o = dp(1f)
        with(frame) {
            canvas.drawLine(left - o, top - o, left + l, top - o, handle); canvas.drawLine(left - o, top - o, left - o, top + l, handle)
            canvas.drawLine(right + o, top - o, right - l, top - o, handle); canvas.drawLine(right + o, top - o, right + o, top + l, handle)
            canvas.drawLine(left - o, bottom + o, left + l, bottom + o, handle); canvas.drawLine(left - o, bottom + o, left - o, bottom - l, handle)
            canvas.drawLine(right + o, bottom + o, right - l, bottom + o, handle); canvas.drawLine(right + o, bottom + o, right + o, bottom - l, handle)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val slop = dp(32f)
                fun near(x: Float, y: Float) = abs(e.x - x) < slop && abs(e.y - y) < slop
                drag = when {
                    near(frame.left, frame.top) -> Drag.TL
                    near(frame.right, frame.top) -> Drag.TR
                    near(frame.left, frame.bottom) -> Drag.BL
                    near(frame.right, frame.bottom) -> Drag.BR
                    frame.contains(e.x, e.y) -> Drag.MOVE
                    else -> Drag.NONE
                }
                if (drag == Drag.NONE) return false
                lastX = e.x; lastY = e.y; dragging = true
                parent.requestDisallowInterceptTouchEvent(true)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.x - lastX
                val dy = e.y - lastY
                lastX = e.x; lastY = e.y
                if (drag == Drag.MOVE) move(dx, dy) else resize(e.x, e.y)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) onCropChanged?.invoke(normalised())
                dragging = false; drag = Drag.NONE
                invalidate()
            }
        }
        return true
    }

    private fun move(dx: Float, dy: Float) {
        val mx = dx.coerceIn(image.left - frame.left, image.right - frame.right)
        val my = dy.coerceIn(image.top - frame.top, image.bottom - frame.bottom)
        frame.offset(mx, my)
    }

    private fun resize(x: Float, y: Float) {
        val min = dp(56f)
        // the corner opposite to the dragged one stays put
        val ax = if (drag == Drag.TL || drag == Drag.BL) frame.right else frame.left
        val ay = if (drag == Drag.TL || drag == Drag.TR) frame.bottom else frame.top
        val left = drag == Drag.TL || drag == Drag.BL
        val top = drag == Drag.TL || drag == Drag.TR
        val maxW = if (left) ax - image.left else image.right - ax
        val maxH = if (top) ay - image.top else image.bottom - ay
        var w = (if (left) ax - x else x - ax).coerceIn(min, maxW)
        var h = (if (top) ay - y else y - ay).coerceIn(min, maxH)
        ratio?.let { r ->
            // keep the ratio: follow the finger on the dominant axis, fit inside the photo
            if (w / h > r) w = h * r else h = w / r
            if (w > maxW) { w = maxW; h = w / r }
            if (h > maxH) { h = maxH; w = h * r }
        }
        frame.set(
            if (left) ax - w else ax, if (top) ay - h else ay,
            if (left) ax else ax + w, if (top) ay else ay + h,
        )
    }
}

/** Live RGB histogram of the current render, drawn as three additive channel curves. */
class HistogramView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var data: Array<IntArray>? = null
    private val colors = intArrayOf(Color.rgb(248, 48, 73), Color.rgb(18, 184, 95), Color.rgb(59, 130, 246))
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.SCREEN }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1f); color = Color.argb(160, 232, 234, 238)
    }
    private val path = Path()

    fun setData(hist: Array<IntArray>) { data = hist; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val hist = data ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        // sqrt scale keeps dark/bright tails visible; ignore the clipped 0/255 bins for the peak
        val peak = (0..2).maxOf { c -> (1..254).maxOf { hist[c][it] } }.coerceAtLeast(1)
        val norm = sqrt(peak.toFloat())
        for (c in 0..2) {
            curve(hist[c], w, h, norm)
            fill.color = colors[c]; fill.alpha = 150
            canvas.drawPath(path, fill)
        }
        curve(hist[3], w, h, norm)
        canvas.drawPath(path, line)
    }

    private fun curve(bins: IntArray, w: Float, h: Float, norm: Float) {
        path.reset()
        path.moveTo(0f, h)
        for (i in 0..255) {
            val v = (bins[max(0, i - 1)] + 2 * bins[i] + bins[min(255, i + 1)]) / 4f
            path.lineTo(i / 255f * w, h - min(1f, sqrt(v) / norm) * h * 0.95f)
        }
        path.lineTo(w, h)
        path.close()
    }
}

/**
 * Before / after comparison for the home screen hero: drag the handle to reveal the edit.
 * Both bitmaps are drawn centre-cropped into a rounded card.
 */
class BeforeAfterView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var before: Bitmap? = null
    private var after: Bitmap? = null
    private var split = 0.5f
    private val radius = dp(16f)
    private val clip = Path()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = dp(2f) }
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; setShadowLayer(dp(6f), 0f, dp(2f), Color.argb(110, 0, 0, 0)) }
    private val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.cennge_primary_foreground)
        style = Paint.Style.STROKE; strokeWidth = dp(2f); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 17, 19, 23) }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = dp(12f)
        typeface = ResourcesCompat.getFont(context, R.font.inter_600)
    }
    private val placeholder = Paint().apply { color = ContextCompat.getColor(context, R.color.cennge_surface_4) }
    private val m = Matrix()
    var beforeLabel = "До"
    var afterLabel = "Після"

    fun setImages(before: Bitmap, after: Bitmap) {
        this.before = before
        this.after = after
        // a short "reveal" sweep so it is obvious the card is interactive
        ValueAnimator.ofFloat(0.92f, 0.12f, 0.5f).apply {
            duration = 1800
            interpolator = TactileEase
            addUpdateListener { split = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    fun updateAfter(after: Bitmap) { this.after = after; invalidate() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        clip.reset()
        clip.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), radius, radius, Path.Direction.CW)
    }

    private fun drawCover(canvas: Canvas, bmp: Bitmap) {
        val s = max(width / bmp.width.toFloat(), height / bmp.height.toFloat())
        m.setScale(s, s)
        m.postTranslate((width - bmp.width * s) / 2, (height - bmp.height * s) / 2)
        canvas.drawBitmap(bmp, m, bitmapPaint)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(clip)
        val b = before
        val a = after
        if (b == null || a == null) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), placeholder)
            canvas.restore(); return
        }
        val x = width * split
        drawCover(canvas, a)
        canvas.save(); canvas.clipRect(0f, 0f, x, height.toFloat()); drawCover(canvas, b); canvas.restore()
        // labels
        val pad = dp(10f)
        fun tag(text: String, right: Boolean) {
            val tw = label.measureText(text)
            val ph = dp(24f)
            val left = if (right) width - pad - tw - dp(20f) else pad
            canvas.drawRoundRect(left, pad, left + tw + dp(20f), pad + ph, ph / 2, ph / 2, pill)
            canvas.drawText(text, left + dp(10f), pad + ph / 2 + label.textSize * 0.36f, label)
        }
        tag(beforeLabel, false); tag(afterLabel, true)
        canvas.restore()
        // divider + knob
        canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
        val cy = height / 2f
        val r = dp(16f)
        canvas.drawCircle(x, cy, r, knob)
        val a1 = dp(4f)
        val path = Path().apply {
            moveTo(x - dp(3f), cy - a1); lineTo(x - dp(3f) - a1, cy); lineTo(x - dp(3f), cy + a1)
            moveTo(x + dp(3f), cy - a1); lineTo(x + dp(3f) + a1, cy); lineTo(x + dp(3f), cy + a1)
        }
        canvas.drawPath(path, arrow)
    }

    private var downX = 0f
    private var dragged = false
    private val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop

    /** Drag moves the divider; a plain tap is a click (opens this photo in the editor). */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (before == null) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = e.x; dragged = false }
            MotionEvent.ACTION_MOVE -> {
                if (!dragged && abs(e.x - downX) > slop) {
                    dragged = true
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                if (dragged) { split = (e.x / width).coerceIn(0.02f, 0.98f); invalidate() }
            }
            MotionEvent.ACTION_UP -> if (!dragged) performClick()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
