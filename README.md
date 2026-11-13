# Практика 1 — збереження стану активіті при повороті

Завдання: маленький клас (Кіт) з 2–3 полями, геттерами, сеттерами та конструктором; поле з типом `Cat` в активіті;
при повороті екрана стан кота має зберігатися (`onSaveInstanceState` / `onRestoreInstanceState`).

## Рішення

- [`Cat.kt`](app/src/main/java/com/example/android_proj/Cat.kt) — `class Cat(var name, var age, var weight)`.
  Для кожного `var` Kotlin сам створює геттер і сеттер. Клас позначено `@Parcelize`, тобто він `Parcelable`.
- [`MainActivity.kt`](app/src/main/java/com/example/android_proj/MainActivity.kt) — поле `cat`, три кнопки змінюють
  вагу, вік та ім'я. `onSaveInstanceState` кладе кота в `Bundle` через `putParcelable`, `onRestoreInstanceState`
  дістає його через `getParcelable`.

Змінюєш стан кнопками, повертаєш телефон — активіті перестворюється, а кіт лишається тим самим.

## Запуск

Android Studio → **Run** (Android 13 / API 33+), або `./gradlew installDebug` (JDK 21).
