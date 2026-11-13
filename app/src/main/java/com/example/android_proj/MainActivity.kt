package com.example.android_proj

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // поле активіті з типом Кішка
    private var cat = Cat("Мурка", 2, 3.5)

    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        info = findViewById(R.id.catInfo)

        // налаштовуємо стан кота
        findViewById<TextView>(R.id.btnRename).setOnClickListener {
            cat.name = if (cat.name == "Мурка") "Барсик" else "Мурка"
            showCat()
        }
        findViewById<TextView>(R.id.btnBirthday).setOnClickListener {
            cat.age += 1
            showCat()
        }
        findViewById<TextView>(R.id.btnFeed).setOnClickListener {
            cat.weight += 0.5
            showCat()
        }

        showCat()
    }

    // перед поворотом кладемо кота в Bundle (Cat — Parcelable)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_CAT, cat)
    }

    // після повороту дістаємо його назад
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getParcelable(KEY_CAT, Cat::class.java)?.let { cat = it }
        showCat()
    }

    private fun showCat() {
        info.text = "Ім'я: ${cat.name}\nВік: ${cat.age}\nВага: ${cat.weight} кг"
    }

    companion object {
        private const val KEY_CAT = "cat"
    }
}
