# Структура GraphFX GUI для редактирования расписания

## Обзор

Построена полная структура JavaFX приложения на Scala 3.3.7 для редактирования расписания студентов.

## Дерево файлов

```
editor/
├── README.md
├── src/main/scala/editor/
│   ├── ui/
│   │   ├── Main.scala                  # Точка входа, инициализирует AppController
│   │   ├── MainWindow.scala            # Стартовый экран (FR1)
│   │   ├── EditorScreen.scala          # Редактор расписания (FR2)
│   │   ├── FormatScreen.scala          # Выбор формата экспорта (FR3)
│   │   ├── StyleScreen.scala           # Выбор стиля оформления (FR4)
│   │   ├── DoneScreen.scala            # Экран готовности (FR6)
│   │   └── AppController.scala         # Контроллер - управляет переходами между экранами
│   │
│   ├── model/
│   │   └── ScheduleModel.scala         # Локальные типы + импорт из shared
│   │
│   ├── validation/
│   │   └── Validator.scala             # Чистые функции валидации (FR2.4)
│   │       • validateLesson() → Either[ValidationError, Lesson]
│   │       • validateDayBlock() → Either[ValidationError, DayBlock]
│   │       • validateSchedule() → Either[ValidationError, Schedule]
│   │       • validateTimeFormat() → Either[ValidationError, String]
│   │       • validateTimeRange() → Either[ValidationError, Unit]
│   │
│   └── serialization/
│       ├── JsonEncoder.scala           # Сериализация в JSON (FR2.5)
│       │   • encodeSchedule() → String
│       │   • saveScheduleToFile() → Either[String, Unit]
│       │
│       └── JsonDecoder.scala           # Десериализация из JSON
│           • loadScheduleFromFile() → Either[DecodeError, Schedule]
│           • parseScheduleJson() → Either[DecodeError, Schedule]

shared/
├── src/main/scala/shared/
│   └── ScheduleModel.scala             # Общая модель для editor и generator
│       • enum LessonType (Lecture, Practice, Lab)
│       • case class Slot
│       • enum DayOfWeek (Mon-Sat)
│       • case class DayBlock
│       • enum WeekType (Odd, Even)
│       • case class Week
│       • case class Meta
│       • case class ScheduleFile (root object)

build.sbt                              # SBT конфиг с JavaFX зависимостями
```

## Функциональные требования (соответствие FR)

| FR | Экран | Описание |
|----|----|---------|
| FR1 | MainWindow | Стартовый экран: "New Schedule", "Open Schedule" |
| FR2 | EditorScreen | Редактор расписания, управление днями и уроками |
| FR2.4 | Validator | Валидация в реальном времени |
| FR2.5 | JsonEncoder | Сохранение в JSON |
| FR3 | FormatScreen | Выбор формата: HTML/SVG/PDF |
| FR4 | StyleScreen | Выбор темы, цвета, размера шрифта |
| FR5 | DoneScreen | Предпросмотр и экспорт |
| FR6 | DoneScreen | Сохранение на диск |

## Архитектура

### Слой UI (JavaFX)
```
Main (запуск)
  ↓
AppController (навигация + состояние)
  ├── MainWindow
  ├── EditorScreen
  ├── FormatScreen
  ├── StyleScreen
  └── DoneScreen
```

### Слой модели
```
Schedule (ScheduleFile)
├── Week (нечётная/чётная)
│   └── DayBlock (пн-сб)
│       └── Slot (пара)
│           ├── subject
│           ├── room
│           ├── teacher
│           ├── lessonType
│           └── subgroups
└── Meta (метаданные)
```

### Слой логики
- **Validator**: чистые функции для проверки корректности данных
- **JsonEncoder**: преобразование моделей в JSON и сохранение
- **JsonDecoder**: чтение JSON и построение моделей

## Использование

### 1. Запуск приложения
```bash
cd editor
sbt run
```

### 2. Навигация
```
MainWindow
  → New Schedule → EditorScreen
  → Open Schedule → (FileChooser - открыть JSON)

EditorScreen
  → Save Schedule → FormatScreen

FormatScreen
  → Next → StyleScreen
  → Back → EditorScreen

StyleScreen
  → Next → DoneScreen
  → Back → FormatScreen

DoneScreen
  → Export → интеграция с generator
  → Save to File → сохранить JSON и вернуться на Main
  → Edit Again → возврат к EditorScreen
```

### 3. Сохранение расписания
```scala
// Автоматически при нажатии "Save Schedule":
// 1. Валидация через Validator.validateSchedule()
// 2. Сохранение через JsonEncoder.saveScheduleToFile()
// 3. Файл сохраняется с временной меткой: schedule_2026-05-26_14-30-45.json
```

### 4. Загрузка расписания
```scala
// При открытии через "Open Schedule":
JsonDecoder.loadScheduleFromFile("path/to/schedule.json")
// Возвращает Either[DecodeError, Schedule]
```

## Примеры кода

### Валидация Lesson
```scala
import editor.validation.Validator
import shared.{Slot, LessonType}

val slot = Slot(
  subject = "Математика",
  room = "101",
  teacher = "Иванов И.И.",
  lessonType = LessonType.Lecture,
  subgroups = List()
)

// Валидация через существующие функции
Validator.validateTimeFormat("14:30")  // Right("14:30")
Validator.validateTimeFormat("25:00")  // Left(InvalidTime("25:00"))
```

### Сохранение в JSON
```scala
import editor.serialization.JsonEncoder
import shared.{ScheduleFile, Meta, Week, DayBlock, Slot, LessonType, DayOfWeek}

val meta = Meta(
  version = "1.0",
  groupName = "ПМ-31",
  createdAt = "2026-05-26T10:00:00Z"
)

val scheduleFile = ScheduleFile(meta, Vector())

JsonEncoder.saveScheduleToFile(scheduleFile, "schedule.json")
// Right(()) — успешно сохранено
```

### Загрузка из JSON
```scala
import editor.serialization.JsonDecoder

JsonDecoder.loadScheduleFromFile("schedule.json") match {
  case Right(schedule) => println(s"Loaded: ${schedule.meta.groupName}")
  case Left(error) => println(s"Error: $error")
}
```

## JavaFX компоненты

### MainWindow
- **VBox** основной контейнер
- **Label** для заголовков
- **Button** для навигации
- Цветовая схема: светлый фон (#f5f5f5), зелено-синие кнопки

### EditorScreen
- **BorderPane** для компоновки
- **ListView** для отображения DayBlocks с кастомными ячейками
- **TextField** для ввода названия расписания
- **Button** для управления днями и уроками
- **HBox/VBox** для компоновки

### FormatScreen, StyleScreen, DoneScreen
- **RadioButton / ComboBox** для выбора параметров
- **TextArea** для превью
- **Button** для навигации

## Интеграция с generator

1. **Editor** сохраняет `ScheduleFile` в JSON
2. **Generator** читает JSON через `JsonDecoder`
3. **Generator** преобразует в HTML/SVG согласно выбранному стилю
4. Финальный файл сохраняется в выбранный формат

Поток:
```
Editor (Scala FX) 
  ↓ JSON файл
shared/ScheduleModel (типы)
  ↓
Generator (читает JSON)
  ↓ HTML/SVG
Файл на диск
```

## Зависимости в build.sbt

```scala
libraryDependencies ++= Seq(
  "org.openjfx" % "javafx-controls" % "21.0.2",
  "org.openjfx" % "javafx-fxml" % "21.0.2",
  "org.openjfx" % "javafx-graphics" % "21.0.2",
  "org.openjfx" % "javafx-base" % "21.0.2",
  // + platform-specific (win, mac, linux)
)
```

## Статус реализации

✅ **Завершено:**
- Полная структура файлов
- UI экраны с JavaFX
- Модель данных (shared)
- Валидация (чистые функции)
- Сериализация JSON (кодирование)
- Десериализация JSON (чтение)
- Контроллер навигации
- build.sbt с JavaFX

⏳ **Рекомендуется дальше:**
- Интеграция полной JSON-библиотеки (circe, play-json, или ujson)
- FileChooser для открытия файлов
- Обработка ошибок в UI
- Тесты (ScalaTest)
- Горячие клавиши и контекстные меню
- Экспорт в generator

## Notes

- Валидация использует `Either` для функционального подхода
- Все UI компоненты разделены по экранам для модульности
- AppController управляет состоянием и навигацией
- JSON сохраняется автоматически с временной меткой
- Интеграция с shared/ScheduleModel обеспечивает совместимость с generator
