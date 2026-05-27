# Editor Module - Functional Schedule Editor

## Overview

Pure functional JavaFX GUI for editing student schedules. Built on **immutable state threading** and **pure functions** with zero mutable state in business logic.

### Architecture Principles

- **Immutable State**: All state changes via pure `AppState => AppState` reducers (no `var`, `AtomicReference`, `Ref`)
- **Functional Composition**: State flows through recursive render functions
- **Type Safety**: Scala 3 with `given` instances (modern dependency injection)
- **Pure Validation**: Accumulates errors functionally (no `mutable.ListBuffer`)
- **IO Isolation**: All file operations via `cats.effect.IO`

---

## Project Structure

```
editor/
├── src/main/scala/editor/
│   ├── ui/
│   │   ├── Main.scala                      # Application entry point
│   │   ├── FxApp.scala                     # JavaFX Application class
│   │   ├── AppController.scala             # State-driven UI orchestration (REFACTORED: functional render pattern)
│   │   ├── MainWindow.scala                # Start screen (FR1)
│   │   ├── EditorScreen.scala              # Schedule editor (FR2) - REFACTORED: no AtomicReference
│   │   ├── FormatScreen.scala              # Export format selection (FR3)
│   │   ├── StyleScreen.scala               # Theme/color/font selection (FR4)
│   │   ├── DoneScreen.scala                # Preview & export (FR6)
│   │   ├── DayBlockCell.scala              # ListView cell renderer - REFACTORED: indices.map instead of zipWithIndex
│   │   ├── Renderer.scala                  # Functional UI renderer
│   │   └── SlotEditorDialog.scala          # NEW: Modal dialog for slot CRUD
│   │
│   ├── model/
│   │   ├── AppState.scala                  # EXPANDED: added workflow & slot editing fields
│   │   ├── Actions.scala                   # EXPANDED: pure reducers including slot CRUD
│   │   └── ScheduleExtended.scala          # UI-specific model extensions
│   │
│   ├── validation/
│   │   └── Validator.scala                 # REFACTORED: pure error accumulation (no mutable)
│   │
│   └── serialization/
│       ├── JsonEncoder.scala               # REFACTORED: implicit -> given
│       └── JsonDecoder.scala               # REFACTORED: implicit -> given
│
├── README.md                               # This file
└── build.sbt (parent)                      # SBT configuration
```

---

## Functional Programming Architecture

### State Model (Immutable)

```scala
case class AppState(
  schedule: ScheduleFile,              // Current schedule data
  currentWeekType: WeekType,           // Odd/Even week selector
  selectedDayIndex: Int,               // For slot selection
  selectedSlotIndex: Int,
  isEditorMode: Boolean,               // Workflow state (start vs editor)
  groupNameInput: String,
  exportFormat: String,                // html/svg/pdf
  theme: String, colorScheme: String, fontSize: String,
  slotEditMode: Boolean,               // Slot editor dialog state
  editingSlotIndex: Int
)
```

**No mutation of AppState** - all updates via pure function composition:

```scala
val newState = Actions.setExportFormat("pdf")(state)  // Returns new state, doesn't mutate
```

### Reducers (Pure Functions)

Type: `type Reducer = AppState => AppState`

All state transitions are pure functions:

```scala
object Actions:
  def saveSlot(dayIdx: Int, slotIdx: Int, slot: Slot): Reducer = s =>
    // Pure logic: extract, update, reconstruct
    val updatedSchedule = /* rebuild immutable structures */
    s.copy(schedule = updatedSchedule, slotEditMode = false)
  
  def deleteSlot(dayIdx: Int, slotIdx: Int): Reducer = s =>
    // No side effects, returns new state
    s.copy(schedule = updatedSchedule)
```

### Render Loop (Functional Recursion)

No mutable event listeners storing references. Instead, each UI event triggers a **pure reducer** followed by **recursive re-render**:

```scala
private def renderEditor(state: AppState): Unit =
  editorScreen.getAddSlotButton.setOnAction(_ =>
    val newState = Actions./* reducer */(state)
    renderEditor(newState)  // Recursive re-render with new state
  )
```

This replaces traditional MVC with **pure functional state threading**.

### Validation (Pure, No Mutation)

```scala
object Validator:
  def validateSlot(slot: Slot): Either[ValidationError, Slot] =
    val checks = List(
      if slot.subject.nonEmpty then Right(()) else Left(EmptyField("Subject")),
      if slot.room.nonEmpty then Right(()) else Left(EmptyField("Room")),
      // ...
    )
    val errors = checks.flatMap(_.left.toOption)
    if errors.isEmpty then Right(slot) else Left(InvalidSlot(errors))
```

**No `mutable.ListBuffer`** - errors accumulated via `flatMap` on List.

### Serialization (Modern Scala 3)

```scala
object JsonEncoder:
  given encodeWeekType: Encoder[WeekType] = 
    Encoder.encodeString.contramap(weekTypeToJson)
  
  given encodeSlot: Encoder[Slot] = 
    Encoder.forProduct5(...)(s => (s.subject, s.room, ...))
```

Uses **`given` instances** (Scala 3 standard) instead of `implicit` for cleaner, more explicit dependency injection.

---

## Features

### FR1: Main Window
- **New Schedule**: Create empty schedule with group name
- **Open Schedule**: Load schedule from JSON file
- Functional button handlers → reducers → re-render

### FR2: Editor Screen
- **Edit schedule**: Select week type, view days with lessons
- **Slot CRUD**: 
  - ✅ **Add Slot** → Opens `SlotEditorDialog` 
  - ✅ **Edit Selected** → Pre-populates dialog with existing slot
  - ✅ **Delete Selected** → Removes slot immediately (no confirmation needed)
- **Week selector**: Switch between Odd/Even weeks
- All changes reflected immediately in AppState (no disk writes yet)

### FR3: Format Selection
- HTML / SVG / PDF export formats
- Selected format stored in AppState

### FR4: Style Selection
- Theme: Light, Dark, Modern, Classic
- Color scheme: Blue, Green, Red, Purple, Orange
- Font size: Small, Normal, Large
- All selections flow through AppState

### FR5/FR6: Done Screen
- Preview of generated schedule
- Save to JSON file (with timestamp)
- Export to selected format (implementation in generator module)
- Edit again → re-enter editor with current state preserved

---

## How to Build & Run

### Prerequisites
- **Scala 3.3.7** (or higher)
- **Java 21+**
- **SBT 1.9+**

### Build

```bash
cd ScheduleGenUI
sbt "project editor" compile
```

### Run

```bash
sbt "project editor" run
```

### Tests (if implemented)

```bash
sbt "project editor" test
```

---

## Key Refactorings (FP Compliance)

### ✅ Removed
- ~~`AtomicReference[AppState]` in AppController~~ → Pure recursive render pattern
- ~~`AtomicReference[ScheduleFile]` in EditorScreen~~ → Stateless component with pure updates
- ~~`var` keywords~~ → All immutable case classes
- ~~`mutable.ListBuffer` in Validator~~ → Functional error accumulation via List flatMap
- ~~`.toList.zipWithIndex`~~ → `.indices.map` in DayBlockCell
- ~~`implicit val` for Encoders/Decoders~~ → Modern `given` instances

### ✅ Architecture Changes
- AppController: `setupEventHandlers()` with mutation → Functional `render*` methods with pure state threading
- Renderer pattern adopted from FxApp for UI orchestration
- SlotEditorDialog created for CRUD operations with dialog result mapping

---

## Functional Patterns Used

### 1. Reducer Pattern
```scala
def updateSlot(...): AppState => AppState = s => s.copy(...)
```

### 2. Function Composition
```scala
val state2 = Actions.setWeekType(Odd)(state1)
val state3 = Actions.setExportFormat("pdf")(state2)
```

### 3. Either for Error Handling
```scala
def validateSlot(...): Either[ValidationError, Slot] = ...
```

### 4. Pure IO with cats.effect
```scala
def saveScheduleToFile(...): IO[Either[String, Unit]] = IO.blocking { ... }
```

### 5. Case Class Immutability
```scala
s.copy(schedule = updated, slotEditMode = false)  // Creates new state
```

---

## Validation

### No Forbidden Patterns
- ✅ No `var`
- ✅ No `AtomicReference` or `Ref`
- ✅ No `mutable.*` collections in logic
- ✅ No `.zipWithIndex` on `.toList`
- ✅ All `implicit` converted to `given`

### Tests

Run validation with:
```bash
# Check for mutable patterns
grep -r "var \|AtomicReference\|mutable\.\|zipWithIndex" src/main/scala/editor/
# Should return 0 results (or only in JavaFX control initialization)
```

---

## Dependencies

From `build.sbt`:
- **JavaFX 21.0.2** (GUI framework)
- **Scala 3.3.7** (language)
- **Cats 2.10.0** (functional effects with `IO`)
- **Circe 0.14.6** (JSON serialization)

**No additional FP libraries needed** - pure functional style achieved with Scala language features.

---

## Next Steps

1. **Generator Module**: Convert schedule to HTML/SVG/PDF
2. **Tests**: Property-based tests for validators using ScalaCheck
3. **UI Polish**: Responsive layouts, better error messages
4. **Accessibility**: Keyboard navigation for all dialogs

---

## Contact & Support

For issues or improvements, refer to the main project README in the parent directory.
