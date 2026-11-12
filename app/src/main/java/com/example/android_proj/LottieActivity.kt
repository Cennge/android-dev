package com.example.android_proj

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import nl.dionsegijn.konfetti.xml.KonfettiView

/**
 * Lottie demo: plays a vector animation from res/raw (JSON in Bodymovin format),
 * controls speed / progress, recolors a layer at runtime via KeyPath + dynamic properties.
 * Konfetti demo: three particle presets fired over the animation.
 */
class LottieActivity : AppCompatActivity() {

    private lateinit var lottie: LottieAnimationView
    private lateinit var konfetti: KonfettiView
    private var loops = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        setContentView(R.layout.activity_lottie)
        findViewById<android.view.View>(R.id.main).applySystemBarsPadding()
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), "airbnb/lottie-android + DanielMartinus/Konfetti")

        lottie = findViewById(R.id.lottie)
        konfetti = findViewById(R.id.konfetti)
        val info = findViewById<TextView>(R.id.info)
        val progress = findViewById<Slider>(R.id.progress)
        val play = findViewById<MaterialButton>(R.id.play)

        lottie.addLottieOnCompositionLoadedListener { composition ->
            info.text = "Композиція: ${composition.bounds.width()}×${composition.bounds.height()} · " +
                "${composition.frameRate.toInt()} fps · ${composition.durationFrames.toInt()} кадрів · " +
                "${composition.layers.size} шарів"
        }
        lottie.addAnimatorUpdateListener {
            if (lottie.isAnimating) progress.value = lottie.progress.coerceIn(0f, 1f)
        }
        lottie.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationRepeat(animation: Animator) {
                loops++
                // celebrate every 3rd loop
                if (loops % 3 == 0) konfetti.start(Confetti.explode(y = 0.35))
            }
        })

        progress.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                lottie.pauseAnimation()
                lottie.progress = value
                play.text = "Play"
            }
        }
        play.setOnClickListener {
            if (lottie.isAnimating) {
                lottie.pauseAnimation(); play.text = "Play"
            } else {
                lottie.resumeAnimation(); play.text = "Pause"
            }
        }

        findViewById<Slider>(R.id.speed).addOnChangeListener { _, value, _ ->
            lottie.speed = value
            findViewById<TextView>(R.id.speedLabel).text = "Швидкість: %.2f×".format(value)
        }

        // Dynamic properties: recolor the "star" layer's fill at runtime.
        val starKeyPath = KeyPath("star", "**")
        findViewById<MaterialSwitch>(R.id.recolor).setOnCheckedChangeListener { _, checked ->
            if (checked) {
                lottie.addValueCallback(starKeyPath, LottieProperty.COLOR_FILTER,
                    LottieValueCallback(PorterDuffColorFilter(0xFFFCE18A.toInt(), PorterDuff.Mode.SRC_ATOP)))
            } else {
                lottie.addValueCallback(starKeyPath, LottieProperty.COLOR_FILTER, LottieValueCallback(null))
            }
        }

        findViewById<MaterialButton>(R.id.burst).setOnClickListener { konfetti.start(Confetti.explode(y = 0.35)) }
        findViewById<MaterialButton>(R.id.parade).setOnClickListener { konfetti.start(Confetti.parade()) }
        findViewById<MaterialButton>(R.id.rain).setOnClickListener { konfetti.start(Confetti.rain()) }
    }
}
