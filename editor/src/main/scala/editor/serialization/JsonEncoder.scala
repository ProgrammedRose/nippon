package editor.serialization

import shared.*
import scala.util.Using
import java.io.{FileWriter, PrintWriter}

/**
 * Сериализация моделей в JSON и запись на диск
 */
object JsonEncoder:

  /**
   * Преобразует ScheduleFile в JSON-строку
   */
  def encodeScheduleFile(scheduleFile: ScheduleFile): String =
    val metaJson = encodeMeta(scheduleFile.meta)
    val weeksJson = scheduleFile.weeks.map(encodeWeek).mkString(",\n    ")

    s"""{
       |  "meta": $metaJson,
       |  "weeks": [
       |    $weeksJson
       |  ]
       |}""".stripMargin

  /**
   * Преобразует Meta в JSON
   */
  private def encodeMeta(meta: shared.Meta): String =
    s"""{
       |    "version": "${meta.version}",
       |    "groupName": "${escapeJson(meta.groupName)}",
       |    "createdAt": "${meta.createdAt}"
       |  }""".stripMargin

  /**
   * Преобразует Week в JSON
   */
  private def encodeWeek(week: shared.Week): String =
    val daysJson = week.days.map(encodeDay).mkString(",\n      ")
    val weekType = week.weekType match {
      case shared.WeekType.Odd => "odd"
      case shared.WeekType.Even => "even"
    }

    s"""{
       |      "weekType": "$weekType",
       |      "days": [
       |        $daysJson
       |      ]
       |    }""".stripMargin

  /**
   * Преобразует DayBlock в JSON
   */
  private def encodeDay(dayBlock: DayBlock): String =
    val dayName = dayBlock.day match {
      case shared.DayOfWeek.Mon => "Monday"
      case shared.DayOfWeek.Tue => "Tuesday"
      case shared.DayOfWeek.Wed => "Wednesday"
      case shared.DayOfWeek.Thu => "Thursday"
      case shared.DayOfWeek.Fri => "Friday"
      case shared.DayOfWeek.Sat => "Saturday"
    }
    
    val slotsJson = dayBlock.slots.map {
      case Some(slot) => encodeSlot(slot)
      case None => "null"
    }.mkString(",\n        ")

    s"""{
       |        "day": "$dayName",
       |        "slots": [
       |          $slotsJson
       |        ]
       |      }""".stripMargin

  /**
   * Преобразует Slot в JSON
   */
  private def encodeSlot(slot: Slot): String =
    val lessonType = slot.lessonType match {
      case shared.LessonType.Lecture => "lecture"
      case shared.LessonType.Practice => "practice"
      case shared.LessonType.Lab => "lab"
    }
    val subgroupsJson = slot.subgroups.map(sg => s""""${escapeJson(sg)}"""").mkString(",")

    s"""{
       |          "subject": "${escapeJson(slot.subject)}",
       |          "room": "${escapeJson(slot.room)}",
       |          "teacher": "${escapeJson(slot.teacher)}",
       |          "lessonType": "$lessonType",
       |          "subgroups": [$subgroupsJson]
       |        }""".stripMargin

  /**
   * Экранирует специальные символы для JSON
   */
  private def escapeJson(s: String): String =
    s.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")

  /**
   * Сохраняет ScheduleFile в JSON-файл на диск
   */
  def saveScheduleToFile(scheduleFile: ScheduleFile, filePath: String): Either[String, Unit] =
    try
      Using(new PrintWriter(new FileWriter(filePath))) { writer =>
        writer.write(encodeScheduleFile(scheduleFile))
      }
      Right(())
    catch
      case e: Exception => Left(s"Failed to save schedule: ${e.getMessage}")
