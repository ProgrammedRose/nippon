package editor.model

import cats.effect.IO
import shared.*
import editor.serialization.JsonEncoder
import java.io.File

type Reducer = AppState => AppState

object Actions:
  // Group name management
  def setGroupName(name: String): Reducer = _.copy(groupNameInput = name)
  
  // Schedule lifecycle
  def createNewSchedule(cfg: ScheduleConfig): Reducer = s =>
    val newSchedule = ScheduleFactory.empty(
      Meta("1.0", s.groupNameInput, java.time.LocalDateTime.now.toString),
      cfg
    )
    s.copy(schedule = newSchedule, currentWeekType = WeekType.Odd, isEditorMode = true)
  
  def loadSchedule(schedule: ScheduleFile): Reducer = s =>
    s.copy(schedule = schedule, currentWeekType = WeekType.Odd, isEditorMode = true)
  
  def backToStart: Reducer = _.copy(isEditorMode = false)
  
  // Week selection
  def setWeekType(weekType: WeekType): Reducer = _.copy(currentWeekType = weekType)
  
  // Format and style workflow
  def setExportFormat(format: String): Reducer = _.copy(exportFormat = format)
  def setTheme(theme: String): Reducer = _.copy(theme = theme)
  def setColorScheme(scheme: String): Reducer = _.copy(colorScheme = scheme)
  def setFontSize(size: String): Reducer = _.copy(fontSize = size)
  
  // Slot selection
  def selectSlot(dayIdx: Int, slotIdx: Int): Reducer = 
    _.copy(selectedDayIndex = dayIdx, selectedSlotIndex = slotIdx)
  
  def clearSlotSelection: Reducer = 
    _.copy(selectedDayIndex = -1, selectedSlotIndex = -1)
  
  // Slot editing mode
  def openSlotEditor(slotIdx: Int): Reducer = 
    _.copy(slotEditMode = true, editingSlotIndex = slotIdx)
  
  def cancelSlotEdit: Reducer = 
    _.copy(slotEditMode = false, editingSlotIndex = -1)
  
  // Slot CRUD operations (pure, no mutation)
  def saveSlot(dayIdx: Int, slotIdx: Int, slot: Slot): Reducer = s =>
    val currentWeek = s.schedule.weeks.find(_.weekType == s.currentWeekType).get
    val day = currentWeek.days(dayIdx)
    val updatedDay = day.copy(slots = day.slots.updated(slotIdx, Some(slot)))
    val updatedDays = currentWeek.days.updated(dayIdx, updatedDay)
    val updatedWeek = currentWeek.copy(days = updatedDays)
    val updatedWeeks = s.schedule.weeks.map(w => 
      if w.weekType == s.currentWeekType then updatedWeek else w
    )
    val updatedSchedule = s.schedule.copy(weeks = updatedWeeks)
    s.copy(
      schedule = updatedSchedule,
      slotEditMode = false,
      editingSlotIndex = -1
    )
  
  def deleteSlot(dayIdx: Int, slotIdx: Int): Reducer = s =>
    val currentWeek = s.schedule.weeks.find(_.weekType == s.currentWeekType).get
    val day = currentWeek.days(dayIdx)
    val updatedDay = day.copy(slots = day.slots.updated(slotIdx, None))
    val updatedDays = currentWeek.days.updated(dayIdx, updatedDay)
    val updatedWeek = currentWeek.copy(days = updatedDays)
    val updatedWeeks = s.schedule.weeks.map(w => 
      if w.weekType == s.currentWeekType then updatedWeek else w
    )
    val updatedSchedule = s.schedule.copy(weeks = updatedWeeks)
    s.copy(schedule = updatedSchedule)
  
  def saveToJson(file: File, schedule: ScheduleFile): IO[Either[String, Unit]] =
    JsonEncoder.saveScheduleToFile(schedule, file)