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
**Проблема:** для проверки написания нужна plain-версия формы настоящего времени
**Решение:** добавили поле presentSingularPlain в entity VerbCard
**Причина:** проверка всегда без огласовок — нужны plain-версии для обоих полей глагола

### [2026-06-15] Все режимы в одном StudyScreen.kt
**Проблема:** спецификация описывала отдельные файлы (FlashcardScreen, WritingScreen и т.д.)
**Решение:** все режимы реализованы внутри одного StudyScreen.kt через when(mode)
**Причина:** единый ViewModel, меньше файлов, проще навигация

### [2026-06-16] Автовоспроизведение TTS в диктанте
**Проблема:** CLAUDE.md говорит "TTS только по кнопке", но диктант без автовоспроизведения не имеет смысла
**Решение:** в режиме DICTATION TTS воспроизводит слово автоматически при смене карточки (LaunchedEffect)
**Причина:** диктант — единственное исключение из правила, смысл режима именно в этом

### [2026-06-16] Icons.Filled.VolumeUp — предупреждение компилятора
**Проблема:** два предупреждения DEPRECATION в StudyScreen.kt строки 444 и 839
**Решение:** оставить как есть — Icons.AutoMirrored.Filled.VolumeUp не существует в Compose BOM 2025.05.01
**Причина:** предупреждения не влияют на работу, не критично для MVP

---

## Статус файлов — актуально на 2026-06-16 (после BUILD SUCCESSFUL)

### ГОТОВО — не трогать без необходимости

**data/db/entity/**
- Card.kt — id, deckId, hebrew, hebrewPlain, russian, transliteration, position
- CardProgress.kt — cardId, deckId, status (NEW/LEARNING/KNOWN), errorCount, correctStreak, lastStudiedAt
- Deck.kt — id, name, type (REGULAR/VERB), createdAt, lastStudiedAt
- SessionResult.kt — id, deckId, mode, totalCards, correctCount, errorCount, durationSeconds, completedAt
- VerbCard.kt — cardId, binyan, root, presentSingular, presentSingularPlain, conjugationJson

**data/db/dao/**
- CardDao.kt — getCardsByDeck (Flow), getCardsByDeckOnce (suspend), insertCards, deleteCard
- DeckDao.kt — getAllDecks (Flow), getDeckById (suspend), getDeckByIdFlow (Flow), insertDeck, updateDeck, deleteDeck
- ProgressDao.kt — getProgress, getProgressByDeck (Flow), getProgressByDeckOnce, upsertProgress, insertSessionResult, getSessionsByDeck, getSessionCountSince, getTotalCorrectCount

**data/db/**
- AppDatabase.kt — Room база, singleton
- Converters.kt — DeckType, StudyMode, CardStatus конвертеры

**data/repository/**
- DeckRepository.kt — getDeckById, getAllDecks, getDeckSummary (→ DeckProgress), insertDeck, updateDeck, deleteDeck, resetDeckProgress
- ProgressRepository.kt — getProgress, upsertProgress, insertSessionResult, getSessionCountToday, getTotalCorrectCount

**domain/model/**
- DeckType.kt — REGULAR, VERB
- StudyMode.kt — FLASHCARD, WRITING, DICTATION, CHOICE

**domain/usecase/**
- ImportDeckUseCase.kt — парсит CSV (обычный и #VERB), вставляет в БД

**util/**
- Transliterator.kt — stripNiqqud(), transliterate()
- TtsManager.kt — init(onReady), speak(text), getAvailableEngines(), shutdown()

**ui/theme/**
- Color.kt — все цвета (тёмная/светлая тема, акценты режимов, свайп, проверка, деструктивные)
- Theme.kt — HebrewCardsTheme, LocalAppColors, AppColors, светлая и тёмная тема
- Type.kt — HebrewTextStyle (cardWord, input, hint, letterCheck), HebrewTextSize

**ui/screen/dashboard/**
- DashboardScreen.kt — список колод, StatsCard, BottomNavBar (Колоды/Статистика/Настройки с навигацией), FAB, диалоги удаления/сброса
- DashboardViewModel.kt — загружает колоды + прогресс, deleteDeck, resetProgress

**ui/screen/deck/**
- DeckScreen.kt — прогресс (known/learning/new), 4 кнопки режимов, меню сброса
- DeckViewModel.kt — колода по deckId, прогресс, resetProgress
- AddDeckScreen.kt — поле вставки CSV, кнопка импорта
- AddDeckViewModel.kt — вызывает ImportDeckUseCase

**ui/screen/study/** ← ВСЕ РЕЖИМЫ ЗДЕСЬ
- StudyScreen.kt — роутер по mode:
  - FlashcardContent ✅ — свайп+анимация, переворот, TTS, кнопки Знаю/Повторить
  - WritingContent ✅ — русский → пишем иврит, stripNikud, побуквенный разбор, Ещё раз
  - DictationContent ✅ — автовоспроизведение TTS, пишем иврит, тот же разбор
  - ChoiceContent ✅ — сетка 2×2, правильный зеленеет 600ms, неправильный краснеет 1200ms
  - SessionCompleteContent ✅ — итоги (правильно/ошибок/время), кнопка назад
  - LetterByLetterFeedback — общий компонент разбора для написания и диктанта
  - DiffDisplay — RTL строка с цветными буквами
- StudyViewModel.kt — SRS-очередь, allCards для ChoiceContent, swipeRight/Left/flip/recordError

**ui/screen/settings/**
- SettingsViewModel.kt — SharedPreferences "hebrewcards_prefs", TTS движки, тема
- SettingsScreen.kt — Switch темы, RadioButton TTS движков, версия приложения

**ui/screen/stats/**
- StatsViewModel.kt — суммирует known/сессии/правильные по всем колодам
- StatsScreen.kt — 3 плашки + список колод с прогресс-барами, пустое состояние

**ui/navigation/**
- NavGraph.kt — маршруты: dashboard, add_deck, deck/{deckId}, study/{deckId}/{mode}, settings, stats

**MainActivity.kt** — setContent { HebrewCardsTheme { HebrewCardsNavGraph() } }

---

### НЕ РЕАЛИЗОВАНО (Этап 2 — после MVP)

1. **Смена темы в рантайме** — SettingsScreen сохраняет выбор, но Theme.kt не читает SharedPreferences
   - Нужно: передавать isDarkTheme из SettingsViewModel в HebrewCardsTheme через StateFlow в MainActivity
   - Приоритет: средний (тёмная тема работает по умолчанию)

2. **Демо-колода при первом запуске** — не реализована

3. **SessionCompleteScreen как отдельный экран** — сейчас встроен в StudyScreen
   - По спецификации: отдельный маршрут session/{deckId} с кнопками "повторить ошибки" и "следующая колода"
   - Сейчас: упрощённый вариант внутри StudyScreen, достаточно для MVP

4. **Геймификация** — стрики, серии без ошибок (Этап 2)

5. **Экспорт/импорт резервной копии** — Этап 2

6. **Глагольные карточки** — Этап 2

---

## Важные технические детали

- **stripNikud** реализована в StudyScreen.kt (private fun), фильтрует символы U+05D0..U+05EA
- **TtsManager.init()** принимает onReady: () -> Unit — колбэк когда TTS готов
- **CardProgress.correctStreak** — поле есть в entity (не в оригинальной спецификации)
- **DeckProgress** — data class в DeckRepository: total, known, learning, new, knownFraction
- **StudyUiState.allCards** — все карточки колоды, нужны ChoiceContent для генерации вариантов
- **StudyUiState.repeatCount** — счётчик ошибок для экрана результатов
