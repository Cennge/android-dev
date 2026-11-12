package com.example.android_proj

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.SingletonImageLoader
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import coil3.transform.RoundedCornersTransformation
import coil3.transform.Transformation
import com.google.android.material.appbar.MaterialToolbar

/**
 * Coil 3 demo: a grid of network images (Filament's showcase renders from GitHub),
 * each loaded with crossfade + placeholder + a different Transformation.
 * The toolbar shows the live size of Coil's memory cache.
 */
class GalleryActivity : AppCompatActivity() {

    private val images = listOf(
        "example_helmet", "example_bistro1", "example_materials1", "example_ssr",
        "example_bistro2", "example_materials2", "sample_gltf_viewer", "sample_image_based_lighting",
        "sample_textured_object", "sample_page_curl", "sample_procedural_texture_quad", "sample_hello_camera",
    ).map { "https://raw.githubusercontent.com/google/filament/main/docs/images/samples/$it.jpg" }

    private val modes: List<Pair<String, Transformation?>> = listOf(
        "CircleCrop" to CircleCropTransformation(),
        "RoundedCorners(48)" to RoundedCornersTransformation(48f),
        "Original" to null,
    )

    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        setContentView(R.layout.activity_gallery)
        findViewById<android.view.View>(R.id.main).applySystemBarsPadding()
        toolbar = findViewById(R.id.toolbar)
        setupToolbar(toolbar, "coil-kt/coil")

        findViewById<RecyclerView>(R.id.grid).apply {
            layoutManager = GridLayoutManager(this@GalleryActivity, 2)
            adapter = Adapter()
        }
    }

    private fun updateCacheStats() {
        val cache = SingletonImageLoader.get(this).memoryCache ?: return
        toolbar.subtitle = "memory cache: %.1f / %.0f MB · %d img".format(
            cache.size / 1_048_576.0, cache.maxSize / 1_048_576.0, cache.keys.size,
        )
    }

    private class Holder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
        val caption: TextView = view.findViewById(R.id.caption)
    }

    private inner class Adapter : RecyclerView.Adapter<Holder>() {
        override fun getItemCount() = images.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_gallery, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (label, transformation) = modes[position % modes.size]
            holder.caption.text = label
            holder.image.scaleType = if (transformation is CircleCropTransformation) {
                ImageView.ScaleType.FIT_CENTER
            } else {
                ImageView.ScaleType.CENTER_CROP
            }
            holder.image.load(images[position]) {
                crossfade(400)
                placeholder(R.drawable.placeholder)
                if (transformation != null) transformations(transformation)
                listener(onSuccess = { _, _ -> updateCacheStats() })
            }
        }
    }
}
