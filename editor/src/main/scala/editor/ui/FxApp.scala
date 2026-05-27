// editor/ui/FxApp.scala
package editor.ui

import javafx.application.Application
import javafx.stage.Stage
import shared.*
import editor.model.AppState

class FxApp extends Application:
  
  override def start(stage: Stage): Unit =
    val initialState = AppState(
      schedule = ScheduleFactory.empty(
        Meta("1.0", "", java.time.LocalDateTime.now.toString),
        ScheduleConfig(2, 6, 6)
      ),
      currentWeekType = WeekType.Odd,
      selectedDayIndex = -1,
      selectedSlotIndex = -1,
      isEditorMode = false,
      groupNameInput = ""
    )
    Renderer.render(initialState, stage, ScheduleConfig(2, 6, 6))
    stage.setTitle("Schedule Editor")
    stage.show()