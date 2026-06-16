# HebrewCards — DECISIONS.md

Технические решения, принятые в процессе разработки.

---

## Формат записи

```
### [ДАТА] Название решения
**Проблема:** что не было ясно из спецификации
**Решение:** что решили
**Причина:** почему именно так
```

---

## Решения

### [2026-06-15] Версии зависимостей
**Решение:** AGP 8.10.1, Kotlin 2.1.0, KSP 2.1.0-1.0.29, Compose BOM 2025.05.01, Room 2.7.1, targetSdk 35, Java 21
**Причина:** Android Studio Quail 2026.1.1 — берём актуальные стабильные версии

### [2026-06-15] Пакет приложения
**Решение:** com.hebrewcards
**Причина:** подтверждено владельцем

### [2026-06-15] Рабочая папка проекта
**Решение:** C:\Users\Govinda\AndroidStudioProjects\HebrewCards2
**Причина:** Android Studio создаёт gradlew и wrapper автоматически — нужен был шаблон от IDE

### [2026-06-15] presentSingularPlain в VerbCard
**Решение:** добавили поле presentSingularPlain в entity VerbCard
**Причина:** проверка всегда без огласовок

### [2026-06-15] Все режимы в одном StudyScreen.kt
**Решение:** все режимы внутри одного файла через when(mode)
**Причина:** единый ViewModel, меньше файлов

### [2026-06-16] Автовоспроизведение TTS в диктанте
**Решение:** LaunchedEffect в DictationContent
**Причина:** единственное исключение из правила "TTS только по кнопке"

### [2026-06-16] Icons.Filled.VolumeUp — предупреждение компилятора
**Решение:** оставить как есть, не критично

### [2026-06-16] GitHub репозиторий
**Решение:** https://github.com/Elisei108/HebrewCards.git, ветка main

### [2026-06-16] buildConfig = true в app/build.gradle.kts
**Причина:** в новых версиях AGP BuildConfig отключён по умолчанию

### [2026-06-16] StatsScreen переиспользует DashboardViewModel
**Решение:** StatsScreen принимает vm: DashboardViewModel = viewModel()

### [2026-06-16] Смена темы через колбэк из MainActivity
**Решение:** isDarkTheme в mutableStateOf, onThemeChange через NavGraph → SettingsScreen

### [2026-06-16] TtsManager.reinit() для смены движка
**Решение:** reinit(enginePackage) — закрывает старый TTS, инициализирует новый

### [2026-06-16] Скорость TTS через setSpeed()
**Решение:** TtsManager.speechRate: Float, setSpeed(rate), speak() использует speechRate
**Ключ SharedPreferences:** "tts_speed" (Float, default 1.0f)

### [2026-06-16] Демо-колода при первом запуске
**Решение:** FirstLaunchManager проверяет флаг в SharedPreferences; при первом запуске автоматически импортирует встроенную колоду «Ульпан алеф · урок 1» (15 слов)
**Ключ SharedPreferences:** "first_launch_done" (Boolean)
**Коммит:** f92aefd

### [2026-06-16] Ручное добавление карточек и импорт из файла
**Решение:** В AddDeckScreen три способа: CSV-вставка (существующий), "Вручную" (ModalBottomSheet с парами иврит/перевод), "Из файла" (системный файловый пикер ACTION_OPEN_DOCUMENT)
**Ручное добавление:** ManualCardPair(id, hebrew, russian) в UiState; saveManualDeck() конвертирует пары в CSV и вызывает тот же ImportDeckUseCase
**Импорт из файла:** importFromFile(fileName, fileContent) — название колоды из имени файла без .csv
**Шрифт поля иврита:** HeeboFontFamily, textAlign = Right, singleLine

### [2026-06-16] Количество слов за сессию
**Решение:** SharedPreferences ключ "session_size" (Int, default 0 = все); StudyViewModel читает при loadQueue() и обрезает очередь через queue.take(sessionSize)
**Значения:** 0 = все, 10 = десять, 20 = двадцать

---

## Рабочий процесс с git

```
Set-Location "C:\Users\Govinda\AndroidStudioProjects\HebrewCards2"
git add .
git commit -m "описание что сделано"
git push
```

---

## Статус файлов — актуально на 2026-06-16

### ГОТОВО — не трогать без необходимости

**data/** — все entity, DAO, репозитории — без изменений

**util/**
- Transliterator.kt — stripNiqqud(), transliterate()
- TtsManager.kt ✅ — init(onReady), reinit(enginePackage), speak(text), setSpeed(rate), getAvailableEngines(), shutdown()

**ui/theme/** — Color.kt, Theme.kt(darkTheme: Boolean), Type.kt

**ui/screen/dashboard/** — без изменений

**ui/screen/deck/** — без изменений

**ui/screen/study/**
- StudyScreen.kt ✅ — все 4 режима + SessionCompleteContent
- StudyViewModel.kt ✅ — SRS, allCards, session_size из prefs, swipeRight/Left/flip/recordError

**ui/screen/settings/**
- SettingsScreen.kt ✅ — 4 карточки:
  - Отображение: Switch темы
  - Звук: RadioButton движков + RadioButton скорости (Нормально/Медленно)
  - Обучение: RadioButton количества слов (10/20/Все)
  - О приложении: версия
- параметры: onThemeChange, onEngineChange, onSpeedChange, onSessionSizeChange

**ui/screen/stats/**
- StatsScreen.kt ✅ — 3 плашки + список колод

**ui/navigation/**
- NavGraph.kt ✅ — параметры: onThemeChange, onEngineChange, onSpeedChange, onSessionSizeChange

**MainActivity.kt** ✅ — все колбэки подключены

**app/build.gradle.kts** — buildConfig = true

---

### НЕ РЕАЛИЗОВАНО (очередь)

1. ~~**Демо-колода при первом запуске**~~ ✅ коммит f92aefd
2. ~~**Ручное добавление + импорт из файла**~~ ✅ коммит 424f204
3. **Геймификация** — 🔥 и ⚡ на дашборде показывают "—"
4. **Экспорт/импорт резервной копии**
5. **Глагольные карточки**

---

## Важные технические детали

- **SharedPreferences файл:** "hebrewcards_prefs"
  - "is_dark_theme" (Boolean, default true)
  - "tts_engine" (String, default "")
  - "tts_speed" (Float, default 1.0f) — медленно=0.7f, нормально=1.0f
  - "session_size" (Int, default 0) — 0=все, 10=десять, 20=двадцать
- **stripNikud** — private fun в StudyScreen.kt, фильтрует U+05D0..U+05EA
- **buildOptions** — private fun в StudyScreen.kt, 3 дистрактора для ChoiceContent
- **CardProgress.correctStreak** — поле есть в entity
- **DeckProgress** — data class в DeckRepository: total, known, learning, new, knownFraction
- **StudyUiState.allCards** — все карточки колоды для ChoiceContent
- **BuildConfig** — работает только с buildConfig = true в app/build.gradle.kts
