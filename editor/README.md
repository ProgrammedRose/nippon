# Schedule Editor - GUI Module

Графический интерфейс для редактирования расписания на JavaFX + Scala 3.

## Структура проекта

```
editor/
├── src/main/scala/editor/
│   ├── ui/
│   │   ├── Main.scala              # Точка входа приложения
│   │   ├── MainWindow.scala        # Стартовый экран (FR1)
│   │   ├── EditorScreen.scala      # Редактор расписания (FR2)
│   │   ├── FormatScreen.scala      # Выбор формата (FR3)
│   │   ├── StyleScreen.scala       # Выбор стиля (FR4)
│   │   ├── DoneScreen.scala        # Экран готовности (FR6)
│   │   └── AppController.scala     # Контроллер навигации
│   │
│   ├── model/
│   │   └── ScheduleModel.scala     # Доменные типы
│   │       - Lesson
│   │       - DayBlock
│   │       - Schedule
│   │       - ScheduleMetadata
│   │
│   ├── validation/
│   │   └── Validator.scala         # Валидация (FR2.4, чистые функции)
│   │       - validateLesson
│   │       - validateDayBlock
│   │       - validateSchedule
│   │       - validateTimeFormat
│   │       - validateTimeRange
│   │
│   └── serialization/
│       ├── JsonEncoder.scala       # Сериализация → JSON (FR2.5)
│       │   - encodeSchedule
│       │   - saveScheduleToFile
│       └── JsonDecoder.scala       # Десериализация ← JSON
│           - loadScheduleFromFile
│           - parseScheduleJson
```

## Функциональные требования

### FR1: Main Window (Главное окно)
- Кнопка "New Schedule" → переход к EditorScreen
- Кнопка "Open Schedule" → FileChooser для открытия JSON

### FR2: Editor Screen (Редактор)
- Ввод названия расписания
- Управление день-блоками (DayBlock)
- Добавление/редактирование/удаление уроков (Lesson)
- Валидация данных в реальном времени
- Кнопка "Save Schedule" → переход на FormatScreen

### FR3: Format Screen (Выбор формата)
- RadioButton: HTML, SVG, PDF
- Навигация Next/Back

### FR4: Style Screen (Выбор стиля)
- ComboBox: Theme (Light/Dark/Modern/Classic)
- ComboBox: Color Scheme (Blue/Green/Red/Purple/Orange)
- ComboBox: Font Size (Small/Normal/Large)

### FR5: Done Screen (Завершение)
- Preview расписания
- Кнопка "Save to File" → сохранение JSON + возврат на Main
- Кнопка "Export" → экспорт в выбранный формат (интеграция с generator)
- Кнопка "Edit Again" → возврат к EditorScreen

## Модель данных

```scala
case class Lesson(
  name: String,
  startTime: String,      // "HH:mm"
  endTime: String,        // "HH:mm"
  room: String = "",
  teacher: String = "",
  notes: String = ""
)

case class DayBlock(
  day: String,            // "Monday", "Tuesday", ...
  lessons: List[Lesson]
)

case class Schedule(
  title: String,
  description: String = "",
  dayBlocks: List[DayBlock],
  metadata: ScheduleMetadata = ScheduleMetadata()
)

case class ScheduleMetadata(
  createdDate: String = "",
  lastModified: String = "",
  author: String = "",
  version: String = "1.0"
)
```

## Валидация

Все валидации возвращают `Either[ValidationError, T]`:

```scala
// Чистые функции, без побочных эффектов
Validator.validateLesson(lesson)       // Either[ValidationError, Lesson]
Validator.validateDayBlock(dayBlock)   // Either[ValidationError, DayBlock]
Validator.validateSchedule(schedule)   // Either[ValidationError, Schedule]
```

## Сериализация

### Сохранение в JSON
```scala
val schedule = Schedule("My Schedule", "", dayBlocks)
JsonEncoder.saveScheduleToFile(schedule, "schedule.json")
```

### Загрузка из JSON
```scala
JsonDecoder.loadScheduleFromFile("schedule.json")
```

## Контроллер приложения

`AppController` управляет навигацией между экранами и состоянием:

```scala
val controller = new AppController(primaryStage)
controller.start()
```

Обработчики событий связывают UI с логикой:
- MainWindow → EditorScreen
- EditorScreen → FormatScreen
- FormatScreen → StyleScreen
- StyleScreen → DoneScreen
- DoneScreen → сохранение или возврат

## Сборка и запуск

### Требования
- Scala 3.3.7
- SBT 1.x
- JavaFX 21.0.2

### Сборка
```bash
cd editor
sbt compile
```

### Запуск
```bash
sbt run
```

### Дополнительные ресурсы для IDEA/VSCode
Убедитесь, что в вашей IDE указан путь к JavaFX SDK:
- macOS/Linux: `/path/to/javafx-sdk/lib`
- Windows: `C:\path\to\javafx-sdk\lib`

## Интеграция с generator

Модуль `editor` сохраняет расписание в JSON. Модуль `generator` читает JSON и создает HTML/SVG.

Поток данных:
```
Editor UI → Schedule Model → JSON → JsonDecoder → Generator → HTML/SVG
```

## Будущие улучшения

- [ ] Поддержка импорта из других форматов (CSV, Excel)
- [ ] Предпросмотр в реальном времени
- [ ] Горячие клавиши для быстрого редактирования
- [ ] История изменений (undo/redo)
- [ ] Темы оформления (dragging into codebase)
- [ ] Синхронизация с облачными сервисами
