package shared

object ScheduleFactory:
  
  def empty(meta: Meta, cfg: ScheduleConfig): ScheduleFile =
    ScheduleFile(
      meta,
      Vector(
        Week.empty(WeekType.Odd, cfg),
        Week.empty(WeekType.Even, cfg)
      )
    )