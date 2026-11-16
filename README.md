# Практика — стрічка карток товарів

Завдання: зробити стрічку з карток для товарів, зображення беруться з інтернету, по ширині екрана може бути
1 або 2 картки, всього карток ±10–15.

| 2 картки в ряд | 1 картка в ряд |
|:---:|:---:|
| ![2 колонки](screenshots/01_two_columns.png) | ![1 колонка](screenshots/02_one_column.png) |

## Рішення

- **14 товарів** ([`shop/Products.kt`](app/src/main/java/com/example/android_proj/shop/Products.kt)): назва, категорія,
  ціна, стара ціна, рейтинг, опис і посилання на фото.
- **Стрічка** ([`shop/FeedScreen.kt`](app/src/main/java/com/example/android_proj/shop/FeedScreen.kt)) —
  `LazyVerticalGrid` з `GridCells.Fixed(columns)`. Перемикач у шапці змінює `columns` між 1 і 2,
  картки плавно переставляються (`animateItem`), вибір зберігається при повороті (`rememberSaveable`).
- **Фото з інтернету** — Coil 3 `AsyncImage` (кешування, плавна поява). У режимі однієї картки фото ширше
  і з'являється опис товару.

Інтерфейс написано на Jetpack Compose (Kotlin), XML лише для теми вікна.

## Запуск

Android Studio → **Run** (Android 13 / API 33+, потрібен інтернет), або `./gradlew installDebug` (JDK 21).

Фото товарів — [Unsplash](https://unsplash.com) (Unsplash License). Назви товарів вигадані.
