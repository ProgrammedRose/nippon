package editor.serialization

import cats.effect.IO
import io.circe.syntax._
import io.circe.{Encoder, Json}
import shared._
import java.io.{File, PrintWriter}

object JsonEncoder:
  
  // Встроенные маппинги (короткие названия, как в эталонном JSON)
  private val weekTypeToJson: Map[WeekType, String] = Map(
    WeekType.Odd -> "odd",
    WeekType.Even -> "even"
  )
  private val dayToJson: Map[DayOfWeek, String] = Map(
    DayOfWeek.Mon -> "mon",
    DayOfWeek.Tue -> "tue",
    DayOfWeek.Wed -> "wed",
    DayOfWeek.Thu -> "thu",
    DayOfWeek.Fri -> "fri",
    DayOfWeek.Sat -> "sat"
  )
  private val lessonTypeToJson: Map[LessonType, String] = Map(
    LessonType.Lecture -> "lecture",
    LessonType.Practice -> "practice",
    LessonType.Lab -> "lab"
  )
  
  given encodeWeekType: Encoder[WeekType] = Encoder.encodeString.contramap(weekTypeToJson)
  given encodeDayOfWeek: Encoder[DayOfWeek] = Encoder.encodeString.contramap(dayToJson)
  given encodeLessonType: Encoder[LessonType] = Encoder.encodeString.contramap(lessonTypeToJson)
  given encodeSlot: Encoder[Slot] = Encoder.forProduct5(
    "subject", "room", "teacher", "lessonType", "subgroups"
  )(s => (s.subject, s.room, s.teacher, s.lessonType, s.subgroups))
  given encodeDayBlock: Encoder[DayBlock] = Encoder.forProduct2("day", "slots")(db => (db.day, db.slots))
  given encodeWeek: Encoder[Week] = Encoder.forProduct2("weekType", "days")(w => (w.weekType, w.days))
  given encodeMeta: Encoder[Meta] = Encoder.forProduct3("version", "groupName", "createdAt")(m => (m.version, m.groupName, m.createdAt))
  given encodeScheduleFile: Encoder[ScheduleFile] = Encoder.forProduct2("meta", "weeks")(sf => (sf.meta, sf.weeks))
  
  def toJsonString(schedule: ScheduleFile): String = schedule.asJson.spaces2
  
  def saveScheduleToFile(schedule: ScheduleFile, file: File): IO[Either[String, Unit]] =
    IO.blocking {
      val pw = new PrintWriter(file)
      try pw.write(toJsonString(schedule))
      finally pw.close()
    }.attempt.map {
      case Right(_) => Right(())
      case Left(ex) => Left(s"Failed to save: ${ex.getMessage}")
    }
