# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

# HebrewCards — CLAUDE.md

## Главный промпт

Android-приложение для изучения ивритской лексики.
Владелец — Евгений, русскоязычный, изучает иврит на уровне ульпан в Израиле.

## Команды сборки и тестирования

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew test                   # Unit-тесты (JVM)
./gradlew connectedAndroidTest   # Instrumented-тесты (нужен эмулятор/устройство)
./gradlew clean                  # Очистка
```

Тесты сейчас — только заглушки (ExampleUnitTest, ExampleInstrumentedTest). Реальные тесты не написаны, мокирующих библиотек нет.

## Жёсткие правила
- Kotlin + Jetpack Compose, никакого XML layout
- Архитектура строго MVVM — логика не в Composable функциях
- Иврит везде шрифтом Heebo, направление RTL
- Огласовки только для отображения — для проверки используем `hebrewPlain`
- Комментарии в коде на русском языке
- Перед созданием файла — проверь не существует ли он уже

## Технический стек
- Kotlin 2.1.0, AGP 8.10.1
- Jetpack Compose BOM 2025.05.01
- Room 2.7.1 (KSP 2.1.0-1.0.29)
- Navigation Compose 2.9.0
- DataStore Preferences 1.1.4 (настройки пользователя)
- minSdk 26, targetSdk 35, Java 21
- Пакет: com.hebrewcards
- **DI: без фреймворка** — ручная инициализация через `AppDatabase.getInstance(context)`; репозитории создаются внутри ViewModel

## Структура проекта
```
app/src/main/java/com/hebrewcards/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt           # singleton, fallbackToDestructiveMigration (v1)
│   │   ├── Converters.kt            # enum-конвертеры для Room
│   │   ├── dao/ (DeckDao, CardDao, ProgressDao)
│   │   └── entity/ (Deck, Card, VerbCard, CardProgress, SessionResult)
│   └── repository/ (DeckRepository, ProgressRepository)
├── domain/
│   ├── model/ (DeckType, StudyMode)
│   └── usecase/ (ImportDeckUseCase)  # CSV parsing → DB insert
├── ui/
│   ├── theme/ (Color, Type, Theme)
│   ├── navigation/ (NavGraph)        # маршруты: DASHBOARD|ADD_DECK|DECK|STUDY|SESSION|SETTINGS|STATS
│   └── screen/
│       ├── dashboard/ (DashboardScreen, DashboardViewModel)
│       ├── deck/ (AddDeckScreen, AddDeckViewModel, DeckScreen — заглушка)
│       ├── study/    — не реализовано
│       ├── session/  — не реализовано
│       ├── settings/ — не реализовано
│       └── stats/    — не реализовано
└── util/
    ├── Transliterator.kt             # Hebrew → Latin, удаление никуд
    └── TtsManager.kt                 # TTS, локаль he-IL, кастомные движки
```

## База данных — сущности

### Deck
- id, name, type (REGULAR|VERB), createdAt, lastStudiedAt

### Card
- id, deckId, hebrew (с огласовками), hebrewPlain (без), russian, transliteration, position

### VerbCard (FK → Card)
- cardId, binyan, root, presentSingular, presentSingularPlain, conjugationJson

### CardProgress (FK → Card)
- cardId, deckId, status (NEW|LEARNING|KNOWN), errorCount, correctStreak, lastStudiedAt

### SessionResult (FK → Deck)
- id, deckId, mode, totalCards, correctCount, errorCount, durationSeconds, completedAt

## Дизайн-система

### Цвета (тёмная тема — основная)
```
Фон:               #1C1C1E  (DarkBackground)
Поверхность:       #2A2A2E  (DarkSurface)
Поверхность 2:     #252528  (DarkSurface2)
Граница:           #3A3A3E  (DarkBorder)
Текст основной:    #F0F0F0
Текст вторичный:   #888888
```

### Акценты
```
Флипкарты:   #4CAF50  (ColorFlashcard) — зелёный
Написание:   #5AABF0  (ColorWriting)   — синий
Диктант:     #A78BFA  (ColorDictation) — фиолетовый
Выбор из 4:  #FFC107  (ColorChoice)    — жёлтый
Удалить:     #FF453A  (ColorDestructive)
```

Цвета доступны через `LocalAppColors` — CompositionLocal в `ui/theme/Theme.kt`.

### Шрифты
- Интерфейс: системный (Roboto)
- Иврит: Heebo (heebo_regular.ttf, heebo_medium.ttf в res/font/)
- Размеры ивритского текста: Small 28sp, Medium 36sp, Large 46sp

### Скругления
- Карточки: 18dp, Кнопки: 14dp

## Алгоритм обучения (SRS)
```
NEW → показываем в режиме "выбор из 4"
    → правильно: LEARNING
    → неправильно: остаётся NEW

LEARNING → основные режимы
         → N правильных подряд (default 3): KNOWN
         → неправильно: остаётся LEARNING

KNOWN → иногда появляется (каждые ~10 карточек 1 KNOWN)
      → неправильно: обратно LEARNING
```

SRS без дат — только статусы (простота > сложность).

## Текущий статус (Этап 1 — MVP)
✅ Room БД: все entity и DAO
✅ Импорт CSV (обычные и глагольные колоды)
✅ Дашборд — список колод, статистика, FAB
✅ Экран добавления колоды (вставка CSV текстом)
✅ Экран колоды (прогресс + выбор режима обучения)
✅ StudyScreen — флипкарты со свайпом, анимацией переворота, TTS, экран результатов
✅ Режим написания — поле ввода RTL/Heebo, побуквенный diff, транслитерация по кнопке
⬜ Завершение сессии
⬜ Базовые настройки

## Формат CSV

### Обычная колода
```
иврит_с_огласовками;перевод
שָׁלוֹם;мир, покой
```

### Глагольная колода
```
#VERB
לָלֶכֶת;идти;פָּעַל;ה-ל-כ;הוֹלֵךְ
```

## Важные решения — не менять
| Решение | Обоснование |
|---|---|
| hebrewPlain для проверки | Стандарт современного иврита |
| Свайп влево = жёлтый refresh | "Повторим" а не "провал" |
| TTS только по кнопке | Пользователь знает произношение |
| Не переходим при ошибке | Обучение через исправление |
| SRS без дат, только статусы | Простота > сложность |
| presentSingularPlain в VerbCard | Проверка всегда без огласовок |
| fallbackToDestructiveMigration | Версия 1, миграции не нужны пока |
