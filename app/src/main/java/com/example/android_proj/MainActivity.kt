package com.example.android_proj

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.inSpans
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Home: where the photo comes from — the camera (physical device), the gallery,
 * or any image URL (handy on the emulator). The hero card is a live before/after
 * rendered with the same pipeline the editor uses.
 */
class MainActivity : AppCompatActivity() {

    private var cameraUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) EditorActivity.start(this, uri.toString())
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) EditorActivity.start(this, uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        setContentView(R.layout.activity_main)
        cameraUri = savedInstanceState?.getString(KEY_CAMERA_URI)?.let(Uri::parse)
        val content = findViewById<LinearLayout>(R.id.content)
        applyInsets(findViewById(R.id.main), top = content, bottom = content)

        findViewById<TextView>(R.id.title).text = SpannableStringBuilder(getString(R.string.home_title)).append(" ")
            .inSpans(ForegroundColorSpan(ContextCompat.getColor(this, R.color.cennge_primary))) {
                append(getString(R.string.home_title_version))
            }

        findViewById<LinearLayout>(R.id.camera).setOnClickListener { openCamera() }
        findViewById<LinearLayout>(R.id.gallery).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        val url = findViewById<EditText>(R.id.url)
        val open = { openUrl(url.text.toString().trim()) }
        findViewById<TextView>(R.id.open).setOnClickListener { open() }
        url.setOnEditorActionListener { _, action, _ -> (action == EditorInfo.IME_ACTION_GO).also { if (it) open() } }

        setupSamples(url)
        setupFeatures()
        setupHero()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cameraUri?.let { outState.putString(KEY_CAMERA_URI, it.toString()) }
    }

    private fun openCamera() {
        // The camera app writes the full-size photo into our cache through a FileProvider URI.
        val dir = File(cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        cameraUri = uri
        try {
            takePicture.launch(uri)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.home_no_camera, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, R.string.home_url_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        EditorActivity.start(this, url)
    }

    private fun setupSamples(url: EditText) {
        val row = findViewById<LinearLayout>(R.id.samples)
        SAMPLES.forEach { id ->
            val thumb = LayoutInflater.from(this).inflate(R.layout.item_sample, row, false) as ImageView
            thumb.clipToOutline = true
            thumb.load(unsplash(id, 240)) {
                crossfade(250)
                placeholder(R.drawable.bg_placeholder)
            }
            thumb.contentDescription = "Приклад фото"
            thumb.setOnClickListener {
                val full = unsplash(id, 2000)
                url.setText(full)
                EditorActivity.start(this, full)
            }
            row.addView(thumb)
        }
    }

    private fun setupFeatures() {
        val group = findViewById<ChipGroup>(R.id.features)
        listOf(
            "${Filter.entries.size - 1} фільтрів", "${Adjust.entries.size} корекцій", "${Effect.entries.size} ефектів",
            "Обрізка · ${Aspect.entries.size} форматів", "Поворот і дзеркало", "Вирівнювання ±45°",
            "Скасувати / Повторити", "Гістограма", "До / Після", "Збереження в галерею",
        ).forEach { label ->
            val chip = LayoutInflater.from(this).inflate(R.layout.item_chip, group, false) as TextView
            chip.text = label
            chip.isClickable = false
            chip.stateListAnimator = null
            chip.textSize = 12f
            chip.layoutParams = ChipGroup.LayoutParams(ChipGroup.LayoutParams.WRAP_CONTENT, dp(32))
            group.addView(chip)
        }
    }

    /** Hero: the original photo on the left, the editor's output on the right. */
    private fun setupHero() {
        val hero = findViewById<BeforeAfterView>(R.id.hero)
        hero.beforeLabel = getString(R.string.home_before)
        hero.afterLabel = getString(R.string.home_after)
        hero.setOnClickListener { EditorActivity.start(this, unsplash(SAMPLES.first(), 2000)) }
        lifecycleScope.launch {
            val before = loadBitmap(unsplash(SAMPLES.first(), 1200), 1200) ?: return@launch
            val after = withContext(Dispatchers.Default) {
                PhotoRenderer.render(
                    before,
                    EditState(
                        filter = Filter.KODA,
                        adjust = mapOf(Adjust.CONTRAST to 0.2f, Adjust.WARMTH to 0.25f),
                        effects = mapOf(Effect.VIGNETTE to 0.6f, Effect.GRAIN to 0.3f),
                    ),
                )
            }
            hero.setImages(before, after)
        }
    }

    companion object {
        private const val KEY_CAMERA_URI = "camera_uri"

        /** Free-to-use photos from Unsplash (Unsplash License). */
        val SAMPLES = listOf(
            "1501785888041-af3ef285b470", "1518791841217-8f162f1e1131", "1517849845537-4d257902454a",
            "1519681393784-d120267933ba", "1504674900247-0877df9cc836", "1507525428034-b723cf961d3e",
            "1477959858617-67f85cf4f1df", "1441974231531-c6227db76b6e",
        )

        fun unsplash(id: String, width: Int) = "https://images.unsplash.com/photo-$id?w=$width&q=85&fm=jpg"
    }
}
