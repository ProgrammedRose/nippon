package editor.model

import shared.{LessonType, WeekType}

case class Lesson(
                   subject: String,
                   teacher: String,
                   room: String,
                   lessonType: LessonType,
                   subgroups: List[Subgroup],      // ограниченный список
                   weekType: Option[WeekType],     // None = каждую неделю, Some(Odd/Even)
                   customWeeks: List[Int]          // если нужно указать конкретные недели
                 )
