# GitHub Release Tracker (Android)

Простое Android-приложение для отслеживания новых релизов в любых публичных репозиториях GitHub.

## Возможности
- Добавление репозиториев (owner/repo).
- Проверка обновлений через Atom-ленту GitHub (`/releases.atom`), **без API-токена**.
- Просмотр списка релизов и текста изменений (changelog).
- Прямая ссылка на скачивание APK из ассетов релиза.
- Локальные уведомления о новых релизах.
- Переключение языка интерфейса (RU / EN).

## Как собрать APK (без Android Studio)

1. Создайте новый репозиторий на GitHub.
2. Загрузите в него все файлы из этого архива (сохраняя структуру папок).
3. Перейдите в раздел **Actions** вашего репозитория.
4. Слева выберите workflow **Build Android APK**.
5. Нажмите **Run workflow** → **Run workflow** (зелёная кнопка).
6. Дождитесь окончания сборки (5–10 минут).
7. Скачайте APK из раздела **Artifacts** (внизу страницы запуска workflow).

> APK будет собран в debug-варианте. Для установки на телефон разрешите «Установку из неизвестных источников».

## Структура проекта
```

app/
build.gradle.kts
src/main/
AndroidManifest.xml
java/com/example/releasetracker/
MainActivity.kt
...
res/
values/strings.xml
values-ru/strings.xml
...
.github/workflows/build.yml
build.gradle.kts
settings.gradle.kts
gradle.properties

```

