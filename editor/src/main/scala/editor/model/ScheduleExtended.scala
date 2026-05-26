package editor.model

import shared.*
import shared.DayBlock

case class ScheduleEditState(
                              scheduleFile: ScheduleFile,
                              isModified: Boolean = false,
                              selectedWeekType: Option[WeekType] = None,
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