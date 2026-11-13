package com.example.android_proj.shop

import androidx.compose.ui.graphics.Color
import java.util.Locale

enum class Category(val label: String, val emoji: String) {
    ALL("Усі", "✨"),
    SHOES("Взуття", "👟"),
    AUDIO("Аудіо", "🎧"),
    GADGETS("Гаджети", "⌚"),
    CLOTHES("Одяг", "👕"),
    ACCESSORIES("Аксесуари", "🕶️"),
}

data class Product(
    val id: Int,
    val name: String,
    val category: Category,
    val price: Int,
    val oldPrice: Int? = null,
    val rating: Float,
    val reviews: Int,
    val photo: String,
    val description: String,
    val colors: List<Color>,
    val sizes: List<String> = emptyList(),
) {
    val discount get() = oldPrice?.let { 100 - price * 100 / it }
}

data class Promo(val tag: String, val title: String, val subtitle: String, val colors: List<Color>, val photo: String)

/** Product photos: Unsplash (Unsplash License). */
private fun unsplash(id: String) = "https://images.unsplash.com/photo-$id?w=700&q=80&fm=jpg"

fun price(value: Int): String = String.format(Locale.US, "%,d", value).replace(',', ' ') + " ₴"

object Catalog {
    private val shoeSizes = listOf("39", "40", "41", "42", "43", "44")
    private val clothesSizes = listOf("XS", "S", "M", "L", "XL")

    val products = listOf(
        Product(
            1, "Flame Runner", Category.SHOES, 3499, 4699, 4.9f, 812, unsplash("1542291026-7eec264c27ff"),
            "Легкі бігові кросівки з дихаючим трикотажним верхом і пружною піною, що повертає енергію на кожному кроці.",
            listOf(Color(0xFFD7263D), Color(0xFF1B1B1F), Color(0xFFF2F2F2)), shoeSizes,
        ),
        Product(
            2, "Studio Max", Category.AUDIO, 5299, null, 4.8f, 1204, unsplash("1505740420928-5e560c06d30e"),
            "Повнорозмірні навушники з активним шумозаглушенням, 40 годинами роботи та м'якими амбушурами з піною пам'яті.",
            listOf(Color(0xFF1B1B1F), Color(0xFFE8E2D6)),
        ),
        Product(
            3, "Orbit Watch", Category.GADGETS, 8999, 10999, 4.7f, 530, unsplash("1546868871-7041f2a55e12"),
            "Розумний годинник з яскравим AMOLED-екраном, пульсометром, GPS і захистом від води до 50 метрів.",
            listOf(Color(0xFF1B1B1F), Color(0xFFB8BCC4), Color(0xFFE0B48A)),
        ),
        Product(
            4, "Snap One", Category.GADGETS, 4199, null, 4.6f, 298, unsplash("1526170375885-4d8ecf77b99f"),
            "Камера миттєвого друку: знімаєш — і за хвилину тримаєш у руках справжнє фото. Вбудований спалах і автофокус.",
            listOf(Color(0xFFF2F2F2), Color(0xFF1B1B1F)),
        ),
        Product(
            5, "Cloud Air", Category.SHOES, 3899, null, 4.8f, 967, unsplash("1600185365483-26d7a4cc7519"),
            "Повсякденні кросівки з повітряною подушкою в п'яті. Легкі, м'які й пасують до всього.",
            listOf(Color(0xFFF2F2F2), Color(0xFFFF7A59)), shoeSizes,
        ),
        Product(
            6, "Wayfarer Noir", Category.ACCESSORIES, 1899, 2499, 4.5f, 176, unsplash("1572635196237-14b3f281503f"),
            "Класичні сонцезахисні окуляри з поляризованими лінзами та захистом UV400.",
            listOf(Color(0xFF1B1B1F), Color(0xFF6B4F3A)),
        ),
        Product(
            7, "Bass Tube", Category.AUDIO, 2799, 3299, 4.7f, 642, unsplash("1608043152269-423dbba4e7e1"),
            "Портативна колонка з глибоким басом, захистом від води IP67 і 20 годинами музики без підзарядки.",
            listOf(Color(0xFF1B1B1F), Color(0xFF2E5AAC), Color(0xFFD7263D)),
        ),
        Product(
            8, "Volt Trainer", Category.SHOES, 4299, null, 4.9f, 388, unsplash("1606107557195-0e29a4b5b4aa"),
            "Тренувальні кросівки зі стабільною п'ятою та гнучкою підошвою для залу й функціональних тренувань.",
            listOf(Color(0xFFCDF33A), Color(0xFF1B1B1F)), shoeSizes,
        ),
        Product(
            9, "Fog Hoodie", Category.CLOTHES, 1999, 2599, 4.8f, 455, unsplash("1556821840-3a63f95609a7"),
            "Теплий худі оверсайз з щільного футеру з начосом. Глибокий капюшон і кишеня-кенгуру.",
            listOf(Color(0xFFB8BCC4), Color(0xFF1B1B1F), Color(0xFF3B4A3F)), clothesSizes,
        ),
        Product(
            10, "Pulse Mini", Category.GADGETS, 2499, null, 4.4f, 213, unsplash("1523275335684-37898b6baf30"),
            "Мінімалістичний фітнес-годинник: кроки, сон і сповіщення. Тиждень роботи від одного заряду.",
            listOf(Color(0xFFF2F2F2), Color(0xFF1B1B1F)),
        ),
        Product(
            11, "Core Tee", Category.CLOTHES, 799, null, 4.6f, 1530, unsplash("1618354691373-d851c5c3a990"),
            "Базова футболка зі 100% органічної бавовни. Щільна, не втрачає форму після прання.",
            listOf(Color(0xFF1B1B1F), Color(0xFFF2F2F2)), clothesSizes,
        ),
        Product(
            12, "Shadow Knit", Category.SHOES, 3199, 3999, 4.5f, 274, unsplash("1491553895911-0055eca6402d"),
            "Чорні кросівки з трикотажним верхом без швів. Легко надягаються і тримають стопу як шкарпетка.",
            listOf(Color(0xFF1B1B1F), Color(0xFF4A4F57)), shoeSizes,
        ),
        Product(
            13, "Echo Wire", Category.AUDIO, 1299, null, 4.3f, 389, unsplash("1583394838336-acd977736f90"),
            "Дротові навушники з чистим звуком і складною конструкцією — зручно брати з собою.",
            listOf(Color(0xFF4A4F57), Color(0xFF1B1B1F)),
        ),
        Product(
            14, "Urban Pack", Category.ACCESSORIES, 2299, null, 4.7f, 507, unsplash("1553062407-98eeb64c6a62"),
            "Міський рюкзак на 22 літри з відділенням для ноутбука 16\" та водовідштовхувальною тканиною.",
            listOf(Color(0xFF1F2A44), Color(0xFF1B1B1F), Color(0xFF3B4A3F)),
        ),
    )

    val hot = products.filter { it.discount != null }

    val promos = listOf(
        Promo(
            "Осінній розпродаж", "−30%", "на все взуття до неділі",
            listOf(Color(0xFFFB6C1A), Color(0xFFE5112F)), unsplash("1542291026-7eec264c27ff"),
        ),
        Promo(
            "Новинка", "Studio Max", "тиша, яку чутно",
            listOf(Color(0xFF7C3AED), Color(0xFF2E5AAC)), unsplash("1505740420928-5e560c06d30e"),
        ),
        Promo(
            "Тільки онлайн", "2 = 3", "третя річ з аксесуарів у подарунок",
            listOf(Color(0xFF0F9F6E), Color(0xFF155E75)), unsplash("1572635196237-14b3f281503f"),
        ),
    )
}
