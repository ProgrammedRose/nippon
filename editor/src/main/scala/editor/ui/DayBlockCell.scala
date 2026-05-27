package editor.ui

import javafx.scene.control.ListCell
import shared.{DayBlock, DayOfWeek, ScheduleConfig}

class DayBlockCell(cfg: ScheduleConfig) extends ListCell[DayBlock]:
  
  private val dayNames: Map[DayOfWeek, String] = Map(
    DayOfWeek.Mon -> "Пн",
    DayOfWeek.Tue -> "Вт",
    DayOfWeek.Wed -> "Ср",
    DayOfWeek.Thu -> "Чт",
    DayOfWeek.Fri -> "Пт",
    DayOfWeek.Sat -> "Сб"
  )
  
  override def updateItem(dayBlock: DayBlock, empty: Boolean): Unit =
    super.updateItem(dayBlock, empty)
    if empty || dayBlock == null then
      setText(null)
      setGraphic(null)
    else
      val dayName = dayNames.getOrElse(dayBlock.day, "???")
      
      val slotsText = dayBlock.slots.indices.map { idx =>
        val timeOpt = cfg.lessonTimes.find(_.number == idx + 1)
        val timeStr = timeOpt.map(t => s" ${t.start}-${t.end}").getOrElse("")
        dayBlock.slots(idx) match
          case Some(slot) => s"${idx + 1}${timeStr}: ${slot.subject} (${slot.room})"
          case None => s"${idx + 1}${timeStr}: ---"
      }.mkString("\n")
      
      setStyle(s"-fx-text-fill: ${cfg.colors.text}; -fx-background-color: ${cfg.colors.oddWeekBg};")
      setText(s"$dayName\n$slotsText")