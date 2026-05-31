package editor.ui

import javafx.scene.control.ListCell
import shared.{DayBlock, DayOfWeek, ScheduleConfig}

class DayBlockCell(
                    cfg: ScheduleConfig
                  ) extends ListCell[DayBlock]:
  
  // Да, есть похожая мапа в коде, но тут разные
  // предметные области. Там - сериализация, тут - GUI
  // Мне одновременно нужны и русские названия, и нет
  private val dayNames =
    Map(
      DayOfWeek.Mon -> "Понедельник",
      DayOfWeek.Tue -> "Вторник",
      DayOfWeek.Wed -> "Среда",
      DayOfWeek.Thu -> "Четверг",
      DayOfWeek.Fri -> "Пятница",
      DayOfWeek.Sat -> "Суббота"
    )
  
  override def updateItem(
                           item: DayBlock,
                           empty: Boolean
                         ): Unit =
    super.updateItem(item, empty)
    
    if empty || item == null then
      setText(null)
      setGraphic(null)
    else
      
      val filledCount =
        item.slots.count(_.nonEmpty)
      
      setText(
        s"""
           |${dayNames(item.day)}
           |Заполнено пар: $filledCount
           |""".stripMargin
      )
      
      if isSelected then
        setStyle(
          s"""
             | -fx-background-color: ${cfg.colors.lessonBg};
             | -fx-text-fill: white;
             | -fx-background-radius: 14;
             | -fx-padding: 18;
             | -fx-font-size: 14px;
             | -fx-font-weight: bold;
             | -fx-border-color: ${cfg.colors.lessonBg};
             | -fx-border-radius: 14;
             |""".stripMargin
        )
      else
        setStyle(
          s"""
             | -fx-background-color: ${cfg.colors.evenWeekBg};
             | -fx-text-fill: ${cfg.colors.text};
             | -fx-background-radius: 14;
             | -fx-padding: 18;
             | -fx-font-size: 14px;
             | -fx-border-color: ${cfg.colors.border};
             | -fx-border-radius: 14;
             |""".stripMargin
        )