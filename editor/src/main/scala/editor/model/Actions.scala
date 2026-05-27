package editor.model

import cats.effect.IO
import shared.*
import editor.serialization.JsonEncoder
import java.io.File

type Reducer = AppState => AppState

object Actions:
  def setGroupName(name: String): Reducer = _.copy(groupNameInput = name)
  
  def createNewSchedule(cfg: ScheduleConfig): Reducer = s =>
    val newSchedule = ScheduleFactory.empty(
      Meta("1.0", s.groupNameInput, java.time.LocalDateTime.now.toString),
      cfg
    )
    s.copy(schedule = newSchedule, currentWeekType = WeekType.Odd, isEditorMode = true)
  
  def loadSchedule(schedule: ScheduleFile): Reducer = s =>
    s.copy(schedule = schedule, currentWeekType = WeekType.Odd, isEditorMode = true)
  
  def saveToJson(file: File, schedule: ScheduleFile): IO[Either[String, Unit]] =
    JsonEncoder.saveScheduleToFile(schedule, file)
  
  def backToStart: Reducer = _.copy(isEditorMode = false)
  
  def setWeekType(weekType: WeekType): Reducer = _.copy(currentWeekType = weekType)