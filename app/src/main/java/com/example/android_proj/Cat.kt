package com.example.android_proj

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Кіт: конструктор з трьома полями.
 * Для кожного `var` Kotlin сам створює геттер і сеттер (getName() / setName() і т.д.).
 * @Parcelize генерує реалізацію Parcelable, тому кота можна покласти в Bundle цілим об'єктом.
 */
@Parcelize
class Cat(
    var name: String,
    var age: Int,
    var weight: Double,
) : Parcelable
