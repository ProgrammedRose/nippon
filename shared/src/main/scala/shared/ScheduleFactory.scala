package shared

/** Строит начальный вариант расписания (пустой),
 * получив данные из конфига */
object ScheduleFactory:
  def empty(meta: Meta, cfg: ScheduleConfig): ScheduleFile =
    ScheduleFile(
      meta,
      Vector(
        Week.empty(WeekType.Odd, cfg),
        Week.empty(WeekType.Even, cfg)
      )
    )