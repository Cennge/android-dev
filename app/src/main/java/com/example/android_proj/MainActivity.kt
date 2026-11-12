package com.example.android_proj

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import coil3.transform.RoundedCornersTransformation
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import nl.dionsegijn.konfetti.xml.KonfettiView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        setContentView(R.layout.activity_main)
        findViewById<android.view.View>(R.id.main).applySystemBarsPadding()

        // Coil: banner with rounded corners (Filament's own showcase render from its repo)
        findViewById<ImageView>(R.id.banner).load(BANNER_URL) {
            crossfade(true)
            placeholder(R.drawable.placeholder)
            transformations(RoundedCornersTransformation(36f))
        }

        val list = findViewById<LinearLayout>(R.id.libraryList)
        val inflater = LayoutInflater.from(this)
        Libraries.all.forEach { lib ->
            val item = inflater.inflate(R.layout.item_library, list, false)
            item.findViewById<TextView>(R.id.rank).text = "#${lib.rank}"
            item.findViewById<TextView>(R.id.name).text = lib.name
            item.findViewById<TextView>(R.id.meta).text = "${lib.category}  ·  ★ ${lib.stars}"
            item.findViewById<TextView>(R.id.repo).text = "github.com/${lib.repo}"
            item.findViewById<TextView>(R.id.description).text = lib.description
            // Coil: GitHub avatar of the library owner, circle-cropped
            item.findViewById<ImageView>(R.id.avatar).load(lib.avatarUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder_circle)
                transformations(CircleCropTransformation())
            }
            item.setOnClickListener { startActivity(Intent(this, lib.demo)) }
            list.addView(item)
        }

        val konfetti = findViewById<KonfettiView>(R.id.konfetti)
        findViewById<ExtendedFloatingActionButton>(R.id.celebrate).setOnClickListener {
            konfetti.start(Confetti.explode())
        }
        if (savedInstanceState == null) konfetti.start(Confetti.parade())
    }

    companion object {
        const val BANNER_URL =
            "https://raw.githubusercontent.com/google/filament/main/docs/images/samples/example_helmet.jpg"
    }
}
