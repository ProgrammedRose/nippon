package editor.validation

import shared.*

/**
 * Валидация компонентов расписания (pure functions)
 * Работает с типами из shared
 */
object Validator {

  sealed trait ValidationError
  case class InvalidSubject(value: String) extends ValidationError
  case class InvalidRoom(value: String) extends ValidationError
  case class InvalidTeacher(value: String) extends ValidationError
  case class EmptyField(fieldName: String) extends ValidationError
  case class InvalidSlot(errors: List[ValidationError]) extends ValidationError
  case class InvalidSchedule(errors: List[ValidationError]) extends ValidationError

  /**
   * Проверяет, что строка не пуста
   */
  def validateNotEmpty(value: String, fieldName: String): Either[ValidationError, String] = {
    if (value.trim.nonEmpty) Right(value)
    else Left(EmptyField(fieldName))
  }

  /**
   * Валидация Slot (пара)
   */
  def validateSlot(slot: Slot): Either[ValidationError, Slot] = {
    val errors = scala.collection.mutable.ListBuffer[ValidationError]()

    if (slot.subject.trim.isEmpty) errors += EmptyField("Subject name")
    if (slot.room.trim.isEmpty) errors += EmptyField("Room number")
    if (slot.teacher.trim.isEmpty) errors += EmptyField("Teacher name")
    if (slot.subgroups.isEmpty) errors += EmptyField("Subgroups")

    if (errors.isEmpty) Right(slot)
    else Left(InvalidSlot(errors.toList))
  }

  /**
   * Валидация DayBlock (день недели с парами)
   */
  def validateDayBlock(dayBlock: DayBlock): Either[ValidationError, DayBlock] = {
    val errors = scala.collection.mutable.ListBuffer[ValidationError]()

    dayBlock.slots.foreach {
      case Some(slot) => validateSlot(slot).left.foreach(e => errors += e)
      case None => // пуста пара, это нормально
    }

    if (errors.isEmpty) Right(dayBlock)
    else Left(InvalidSchedule(errors.toList))
  }

  /**
   * Валидация Week (неделя - чётная или нечётная)
   */
  def validateWeek(week: Week): Either[ValidationError, Week] = {
    val errors = scala.collection.mutable.ListBuffer[ValidationError]()

    week.days.foreach(dayBlock =>
      validateDayBlock(dayBlock).left.foreach(e => errors += e)
    )

    if (errors.isEmpty) Right(week)
    else Left(InvalidSchedule(errors.toList))
  }

  /**
   * Валидация ScheduleFile (полное расписание)
   */
  def validateScheduleFile(scheduleFile: ScheduleFile): Either[ValidationError, ScheduleFile] = {
    val errors = scala.collection.mutable.ListBuffer[ValidationError]()

    if (scheduleFile.meta.groupName.trim.isEmpty) errors += EmptyField("Group name")
    if (scheduleFile.meta.version.trim.isEmpty) errors += EmptyField("Version")

    scheduleFile.weeks.foreach(week =>
      validateWeek(week).left.foreach(e => errors += e)
    )

    if (errors.isEmpty) Right(scheduleFile)
    else Left(InvalidSchedule(errors.toList))
  }
}
