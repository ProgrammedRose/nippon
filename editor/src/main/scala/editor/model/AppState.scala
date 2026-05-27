package editor.model

import shared.*

case class AppState(
  schedule: ScheduleFile,
  currentWeekType: WeekType,
  selectedDayIndex: Int,
  selectedSlotIndex: Int,
  isEditorMode: Boolean,
  groupNameInput: String,
  exportFormat: String = "html",
  theme: String = "Light",
  colorScheme: String = "Blue",
  fontSize: String = "Normal",
  slotEditMode: Boolean = false,
  editingSlotIndex: Int = -1
)

object AppState:
  def empty(groupName: String, cfg: ScheduleConfig): AppState =
    val schedule = ScheduleFactory.empty(
      Meta("1.0", groupName, java.time.LocalDateTime.now.toString),
      cfg
    )
    AppState(
      schedule = schedule,
      currentWeekType = WeekType.Odd,
      selectedDayIndex = -1,
      selectedSlotIndex = -1,
      isEditorMode = false,
      groupNameInput = groupName,
      exportFormat = "html",
      theme = "Light",
      colorScheme = "Blue",
      fontSize = "Normal",
      slotEditMode = false,
      editingSlotIndex = -1
    )