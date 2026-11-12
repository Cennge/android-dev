package com.example.android_proj

import android.app.Activity

/** The research result: top-5 third-party Android libraries picked for this demo. */
data class Library(
    val rank: Int,
    val name: String,
    val repo: String,
    val stars: String,
    val category: String,
    val description: String,
    val gradle: String,
    val demo: Class<out Activity>,
) {
    val owner: String get() = repo.substringBefore('/')
    /** GitHub serves the owner's avatar at this URL; Coil follows the redirect and caches it. */
    val avatarUrl: String get() = "https://github.com/$owner.png?size=200"
}

object Libraries {
    val all = listOf(
        Library(
            1, "Media3 · ExoPlayer", "androidx/media", "3.1k (+22k ExoPlayer)", "Відео / Аудіо",
            "Офіційний медіаплеєр Google: MP4, HLS, DASH, плейлисти, швидкість, треки.",
            "androidx.media3:media3-exoplayer:1.11.1", PlayerActivity::class.java,
        ),
        Library(
            2, "Filament", "google/filament", "20.7k", "3D графіка",
            "PBR-рушій реального часу від Google: glTF 2.0, IBL-освітлення, bloom, тіні.",
            "com.google.android.filament:filament-android:1.80.1", FilamentActivity::class.java,
        ),
        Library(
            3, "Lottie", "airbnb/lottie-android", "35.8k", "Анімації",
            "Відтворює векторні анімації After Effects (JSON) нативно, з керуванням прогресом.",
            "com.airbnb.android:lottie:6.7.1", LottieActivity::class.java,
        ),
        Library(
            4, "Coil 3", "coil-kt/coil", "12.0k", "Зображення",
            "Kotlin-first завантажувач зображень: кеш у пам'яті й на диску, трансформації, crossfade.",
            "io.coil-kt.coil3:coil:3.6.3", GalleryActivity::class.java,
        ),
        Library(
            5, "Konfetti", "DanielMartinus/Konfetti", "3.4k", "Частинки / FX",
            "Легка система частинок: вибухи конфеті, парад, дощ, власні форми.",
            "nl.dionsegijn:konfetti-xml:2.2.0", LottieActivity::class.java,
        ),
    )
}
