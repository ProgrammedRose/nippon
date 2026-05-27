package editor.model

import shared.{LessonType, Slot}

// Расширенные данные для занятия (хранятся отдельно от Slot)
final case class ExtendedLesson(
                                 subject: String,
                                 room: String,
                                 teacher: String,
                                 lessonType: LessonType,
                                 subgroups: List[String],  // только "1","2","3","4" или пустой список (вся группа)
                                 weekNumbers: List[Int]    // номера недель от 1 до 18
                               )

object ExtendedLesson:
  
  // Валидация при создании
  def create(
              subject: String,
              room: String,
              teacher: String,
              lessonType: LessonType,
              subgroups: List[String],
              weekNumbers: List[Int]
            ): Either[String, ExtendedLesson] =
    for
      _ <- Either.cond(subject.trim.nonEmpty, (), "Название предмета обязательно")
      _ <- Either.cond(room.trim.nonEmpty, (), "Аудитория обязательна")
      _ <- Either.cond(teacher.trim.nonEmpty, (), "Преподаватель обязателен")
      _ <- Either.cond(
        subgroups.forall(s => s == "1" || s == "2" || s == "3" || s == "4" || s.isEmpty),
        (),
        "Подгруппы могут быть только 1,2,3,4"
      )
      _ <- Either.cond(
        weekNumbers.forall(w => 1 <= w && w <= 18),
        (),
        "Номера недель должны быть от 1 до 18"
      )
      _ <- Either.cond(weekNumbers.nonEmpty, (), "Должна быть указана хотя бы одна неделя")
    yield ExtendedLesson(
      subject.trim,
      room.trim,
      teacher.trim,
      lessonType,
      subgroups.filter(_.nonEmpty),
      weekNumbers.sorted.distinct
    )
  
  // Преобразование в Slot (без недель, для сохранения в shared-формате)
  def toSlot(lesson: ExtendedLesson): Slot =
    Slot(
      subject = lesson.subject,
      room = lesson.room,
      teacher = lesson.teacher,
      lessonType = lesson.lessonType,
      subgroups = lesson.subgroups
    )
  
  // Создание из Slot (с неделями по умолчанию — все)
  def fromSlot(slot: Slot, weekNumbers: List[Int] = (1 to 18).toList): Either[String, ExtendedLesson] =
    create(
      slot.subject,
      slot.room,
      slot.teacher,
      slot.lessonType,
      slot.subgroups,
      weekNumbers
    )
