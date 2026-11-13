package com.example.android_proj

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // поле активіті з типом Кішка
    private var cat = Cat(name = "Мурка", age = 2, weight = 3.5)

    private lateinit var nameText: TextView
    private lateinit var ageText: TextView
    private lateinit var weightText: TextView

    private val names = listOf("Мурка", "Барсик", "Пушок", "Сніжок", "Рижик", "Масяня", "Тигра", "Люся")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val root = findViewById<android.view.View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        nameText = findViewById(R.id.catName)
        ageText = findViewById(R.id.catAge)
        weightText = findViewById(R.id.catWeight)

        // налаштовуємо стан кота кнопками
        findViewById<TextView>(R.id.btnRename).setOnClickListener {
            cat.name = names.filter { it != cat.name }.random()
            render()
        }
        findViewById<TextView>(R.id.btnBirthday).setOnClickListener {
            cat.age = cat.age + 1
            render()
        }
        findViewById<TextView>(R.id.btnFeed).setOnClickListener {
            cat.weight = cat.weight + 0.1
            render()
        }
        findViewById<TextView>(R.id.btnReset).setOnClickListener {
            cat = Cat(name = "Мурка", age = 2, weight = 3.5)
            render()
        }

        render()
    }

    /** Зберігаємо стан кота перед тим, як активіті буде знищене (наприклад, при повороті). */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_NAME, cat.name)
        outState.putInt(KEY_AGE, cat.age)
        outState.putDouble(KEY_WEIGHT, cat.weight)
        Log.d(TAG, "onSaveInstanceState: $cat")
    }

    /** Відновлюємо кота в новому екземплярі активіті. */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        cat = Cat(
            name = savedInstanceState.getString(KEY_NAME, "Мурка"),
            age = savedInstanceState.getInt(KEY_AGE, 2),
            weight = savedInstanceState.getDouble(KEY_WEIGHT, 3.5),
        )
        Log.d(TAG, "onRestoreInstanceState: $cat")
        render()
    }

    private fun render() {
        nameText.text = cat.name
        ageText.text = resources.getQuantityString(R.plurals.years, cat.age, cat.age)
        weightText.text = String.format(Locale.US, "%.1f кг", cat.weight)
    }

    companion object {
        private const val TAG = "CatState"
        private const val KEY_NAME = "cat_name"
        private const val KEY_AGE = "cat_age"
        private const val KEY_WEIGHT = "cat_weight"
    }
}
