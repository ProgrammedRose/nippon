package editor.ui

import javafx.scene.control.ListCell
import shared.*

class DayBlockCell extends ListCell[DayBlock]:
  override def updateItem(dayBlock: DayBlock, empty: Boolean): Unit =
    super.updateItem(dayBlock, empty)
    if empty || dayBlock == null then
      setText(null)
      setGraphic(null)
    else
      val dayName = dayBlock.day match
        case DayOfWeek.Mon => "Пн"
        case DayOfWeek.Tue => "Вт"
        case DayOfWeek.Wed => "Ср"
        case DayOfWeek.Thu => "Чт"
        case DayOfWeek.Fri => "Пт"
        case DayOfWeek.Sat => "Сб"
      
      val slotsText = dayBlock.slots.indices.map { idx =>
        val slotText = dayBlock.slots(idx) match
          case Some(slot) => s"${idx+1}. ${slot.subject} (${slot.room})"
          case None => s"${idx+1}. ---"
        slotText
      }.mkString("\n")
      
      setText(s"$dayName\n$slotsText")
