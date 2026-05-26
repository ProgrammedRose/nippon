package shared

final case class ScheduleConfig(
                                 weeks: Int,
                                 daysPerWeek: Int,
                                 slotsPerDay: Int
                               )