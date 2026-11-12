package com.example.android_proj

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.IBLPrefilterContext
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import com.google.android.filament.utils.max
import com.google.android.filament.utils.rotation
import com.google.android.filament.utils.scale
import com.google.android.filament.utils.translation
import com.google.android.filament.utils.transpose
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Filament demo: loads a glTF 2.0 binary model (Khronos "Damaged Helmet"),
 * lights it with image-based lighting generated at runtime from an HDR panorama,
 * enables bloom / SSAO / FXAA and spins it. Drag to orbit, pinch to zoom.
 */
class FilamentActivity : AppCompatActivity() {

    companion object {
        // README: "You must always first initialize Filament" — Utils.init() also loads gltfio.
        init { Utils.init() }

        private val ENVIRONMENTS = mapOf(
            R.id.envStudio to "envs/studio_small_09.hdr",
            R.id.envPhoto to "envs/brown_photostudio_02.hdr",
        )
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var modelViewer: ModelViewer
    private lateinit var stats: TextView
    private val choreographer: Choreographer = Choreographer.getInstance()

    private lateinit var modelBuffer: ByteBuffer
    private var pendingEnvironment: String? = null
    private var autoRotate = true
    private var angle = 0f
    private var frames = 0
    private var fpsWindowStart = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            if (autoRotate) angle = (angle + 0.6f) % 360f
            updateModelTransform()
            updateProjection()
            val rendered = modelViewer.render(frameTimeNanos)
            // IBLPrefilterContext must run once the engine is rendering (as in Filament's gltf-viewer
            // sample, which builds IBL on demand). Running it in onCreate corrupted memory.
            if (rendered) pendingEnvironment?.let {
                pendingEnvironment = null
                loadEnvironment(it)
            }
            countFps(frameTimeNanos)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        setContentView(R.layout.activity_filament)
        findViewById<android.view.View>(R.id.main).applySystemBarsPadding()
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), "google/filament · PBR + IBL")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        stats = findViewById(R.id.stats)
        surfaceView = findViewById(R.id.surface)
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer) // orbit / zoom camera manipulator

        // gltfio decodes the embedded textures asynchronously from this buffer,
        // so keep a strong reference to it for the lifetime of the activity.
        modelBuffer = assets.open("models/DamagedHelmet.glb").use { ByteBuffer.wrap(it.readBytes()) }
        modelViewer.loadModelGlb(modelBuffer)
        setupView()
        pendingEnvironment = ENVIRONMENTS.getValue(R.id.envStudio)

        findViewById<MaterialSwitch>(R.id.rotateSwitch).setOnCheckedChangeListener { _, checked ->
            autoRotate = checked
        }
        findViewById<MaterialButtonToggleGroup>(R.id.envGroup).addOnButtonCheckedListener { _, id, checked ->
            if (checked) pendingEnvironment = ENVIRONMENTS.getValue(id)
        }

        val asset = modelViewer.asset!!
        stats.text = "glTF: DamagedHelmet.glb · ${asset.entities.size} entities · " +
            "${asset.renderableEntities.size} renderables"
    }

    private fun setupView() {
        val view = modelViewer.view
        view.renderQuality = view.renderQuality.apply { hdrColorBuffer = View.QualityLevel.MEDIUM }
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            enabled = true
            quality = View.QualityLevel.MEDIUM
        }
        view.antiAliasing = View.AntiAliasing.FXAA
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = true }
        view.bloomOptions = view.bloomOptions.apply {
            enabled = true
            strength = 0.15f
        }
    }

    /** One prefilter context for the activity; built lazily once the engine is rendering. */
    private val prefilter by lazy {
        val context = IBLPrefilterContext(modelViewer.engine)
        Triple(
            context,
            IBLPrefilterContext.EquirectangularToCubemap(context),
            IBLPrefilterContext.SpecularFilter(context),
        )
    }
    private val environments = mutableMapOf<String, Pair<IndirectLight, Skybox>>()

    /**
     * HDR panorama -> cubemap -> prefiltered specular reflections (IBLPrefilterContext).
     * Each environment is built once and cached, so switching is instant.
     * Note: destroying the prefilter objects/equirect texture right after use (as the gltf-viewer
     * sample does) crashed the renderer on the next frame here, so they live as long as the engine.
     */
    private fun loadEnvironment(path: String) {
        val (ibl, sky) = environments.getOrPut(path) {
            val engine = modelViewer.engine
            val (_, toCubemap, specular) = prefilter
            val equirect = HDRLoader.createTexture(engine, readAsset(path)) ?: return
            val skyboxTexture = toCubemap.run(equirect)!!
            val reflections = specular.run(skyboxTexture)
            IndirectLight.Builder().reflections(reflections).intensity(28_000f).build(engine) to
                Skybox.Builder().environment(skyboxTexture).build(engine)
        }
        modelViewer.scene.indirectLight = ibl
        modelViewer.scene.skybox = sky
    }

    /**
     * Workaround: on this emulator ModelViewer's setLensProjection() leaves a degenerate projection
     * (focal ~1 mm, far = 1 m) that clips the model, and it re-applies it on resize.
     * Setting a FOV-based projection every frame is cheap and keeps the camera correct.
     */
    private fun updateProjection() {
        val vp = modelViewer.view.viewport
        if (vp.width == 0 || vp.height == 0) return
        modelViewer.camera.setProjection(45.0, vp.width.toDouble() / vp.height, 0.05, 100.0, com.google.android.filament.Camera.Fov.VERTICAL)
    }

    /** Same math as ModelViewer.transformToUnitCube(), plus a rotation around Y. */
    private fun updateModelTransform() {
        val asset = modelViewer.asset ?: return
        val tm = modelViewer.engine.transformManager
        val center = asset.boundingBox.center.let { Float3(it[0], it[1], it[2]) }
        val halfExtent = asset.boundingBox.halfExtent.let { Float3(it[0], it[1], it[2]) }
        val scaleFactor = 2.0f / (2.0f * max(halfExtent))
        val transform = translation(Float3(0f, 0f, -4f)) *
            rotation(Float3(0f, 1f, 0f), angle) *
            scale(Float3(scaleFactor)) *
            translation(-center)
        tm.setTransform(tm.getInstance(asset.root), transpose(transform).toFloatArray())
    }

    private fun countFps(frameTimeNanos: Long) {
        if (fpsWindowStart == 0L) fpsWindowStart = frameTimeNanos
        frames++
        val elapsed = frameTimeNanos - fpsWindowStart
        if (elapsed >= 1_000_000_000L) {
            val fps = frames * 1e9 / elapsed
            findViewById<TextView>(R.id.fps).text = "%.0f FPS · кут %.0f°".format(fps, angle)
            frames = 0
            fpsWindowStart = frameTimeNanos
        }
    }

    private fun readAsset(name: String): ByteBuffer {
        val bytes = assets.open(name).use { it.readBytes() }
        return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).flip() as ByteBuffer
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallback)
        modelViewer.destroyModel()
    }
}
