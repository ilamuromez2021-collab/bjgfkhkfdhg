# AniWave — Android-приложение

Нативный Android-проект на Kotlin и Jetpack Compose (не WebView).

## Возможности
- Популярные, лучшие и сезонные аниме из Jikan API.
- Поиск, обложки, рейтинг, жанры и описание.
- Избранное сохраняется на устройстве.
- Переход к поиску доступного просмотра.
- Автоматическая сборка APK через GitHub Actions.

## Скачать APK
1. Откройте вкладку Actions.
2. Запустите **Build AniWave Android APK** через **Run workflow** или дождитесь завершения автоматической сборки.
3. Откройте успешный запуск и найдите Artifacts.
4. Скачайте ZIP **AniWave-Android-APK**, распакуйте и установите app-debug.apk на Android.

## Сборка локально
Нужны JDK 17 и Android SDK platform 35. В корне проекта выполните `gradle assembleDebug`. APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

API: https://docs.api.jikan.moe/. Для каталога нужно интернет-соединение. Видеофайлы не хостятся приложением.
