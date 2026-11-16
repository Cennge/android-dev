package com.example.android_proj.shop

import androidx.compose.ui.graphics.Color
import java.util.Locale

/** Пункт списку бажань: фото + текстовий опис. */
data class Wish(
    val id: Int,
    val title: String,
    val description: String,
    val photo: String,
    val price: Int,
    val saved: Int,
    val priority: Priority,
) {
    val progress get() = (saved.toFloat() / price).coerceIn(0f, 1f)
}

enum class Priority(val label: String, val color: Color) {
    HIGH("Дуже хочу", Palette.Pink),
    MEDIUM("Хочу", Palette.Primary),
    LOW("Колись", Palette.Violet),
}

/** Фото: Unsplash (Unsplash License). */
private fun unsplash(id: String) = "https://images.unsplash.com/photo-$id?w=900&q=80&fm=jpg"

fun price(value: Int): String = String.format(Locale.US, "%,d", value).replace(',', ' ') + " ₴"

object Wishlist {
    val wishes = listOf(
        Wish(
            1, "Бездротові навушники",
            "Повнорозмірні, з шумозаглушенням — щоб слухати музику в дорозі й не чути метро.",
            unsplash("1505740420928-5e560c06d30e"), 5299, 3900, Priority.HIGH,
        ),
        Wish(
            2, "Плівковий фотоапарат",
            "Компактна камера для подорожей: живі кольори, зерно і жодних фільтрів.",
            unsplash("1526170375885-4d8ecf77b99f"), 4199, 1500, Priority.MEDIUM,
        ),
        Wish(
            3, "Розумний годинник",
            "Кроки, сон, пульс і сповіщення на зап'ясті. Мотивація більше рухатися.",
            unsplash("1546868871-7041f2a55e12"), 8999, 2200, Priority.MEDIUM,
        ),
        Wish(
            4, "Нові кросівки",
            "Яскраві та легкі — для ранкових пробіжок і довгих прогулянок містом.",
            unsplash("1542291026-7eec264c27ff"), 3499, 3499, Priority.HIGH,
        ),
        Wish(
            5, "Міський рюкзак",
            "Місткий, з відділом для ноутбука — на пари, в поїздки й на вихідні.",
            unsplash("1553062407-98eeb64c6a62"), 2299, 400, Priority.LOW,
        ),
    )
}
