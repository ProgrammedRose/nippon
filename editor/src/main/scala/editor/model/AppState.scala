package editor.model

import shared.*

case class AppState(
                     schedule: ScheduleFile,
                     currentWeekType: WeekType,
                     selectedDayIndex: Int,
                     selectedSlotIndex: Int,
                     isEditorMode: Boolean,
                     groupNameInput: String
                   )

object AppState:
  def empty(groupName: String, cfg: ScheduleConfig): AppState =
    val schedule = ScheduleFactory.empty(
      Meta("1.0", groupName, java.time.LocalDateTime.now.toString),
      cfg
    )
    AppState(schedule, WeekType.Odd, -1, -1, false, groupName)