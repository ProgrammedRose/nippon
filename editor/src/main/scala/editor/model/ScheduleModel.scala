package editor.model

import shared.*

/**
 * Вспомогательные типы для UI редактора
 * Основная доменная модель находится в shared.ScheduleModel
 */

case class ScheduleEditState(
  scheduleFile: ScheduleFile,
  isModified: Boolean = false,
  selectedWeekType: Option[shared.DayBlock.WeekType] = None,
  selectedDayIndex: Int = -1,
  selectedSlotIndex: Int = -1
)

case class EditorState(
  scheduleFile: ScheduleFile,
  exportFormat: String = "html",
  theme: String = "Light",
  colorScheme: String = "Blue",
  fontSize: String = "Normal"
)
