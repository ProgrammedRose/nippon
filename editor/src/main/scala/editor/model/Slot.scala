package editor.model

import shared.LessonType

// В Slot нужно добавить валидацию при создании
final case class Slot(
                       subject: String,     // не может быть пустым
                       room: String,        // не может быть пустым  
                       teacher: String,     // не может быть пустым
                       lessonType: LessonType,
                       subgroups: List[String],
                       weekNumbers: List[Int] = (1 to 18).toList  // номера недель, когда есть занятие
                     )

// В объект-компаньон добавить validate метод
object Slot:
  def create(
              subject: String,
              room: String,
              teacher: String,
              lessonType: LessonType,
              subgroups: List[String],
              weekNumbers: List[Int]
            ): Either[String, Slot] =
    if subject.isBlank then Left("Название предмета обязательно")
    else if room.isBlank then Left("Аудитория обязательна")
    else if teacher.isBlank then Left("Преподаватель обязателен")
    else Right(Slot(subject.trim, room.trim, teacher.trim, lessonType, subgroups, weekNumbers))