# Практика — ListView + SimpleAdapter

Завдання: сформувати масив URL картинок, картинки вантажаться з інтернету
([Glide](https://github.com/bumptech/glide)), картинки круглі
([CircleImageView](https://github.com/hdodenhof/CircleImageView)), картинка відповідає тексту, 4–5 пунктів.
Основне — `SimpleAdapter`.

<img src="screenshots/01_list.png" width="300" alt="Список">

## Рішення

- Код — [`MainActivity.java`](app/src/main/java/com/example/android_proj/MainActivity.java) (Java).
- Масиви `images` (URL), `titles`, `descriptions`, `prices` → список `Map` → `SimpleAdapter`
  з розміткою пункту [`item_row.xml`](app/src/main/res/layout/item_row.xml).
- `SimpleAdapter` сам не вантажить картинки з URL, тому через `setViewBinder` для `CircleImageView`
  картинка завантажується Glide (`dontAnimate()`, бо CircleImageView не підтримує анімацію переходу).
- Дозвіл `INTERNET` у маніфесті.

## Запуск

Android Studio → **Run** (Android 13 / API 33+, потрібен інтернет), або `./gradlew installDebug` (JDK 21).

Фото — [Unsplash](https://unsplash.com) (Unsplash License).
