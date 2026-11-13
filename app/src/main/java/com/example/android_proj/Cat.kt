package com.example.android_proj

import kotlin.math.roundToInt

/**
 * Кіт: три поля, конструктор, геттери та сеттери.
 * Сеттери перевіряють значення, тому кіт не може мати порожнє ім'я чи від'ємний вік.
 */
class Cat(name: String, age: Int, weight: Double) {

    var name: String = name
        get() = field
        set(value) {
            field = value.trim().ifEmpty { "Безіменний" }
        }

    /** Вік у роках. */
    var age: Int = age
        get() = field
        set(value) {
            field = value.coerceIn(0, 30)
        }

    /** Вага в кілограмах. */
    var weight: Double = weight
        get() = field
        set(value) {
            // округлюємо до 0.1 кг, щоб 3.5 + 0.1 + 0.1 не перетворювалося на 3.7000000000000006
            field = (value.coerceIn(0.5, 15.0) * 10).roundToInt() / 10.0
        }

    init {
        // пропускаємо значення з конструктора через сеттери, щоб перевірка працювала і тут
        this.name = name
        this.age = age
        this.weight = weight
    }

    override fun toString() = "Cat(name=$name, age=$age, weight=$weight)"
}
