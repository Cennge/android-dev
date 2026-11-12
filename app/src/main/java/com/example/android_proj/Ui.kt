package com.example.android_proj

import android.graphics.Color
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import java.util.concurrent.TimeUnit

/** Dark app theme -> light status/navigation bar icons. */
fun AppCompatActivity.enableDarkEdgeToEdge() = enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
)

/** targetSdk 37 is always edge-to-edge: pad the root view by the system bars. */
fun View.applySystemBarsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
        insets
    }
}

fun AppCompatActivity.setupToolbar(toolbar: MaterialToolbar, subtitle: String? = null) {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.subtitle = subtitle
    toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
}

/** Konfetti presets, based on the samples linked from the Konfetti README. */
object Confetti {
    private val colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x7c9cff)
    private val shapes = listOf(Shape.Square, Shape.Circle, Shape.Polygon.STAR)

    fun explode(y: Double = 0.3) = listOf(
        Party(
            speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360,
            colors = colors, shapes = shapes,
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(120),
            position = Position.Relative(0.5, y),
        )
    )

    fun parade(): List<Party> {
        val base = Party(
            speed = 10f, maxSpeed = 30f, damping = 0.9f, angle = Angle.RIGHT - 45,
            spread = Spread.SMALL, colors = colors, shapes = shapes,
            emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(40),
            position = Position.Relative(0.0, 0.5),
        )
        return listOf(base, base.copy(angle = Angle.LEFT + 45, position = Position.Relative(1.0, 0.5)))
    }

    fun rain() = listOf(
        Party(
            speed = 0f, maxSpeed = 15f, damping = 0.9f, angle = Angle.BOTTOM,
            spread = Spread.ROUND, colors = colors, shapes = shapes,
            emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(100),
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
        )
    )
}
