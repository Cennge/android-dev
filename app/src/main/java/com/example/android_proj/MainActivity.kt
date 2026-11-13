package com.example.android_proj

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private var imageUri: Uri? = null

    private lateinit var image: ImageView
    private lateinit var hint: TextView
    private lateinit var text: EditText

    // системний вибір зображення з пристрою (дозволи не потрібні)
    private val pickImage = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) showImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        image = findViewById(R.id.image)
        hint = findViewById(R.id.imageHint)
        text = findViewById(R.id.text)

        findViewById<TextView>(R.id.btnPick).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
        findViewById<TextView>(R.id.btnShare).setOnClickListener { share() }

        // після повороту екрана повертаємо вибране фото
        savedInstanceState?.getParcelable(KEY_IMAGE, Uri::class.java)?.let { showImage(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_IMAGE, imageUri)
    }

    private fun showImage(uri: Uri) {
        imageUri = uri
        image.setImageURI(uri)
        hint.visibility = TextView.GONE
    }

    // кнопка «Поширити»: фото + текст в одному Intent, система показує вибір додатка
    private fun share() {
        val uri = imageUri
        if (uri == null) {
            Toast.makeText(this, R.string.no_image, Toast.LENGTH_SHORT).show()
            return
        }

        // копіюємо фото у свій кеш і віддаємо через FileProvider,
        // щоб Telegram та інші додатки точно мали до нього доступ
        val file = File(cacheDir, "shared/image.jpg").apply { parentFile?.mkdirs() }
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val shareUri = FileProvider.getUriForFile(this, "$packageName.files", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_TEXT, text.text.toString())
            clipData = ClipData.newRawUri(null, shareUri) // мініатюра фото в діалозі вибору
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.choose_app)))
    }

    companion object {
        private const val KEY_IMAGE = "image"
    }
}
