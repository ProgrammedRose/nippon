package editor.serialization

import shared.*

object SerializeMappings {
  
  val weekTypeToJson: Map[WeekType, String] = Map(
    WeekType.Odd -> "odd",
    WeekType.Even -> "even"
  )
  val jsonToWeekType: Map[String, WeekType] = weekTypeToJson.map(_.swap)
  
  val dayToJson: Map[DayOfWeek, String] = Map(
    DayOfWeek.Mon -> "mon",
    DayOfWeek.Tue -> "tue",
    DayOfWeek.Wed -> "wed",
    DayOfWeek.Thu -> "thu",
    DayOfWeek.Fri -> "fri",
    DayOfWeek.Sat -> "sat"
  )
  val jsonToDay: Map[String, DayOfWeek] = dayToJson.map(_.swap)
  
  val lessonTypeToJson: Map[LessonType, String] = Map(
    LessonType.Lecture -> "lecture",
    LessonType.Practice -> "practice",
    LessonType.Lab -> "lab"
  )
  val jsonToLessonType: Map[String, LessonType] = lessonTypeToJson.map(_.swap)
}
