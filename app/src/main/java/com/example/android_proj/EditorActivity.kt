package com.example.android_proj

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.text.inSpans
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The editor. The original photo is never touched: every change produces a new [EditState],
 * the preview is re-rendered from a downscaled copy in the background (latest request wins),
 * and "Save" renders the same state at full resolution.
 */
class EditorActivity : AppCompatActivity() {

    private enum class Tool(val label: Int, val icon: Int, val panel: Int) {
        FILTERS(R.string.tab_filters, R.drawable.ic_filter, R.id.panelFilters),
        ADJUST(R.string.tab_adjust, R.drawable.ic_tune, R.id.panelAdjust),
        CROP(R.string.tab_crop, R.drawable.ic_crop, R.id.panelCrop),
        ROTATE(R.string.tab_rotate, R.drawable.ic_rotate_right, R.id.panelRotate),
        EFFECTS(R.string.tab_effects, R.drawable.ic_effects, R.id.panelEffects),
    }

    // ---- images
    private var full: Bitmap? = null      // what gets saved (≤ FULL_SIZE px)
    private var preview: Bitmap? = null   // what gets edited live (≤ PREVIEW_SIZE px)
    private var rendered: Bitmap? = null

    // ---- state + history
    private val history = mutableListOf(EditState())
    private var index = 0
    private val committed get() = history[index]
    /** Committed state + whatever the finger is dragging right now. */
    private var live = EditState()
    private val renderRequests = MutableStateFlow<Pair<EditState, Boolean>?>(null)

    private var tool = Tool.FILTERS
    private var selectedAdjust = Adjust.BRIGHTNESS
    private var selectedEffect = Effect.VIGNETTE

    // ---- views
    private lateinit var photo: ImageView
    private lateinit var cropOverlay: CropOverlayView
    private lateinit var histogram: HistogramView
    private lateinit var info: TextView
    private lateinit var toast: TextView
    private lateinit var undo: ImageButton
    private lateinit var redo: ImageButton
    private val tabs = mutableMapOf<Tool, View>()
    private val adjustChips = mutableMapOf<Adjust, TextView>()
    private val effectChips = mutableMapOf<Effect, TextView>()
    private val aspectChips = mutableMapOf<Aspect, TextView>()
    private lateinit var filterAdapter: FilterAdapter
    private lateinit var filterRow: SliderRow
    private lateinit var adjustRow: SliderRow
    private lateinit var straightenRow: SliderRow
    private lateinit var effectRow: SliderRow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        setContentView(R.layout.activity_editor)
        applyInsets(findViewById(R.id.main), top = findViewById(R.id.topBar), bottom = findViewById(R.id.sheet))

        photo = findViewById(R.id.photo)
        cropOverlay = findViewById(R.id.cropOverlay)
        histogram = findViewById(R.id.histogram)
        info = findViewById(R.id.info)
        toast = findViewById(R.id.toast)
        undo = findViewById(R.id.undo)
        redo = findViewById(R.id.redo)

        findViewById<View>(R.id.back).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        undo.setOnClickListener { moveInHistory(-1) }
        redo.setOnClickListener { moveInHistory(+1) }
        findViewById<View>(R.id.save).setOnClickListener { save() }
        findViewById<View>(R.id.share).setOnClickListener { share() }
        setupCompare()
        setupTabs()
        setupFilters()
        setupAdjust()
        setupCrop()
        setupRotate()
        setupEffects()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (committed == EditState()) finish() else confirmExit()
            }
        })

        lifecycleScope.launch { renderLoop() }
        load(intent.getStringExtra(EXTRA_SOURCE).orEmpty())
    }

    // ================================================================== loading & rendering

    private fun load(source: String) {
        lifecycleScope.launch {
            val bmp = runCatching { loadBitmap(source, FULL_SIZE) }.getOrNull()
            if (bmp == null) {
                findViewById<TextView>(R.id.loadingText).text = getString(R.string.editor_load_failed)
                findViewById<View>(R.id.loading).getChildAt(0).isVisible = false
                return@launch
            }
            full = bmp
            val small = withContext(Dispatchers.Default) { downscale(bmp, PREVIEW_SIZE) }
            preview = small
            findViewById<View>(R.id.loading).isVisible = false
            listOf(R.id.compare, R.id.share, R.id.histogramCard).forEach { reveal(findViewById(it)) }
            buildFilterThumbs(small)
            refreshUi()
            requestRender()
        }
    }

    private fun View.getChildAt(i: Int) = (this as ViewGroup).getChildAt(i)

    private fun reveal(v: View) {
        v.alpha = 0f; v.isVisible = true
        v.animate().alpha(1f).setDuration(300).setInterpolator(TactileEase).start()
    }

    private fun downscale(src: Bitmap, maxSide: Int): Bitmap {
        val s = maxSide / max(src.width, src.height).toFloat()
        return if (s >= 1f) src else src.scale((src.width * s).roundToInt(), (src.height * s).roundToInt())
    }

    private fun requestRender() {
        renderRequests.value = live to (tool == Tool.CROP)
    }

    /** Latest request wins: a new slider value cancels the render still in progress. */
    private suspend fun renderLoop() {
        renderRequests.filterNotNull().collectLatest { (state, cropMode) ->
            val src = preview ?: return@collectLatest
            val (bmp, hist) = withContext(Dispatchers.Default) {
                val out = PhotoRenderer.render(src, state, PhotoRenderer.Options(crop = !cropMode))
                out to PhotoRenderer.histogram(out)
            }
            rendered = bmp
            photo.setImageBitmap(bmp)
            histogram.setData(hist)
            if (cropMode) photo.post { syncCropOverlay() }
        }
    }

    // ================================================================== history

    private fun commit(state: EditState) {
        live = state
        if (state != committed) {
            while (history.size > index + 1) history.removeAt(history.lastIndex)
            history += state
            if (history.size > MAX_HISTORY) history.removeAt(0)
            index = history.lastIndex
        }
        refreshUi()
        requestRender()
    }

    /** Live preview while a slider is dragged; committed on release. */
    private fun preview(state: EditState) {
        live = state
        updateInfo()
        requestRender()
    }

    private fun moveInHistory(step: Int) {
        val target = index + step
        if (target !in history.indices) return
        index = target
        live = committed
        refreshUi()
        requestRender()
    }

    private fun refreshUi() {
        undo.isEnabled = index > 0
        redo.isEnabled = index < history.lastIndex
        updateInfo()
        filterAdapter.select(live.filter)
        filterRow.set(
            label = "${getString(R.string.filter_intensity)} · ${live.filter.label}",
            value = live.filterAmount * 100, enabled = live.filter != Filter.ORIGINAL,
        )
        adjustChips.forEach { (a, chip) -> chip.isSelected = a == selectedAdjust; chip.text = chipText(a.label, live.adjust(a)) }
        adjustRow.configure(selectedAdjust.bipolar)
        adjustRow.set(selectedAdjust.label, live.adjust(selectedAdjust) * 100)
        effectChips.forEach { (e, chip) -> chip.isSelected = e == selectedEffect; chip.text = chipText(e.label, live.effect(e)) }
        effectRow.set(selectedEffect.label, live.effect(selectedEffect) * 100)
        aspectChips.forEach { (a, chip) -> chip.isSelected = a == live.aspect }
        straightenRow.set(getString(R.string.straighten), live.straighten, suffix = "°", decimals = 1)
    }

    private fun updateInfo() {
        val src = full ?: return
        val (ow, oh) = PhotoRenderer.orientedSize(src.width, src.height, live.quarterTurns)
        val w = (live.crop.width() * ow).roundToInt()
        val h = (live.crop.height() * oh).roundToInt()
        val n = live.changeCount
        info.text = if (n == 0) "$w×$h · оригінал" else "$w×$h · ${changesLabel(n)}"
        findViewById<TextView>(R.id.cropInfo).text = if (live.isCropped) "Результат: $w × $h px" else getString(R.string.crop_hint)
    }

    private fun chipText(label: String, value: Float): CharSequence {
        if (value == 0f) return label
        val v = (value * 100).roundToInt()
        return SpannableStringBuilder(label).append("  ").inSpans(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.cennge_primary_text)),
        ) { append(if (v > 0) "+$v" else "$v") }
    }

    // ================================================================== tabs

    private fun setupTabs() {
        val container = findViewById<LinearLayout>(R.id.tabs)
        Tool.entries.forEach { t ->
            val tab = LayoutInflater.from(this).inflate(R.layout.item_tab, container, false)
            tab.findViewById<ImageView>(R.id.tabIcon).setImageResource(t.icon)
            tab.findViewById<TextView>(R.id.tabLabel).setText(t.label)
            tab.contentDescription = getString(t.label)
            tab.setOnClickListener { selectTool(t) }
            container.addView(tab)
            tabs[t] = tab
        }
        selectTool(Tool.FILTERS, animate = false)
    }

    private fun selectTool(t: Tool, animate: Boolean = true) {
        val previous = tool
        tool = t
        tabs.forEach { (k, v) -> v.isSelected = k == t }
        Tool.entries.forEach { findViewById<View>(it.panel).isVisible = it == t }
        val panel = findViewById<View>(t.panel)
        if (animate) {
            panel.alpha = 0f; panel.translationY = dp(8).toFloat()
            panel.animate().alpha(1f).translationY(0f).setDuration(200).setInterpolator(TactileEase).start()
        }
        val cropMode = t == Tool.CROP
        cropOverlay.isVisible = cropMode
        findViewById<View>(R.id.histogramCard).animate().alpha(if (cropMode) 0f else 1f).setDuration(150).start()
        if ((previous == Tool.CROP) != cropMode) requestRender()
    }

    // ================================================================== filters

    private fun setupFilters() {
        filterAdapter = FilterAdapter { f ->
            commit(live.copy(filter = f, filterAmount = if (f == live.filter) live.filterAmount else 1f))
        }
        findViewById<RecyclerView>(R.id.filters).apply {
            layoutManager = LinearLayoutManager(this@EditorActivity, RecyclerView.HORIZONTAL, false)
            adapter = filterAdapter
            itemAnimator = null
        }
        filterRow = SliderRow(findViewById(R.id.filterSlider), 0f, 100f,
            onDrag = { preview(live.copy(filterAmount = it / 100f)) },
            onRelease = { commit(live.copy(filterAmount = it / 100f)) })
    }

    private fun buildFilterThumbs(src: Bitmap) {
        lifecycleScope.launch {
            val side = 160
            val square = withContext(Dispatchers.Default) {
                val s = minOf(src.width, src.height)
                val crop = Bitmap.createBitmap(src, (src.width - s) / 2, (src.height - s) / 2, s, s)
                crop.scale(side, side)
            }
            Filter.entries.forEach { f ->
                val thumb = withContext(Dispatchers.Default) { PhotoRenderer.filterThumb(square, f) }
                filterAdapter.setThumb(f, thumb)
            }
        }
    }

    private inner class FilterAdapter(val onClick: (Filter) -> Unit) : RecyclerView.Adapter<FilterAdapter.Holder>() {
        private val thumbs = mutableMapOf<Filter, Bitmap>()
        private var selected = Filter.ORIGINAL

        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val thumb: ImageView = v.findViewById(R.id.thumb)
            val name: TextView = v.findViewById(R.id.name)
            init { thumb.clipToOutline = true }
        }

        fun setThumb(f: Filter, bmp: Bitmap) { thumbs[f] = bmp; notifyItemChanged(f.ordinal) }
        fun select(f: Filter) {
            if (f == selected) return
            val old = selected
            selected = f
            notifyItemChanged(old.ordinal); notifyItemChanged(f.ordinal)
        }

        override fun getItemCount() = Filter.entries.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_filter, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val f = Filter.entries[position]
            holder.name.text = f.label
            holder.thumb.setImageBitmap(thumbs[f])
            holder.itemView.isSelected = f == selected
            holder.itemView.setOnClickListener { onClick(f) }
        }
    }

    // ================================================================== adjust & effects

    private fun chip(container: LinearLayout, label: String, onClick: () -> Unit): TextView {
        val chip = LayoutInflater.from(this).inflate(R.layout.item_chip, container, false) as TextView
        chip.text = label
        chip.setOnClickListener { onClick() }
        container.addView(chip)
        return chip
    }

    private fun setupAdjust() {
        val container = findViewById<LinearLayout>(R.id.adjustChips)
        Adjust.entries.forEach { a ->
            adjustChips[a] = chip(container, a.label) { selectedAdjust = a; refreshUi() }
        }
        adjustRow = SliderRow(findViewById(R.id.adjustSlider), -100f, 100f,
            onDrag = { preview(live.withAdjust(selectedAdjust, it / 100f)) },
            onRelease = { commit(live.withAdjust(selectedAdjust, it / 100f)) },
            onReset = { commit(live.withAdjust(selectedAdjust, 0f)) })
    }

    private fun setupEffects() {
        val container = findViewById<LinearLayout>(R.id.effectChips)
        Effect.entries.forEach { e ->
            effectChips[e] = chip(container, e.label) { selectedEffect = e; refreshUi() }
        }
        effectRow = SliderRow(findViewById(R.id.effectSlider), 0f, 100f,
            onDrag = { preview(live.withEffect(selectedEffect, it / 100f)) },
            onRelease = { commit(live.withEffect(selectedEffect, it / 100f)) },
            onReset = { commit(live.withEffect(selectedEffect, 0f)) })
    }

    private fun EditState.withAdjust(a: Adjust, v: Float) =
        copy(adjust = (adjust + (a to v)).filterValues { it != 0f })

    private fun EditState.withEffect(e: Effect, v: Float) =
        copy(effects = (effects + (e to v)).filterValues { it != 0f })

    // ================================================================== crop

    private fun setupCrop() {
        val container = findViewById<LinearLayout>(R.id.aspectChips)
        Aspect.entries.forEach { a ->
            aspectChips[a] = chip(container, a.label) {
                val crop = cropOverlay.applyRatio(pixelRatio(a))
                commit(live.copy(aspect = a, crop = if (a == Aspect.FREE) live.crop else crop))
            }
        }
        cropOverlay.onCropChanged = { commit(live.copy(crop = it)) }
        findViewById<TextView>(R.id.cropReset).centerIconWithText()
        findViewById<View>(R.id.cropReset).setOnClickListener {
            commit(live.copy(crop = EditState.FULL, aspect = Aspect.FREE))
            syncCropOverlay()
        }
    }

    /** Ratio of the crop frame in pixels; "Original" is the photo's own ratio after turning. */
    private fun pixelRatio(a: Aspect): Float? {
        val src = preview ?: return null
        val (ow, oh) = PhotoRenderer.orientedSize(src.width, src.height, live.quarterTurns)
        return when (a.ratio) {
            null -> null
            -1f -> ow.toFloat() / oh
            else -> a.ratio
        }
    }

    /** Places the crop frame over the photo as the ImageView actually draws it (fitCenter + padding). */
    private fun syncCropOverlay() {
        val d = photo.drawable ?: return
        val bounds = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        photo.imageMatrix.mapRect(bounds)
        bounds.offset(photo.paddingLeft.toFloat(), photo.paddingTop.toFloat())
        cropOverlay.setImageBounds(bounds, live.crop, pixelRatio(live.aspect))
    }

    // ================================================================== rotate

    private fun setupRotate() {
        listOf(R.id.rotateLeft, R.id.rotateRight).forEach { findViewById<TextView>(it).centerIconWithText() }
        findViewById<View>(R.id.rotateLeft).setOnClickListener { commit(live.rotated(clockwise = false)) }
        findViewById<View>(R.id.rotateRight).setOnClickListener { commit(live.rotated(clockwise = true)) }
        findViewById<View>(R.id.flipH).setOnClickListener { commit(live.flippedH()) }
        findViewById<View>(R.id.flipV).setOnClickListener { commit(live.flippedV()) }
        straightenRow = SliderRow(findViewById(R.id.straightenSlider), -45f, 45f, step = 0.5f,
            onDrag = { preview(live.copy(straighten = it)) },
            onRelease = { commit(live.copy(straighten = it)) },
            onReset = { commit(live.copy(straighten = 0f)) })
        straightenRow.configure(bipolar = true)
    }

    // ================================================================== compare / save / share

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCompare() {
        val compare = findViewById<TextView>(R.id.compare)
        compare.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    preview?.let { photo.setImageBitmap(it) }
                    compare.text = getString(R.string.editor_original)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    rendered?.let { photo.setImageBitmap(it) }
                    compare.text = getString(R.string.editor_hold)
                }
            }
            true
        }
    }

    private suspend fun renderFull(): Bitmap? {
        val src = full ?: return null
        return withContext(Dispatchers.Default) { PhotoRenderer.render(src, committed) }
    }

    private fun save() {
        if (full == null) return
        lifecycleScope.launch {
            toast.flash("Зберігаю…", duration = 10_000)
            val ok = runCatching {
                val bmp = renderFull() ?: error("no image")
                withContext(Dispatchers.IO) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "Photoshop2_${System.currentTimeMillis()}.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Photoshop 2.0")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                    contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                    values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }
                "${bmp.width} × ${bmp.height}"
            }
            ok.onSuccess { size -> toast.flash("Збережено · $size · Pictures/Photoshop 2.0", R.drawable.ic_check) }
                .onFailure { toast.flash(getString(R.string.editor_save_failed), R.drawable.ic_close) }
        }
    }

    private fun share() {
        lifecycleScope.launch {
            val bmp = renderFull() ?: return@launch
            val file = withContext(Dispatchers.IO) {
                val dir = File(cacheDir, "shared").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                File(dir, "photoshop2_${System.currentTimeMillis()}.jpg").also { f ->
                    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                }
            }
            val uri = FileProvider.getUriForFile(this@EditorActivity, "$packageName.files", file)
            val send = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(send, null))
        }
    }

    private fun confirmExit() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Вийти з редактора?")
            .setMessage("Незбережені зміни (${changesLabel(committed.changeCount)}) буде втрачено.")
            .setNegativeButton("Залишитись", null)
            .setPositiveButton("Вийти") { _, _ -> finish() }
            .show()
    }

    // ================================================================== slider row

    /** Label + value + cennge slider. A double tap on the value resets it. */
    private inner class SliderRow(
        root: View,
        from: Float,
        to: Float,
        step: Float = 1f,
        onDrag: (Float) -> Unit,
        onRelease: (Float) -> Unit,
        onReset: (() -> Unit)? = null,
    ) {
        val label: TextView = root.findViewById(R.id.sliderLabel)
        val value: TextView = root.findViewById(R.id.sliderValue)
        val slider: Slider = root.findViewById(R.id.slider)
        private var suffix = ""
        private var decimals = 0

        init {
            slider.valueFrom = from
            slider.valueTo = to
            slider.stepSize = step
            slider.addOnChangeListener { _, v, fromUser ->
                showValue(v)
                if (fromUser) onDrag(v)
            }
            slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) = Unit
                override fun onStopTrackingTouch(slider: Slider) = onRelease(slider.value)
            })
            if (onReset != null) value.setOnClickListener { onReset() }
        }

        fun configure(bipolar: Boolean) {
            slider.valueFrom = if (bipolar) -abs(slider.valueTo) else 0f
            slider.isCentered = bipolar
        }

        fun set(label: String, value: Float, enabled: Boolean = true, suffix: String = "", decimals: Int = 0) {
            this.label.text = label
            this.suffix = suffix
            this.decimals = decimals
            slider.isEnabled = enabled
            slider.alpha = if (enabled) 1f else 0.35f
            val v = (if (slider.stepSize > 0) (value / slider.stepSize).roundToInt() * slider.stepSize else value)
                .coerceIn(slider.valueFrom, slider.valueTo)
            if (slider.value != v) slider.value = v
            showValue(v)
            this.value.alpha = if (enabled) 1f else 0.4f
        }

        private fun showValue(v: Float) {
            val text = if (decimals == 0) v.roundToInt().toString() else "%.${decimals}f".format(v)
            value.text = (if (v > 0 && slider.valueFrom < 0) "+" else "") + text + suffix
        }
    }

    companion object {
        const val EXTRA_SOURCE = "source"
        private const val FULL_SIZE = 3000
        private const val PREVIEW_SIZE = 1400
        private const val MAX_HISTORY = 60

        fun start(context: Context, source: String) {
            context.startActivity(Intent(context, EditorActivity::class.java).putExtra(EXTRA_SOURCE, source))
        }
    }
}
