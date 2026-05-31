package editor.model

import cats.effect.IO
import shared.*
import editor.serialization.JsonEncoder
import editor.validation.Validator
import java.io.File

/** Тип чистого преобразования состояния. */
type Reducer = AppState => AppState

/** Набор действий (редьюсеров) для обновления состояния редактора. */
object Actions:
  
  /** Устанавливает введённое имя группы во временное поле состояния. */
  def setGroupName(name: String): Reducer = _.copy(groupNameInput = name)
  
  /** Создаёт новое пустое расписание с текущим именем группы и переводит редактор в активный режим. */
  def createNewSchedule(cfg: ScheduleConfig): Reducer = s =>
    val newSchedule = ScheduleFactory.empty(
      Meta("1.0", s.groupNameInput, java.time.LocalDateTime.now.toString),
      cfg
    )
    s.copy(schedule = newSchedule, currentWeekType = WeekType.Odd, isEditorMode = true)
  
  /** Загружает готовое расписание и переводит редактор в активный режим. */
  def loadSchedule(schedule: ScheduleFile): Reducer = s =>
    s.copy(schedule = schedule, currentWeekType = WeekType.Odd, isEditorMode = true)
  
  /** Возвращается на начальный экран (выход из режима редактора). */
  def backToStart: Reducer = _.copy(isEditorMode = false)
  
  /** Переключает отображаемую неделю (чётная/нечётная). */
  def setWeekType(weekType: WeekType): Reducer = _.copy(currentWeekType = weekType)
  
  /** Устанавливает формат экспорта (html, svg, sec, thrd). */
  def setExportFormat(format: String): Reducer = _.copy(exportFormat = format)
  /** Устанавливает тему оформления для генератора. */
  def setTheme(theme: String): Reducer = _.copy(theme = theme)
  /** Устанавливает цветовую схему UI редактора. */
  def setColorScheme(scheme: String): Reducer = _.copy(colorScheme = scheme)
  /** Устанавливает размер шрифта в UI редактора. */
  def setFontSize(size: String): Reducer = _.copy(fontSize = size)
  
  /** Выбирает день и сбрасывает выбор слота. */
  def selectDay(dayIdx: Int): Reducer = _.copy(selectedDayIndex = dayIdx, selectedSlotIndex = -1)
  /** Выбирает слот в текущем выбранном дне. */
  def selectSlot(slotIdx: Int): Reducer = _.copy(selectedSlotIndex = slotIdx)
  /** Снимает выделение дня и слота. */
  def clearSlotSelection: Reducer = _.copy(selectedDayIndex = -1, selectedSlotIndex = -1)
  
  /** Открывает форму редактирования слота. */
  def openSlotEditor(slotIdx: Int): Reducer = _.copy(slotEditMode = true, editingSlotIndex = slotIdx)
  /** Закрывает форму редактирования без сохранения. */
  def cancelSlotEdit: Reducer = _.copy(slotEditMode = false, editingSlotIndex = -1)
  
  /** Возвращает текущую неделю (согласно currentWeekType) из расписания, если она существует. */
  private def getCurrentWeek(s: AppState): Option[Week] =
    s.schedule.weeks.find(_.weekType == s.currentWeekType)
  
  /** Применяет функцию f к текущей неделе и возвращает обновлённое состояние.
   * Если текущая неделя не найдена, возвращает исходное состояние.
   */
  private def updateCurrentWeek(s: AppState)(f: Week => Week): AppState =
    getCurrentWeek(s).fold(s) { week =>
      val updatedWeeks = s.schedule.weeks.map {
        case w if w.weekType == s.currentWeekType => f(w)
        case w => w
      }
      s.copy(schedule = s.schedule.copy(weeks = updatedWeeks))
    }
  
  /** Сохраняет отредактированный слот после успешной валидации. При ошибке валидации состояние не меняется. */
  def saveSlot(dayIndex: Int, slotIndex: Int, slot: Slot): Reducer = s =>
    Validator.validateSlot(slot) match
      case Right(validSlot) =>
        updateCurrentWeek(s) { week =>
          val day = week.days(dayIndex)
          val updatedDay = day.copy(slots = day.slots.updated(slotIndex, Some(validSlot)))
          week.copy(days = week.days.updated(dayIndex, updatedDay))
        }
      case Left(_) => s
  
  /** Удаляет слот (заменяет его на None). */
  def deleteSlot(dayIndex: Int, slotIndex: Int): Reducer = s =>
    updateCurrentWeek(s) { week =>
      val day = week.days(dayIndex)
      val updatedDay = day.copy(slots = day.slots.updated(slotIndex, None))
      week.copy(days = week.days.updated(dayIndex, updatedDay))
    }
  
  /** Сохраняет расписание в файл в формате JSON (чистый эффект, не редьюсер). */
  def saveToJson(file: File, schedule: ScheduleFile): IO[Either[String, Unit]] =
    JsonEncoder.saveScheduleToFile(schedule, file)