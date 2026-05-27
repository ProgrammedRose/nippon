package editor.validation

import shared.*

/**
 * Валидация компонентов расписания (pure functions, no mutation)
 * Работает с типами из shared
 */
object Validator:

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
  def validateNotEmpty(value: String, fieldName: String): Either[ValidationError, String] =
    if value.trim.nonEmpty then Right(value)
    else Left(EmptyField(fieldName))

  /**
   * Функциональное накопление ошибок из списка Either
   */
  private def accumulate[A](results: List[Either[ValidationError, A]]): Either[List[ValidationError], Unit] =
    val errors = results.flatMap(_.left.toOption)
    if errors.isEmpty then Right(()) else Left(errors)

  /**
   * Валидация Slot (пара) - чистая функция, без мутации
   */
  def validateSlot(slot: Slot): Either[ValidationError, Slot] =
    val checks = List(
      if slot.subject.trim.nonEmpty then Right(()) else Left(EmptyField("Subject name")),
      if slot.room.trim.nonEmpty then Right(()) else Left(EmptyField("Room number")),
      if slot.teacher.trim.nonEmpty then Right(()) else Left(EmptyField("Teacher name"))
      // подгруппы не проверяем — пустой список означает «вся группа», это ок
    )

    val errors = checks.flatMap(_.left.toOption)
    if errors.isEmpty then Right(slot)
    else Left(InvalidSlot(errors))
  /**
   * Валидация DayBlock (день недели с парами)
   */
  def validateDayBlock(dayBlock: DayBlock): Either[ValidationError, DayBlock] =
    val slotErrors = dayBlock.slots.flatMap {
      case Some(slot) => validateSlot(slot).left.toOption.toList
      case None => Nil
    }
    
    if slotErrors.isEmpty then Right(dayBlock)
    else Left(InvalidSchedule(slotErrors.toList))

  /**
   * Валидация Week (неделя - чётная или нечётная)
   */
  def validateWeek(week: Week): Either[ValidationError, Week] =
    val dayErrors = week.days.flatMap { dayBlock =>
      validateDayBlock(dayBlock).left.toOption.toList
    }
    
    if dayErrors.isEmpty then Right(week)
    else Left(InvalidSchedule(dayErrors.toList))

  /**
   * Валидация ScheduleFile (полное расписание)
   */
  def validateScheduleFile(scheduleFile: ScheduleFile): Either[ValidationError, ScheduleFile] =
    val metaChecks = List(
      if scheduleFile.meta.groupName.trim.nonEmpty then Right(()) 
        else Left(EmptyField("Group name")),
      if scheduleFile.meta.version.trim.nonEmpty then Right(()) 
        else Left(EmptyField("Version"))
    )
    
    val metaErrors = metaChecks.flatMap(_.left.toOption)
    
    val weekErrors = scheduleFile.weeks.flatMap { week =>
      validateWeek(week).left.toOption.toList
    }
    
    val allErrors = metaErrors ++ weekErrors
    if allErrors.isEmpty then Right(scheduleFile)
    else Left(InvalidSchedule(allErrors))
