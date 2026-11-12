package com.example.android_proj

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.ui.PlayerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Media3 demo: a mixed playlist (progressive MP4, adaptive HLS stream, MP3 audio),
 * playback-speed control and live info about the selected video/audio tracks.
 */
@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {

    private data class Clip(val title: String, val subtitle: String, val uri: String, val mime: String? = null)

    private val clips = listOf(
        Clip("Big Buck Bunny", "MP4 · progressive",
            "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"),
        Clip("Apple BipBop", "HLS · adaptive bitrate",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            MimeTypes.APPLICATION_M3U8),
        Clip("Audio sample", "MP3 · audio only",
            "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"),
    )
    private val speeds = mapOf(R.id.speed05 to 0.5f, R.id.speed1 to 1f, R.id.speed15 to 1.5f, R.id.speed2 to 2f)

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var nowPlaying: TextView
    private lateinit var status: TextView
    private lateinit var trackInfo: TextView
    private lateinit var chips: ChipGroup
    private var startIndex = 0
    private var startPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        setContentView(R.layout.activity_player)
        findViewById<android.view.View>(R.id.main).applySystemBarsPadding()
        setupToolbar(findViewById<MaterialToolbar>(R.id.toolbar), "androidx/media · ${clips.size} кліпи в плейлисті")

        playerView = findViewById(R.id.playerView)
        nowPlaying = findViewById(R.id.nowPlaying)
        status = findViewById(R.id.status)
        trackInfo = findViewById(R.id.trackInfo)
        chips = findViewById(R.id.playlist)

        clips.forEachIndexed { index, clip ->
            chips.addView(Chip(this).apply {
                id = CHIP_ID_BASE + index
                text = clip.title
                isCheckable = true
                setOnClickListener { player?.seekTo(index, 0) }
            })
        }

        findViewById<MaterialButtonToggleGroup>(R.id.speedGroup).addOnButtonCheckedListener { _, id, checked ->
            if (checked) player?.setPlaybackSpeed(speeds.getValue(id))
        }
    }

    override fun onStart() {
        super.onStart()
        // Decoder fallback: if the primary (hardware) decoder fails to initialise or errors out,
        // Media3 retries with the next decoder for the format (e.g. the software c2.android.* one).
        val renderers = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(codecSelector)
        val exo = ExoPlayer.Builder(this, renderers).build()
        player = exo
        playerView.player = exo
        exo.addListener(listener)
        exo.setMediaItems(clips.map { it.toMediaItem() }, startIndex, startPosition)
        exo.prepare()
        exo.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.let {
            startIndex = it.currentMediaItemIndex
            startPosition = it.currentPosition
            it.removeListener(listener)
            it.release()
        }
        player = null
        playerView.player = null
    }

    /**
     * Custom MediaCodecSelector: on the Android emulator the "goldfish" hardware H.264 decoder errors
     * out mid-stream, so there we move it to the end of the list and let the software decoder go first.
     * On real devices the default order (hardware first) is kept.
     */
    private val codecSelector = MediaCodecSelector { mimeType, secure, tunneling ->
        val decoders = MediaCodecUtil.getDecoderInfos(mimeType, secure, tunneling)
        if (isEmulator) decoders.sortedBy { it.name.contains("goldfish") } else decoders
    }

    private val isEmulator: Boolean
        get() = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish" || Build.PRODUCT.startsWith("sdk_")

    /** Shows the formats that are actually being decoded, plus how many renditions the stream offers. */
    private fun refreshTrackInfo() {
        val exo = player ?: return
        val renditions = exo.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .sumOf { it.length }
        val lines = mutableListOf<String>()
        exo.videoFormat?.let { f ->
            lines += "Відео: ${f.sampleMimeType} · ${f.codecs ?: "?"}"
            lines += "  ${f.width}×${f.height}" + (if (f.bitrate > 0) " · ${f.bitrate / 1000} kbps" else "") +
                (if (f.frameRate > 0) " · %.0f fps".format(f.frameRate) else "")
            if (renditions > 1) lines += "  ABR: активна 1 з $renditions якостей"
        }
        exo.audioFormat?.let { f ->
            lines += "Аудіо: ${f.sampleMimeType}"
            lines += "  " + listOfNotNull(
                f.sampleRate.takeIf { it > 0 }?.let { "$it Hz" },
                f.channelCount.takeIf { it > 0 }?.let { "$it ch" },
                f.bitrate.takeIf { it > 0 }?.let { "${it / 1000} kbps" },
            ).joinToString(" · ")
        }
        trackInfo.text = lines.joinToString("\n").ifEmpty { "Треки ще не вибрано" }
    }

    private fun Clip.toMediaItem() = MediaItem.Builder()
        .setUri(uri)
        .setMimeType(mime)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(subtitle).build())
        .build()

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val meta = mediaItem?.mediaMetadata ?: return
            nowPlaying.text = "▶ ${meta.title} — ${meta.artist}"
            chips.check(CHIP_ID_BASE + (player?.currentMediaItemIndex ?: 0))
        }

        override fun onPlaybackStateChanged(state: Int) {
            status.text = "Стан плеєра: " + when (state) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING…"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "?"
            }
        }

        override fun onTracksChanged(tracks: Tracks) = refreshTrackInfo()
        override fun onVideoSizeChanged(videoSize: VideoSize) = refreshTrackInfo()

        override fun onPlayerError(error: PlaybackException) {
            status.text = "Помилка: ${error.errorCodeName}"
        }
    }

    private companion object {
        const val CHIP_ID_BASE = 1000
    }
}
