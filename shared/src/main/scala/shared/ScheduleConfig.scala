package shared

final case class ScheduleConfig(
                                 weeks: Int,
                                 daysPerWeek: Int,
                                 slotsPerDay: Int
                               )

object ScheduleConfig:
  import io.circe.Decoder

  given Decoder[ScheduleConfig] =
    Decoder.forProduct3("weeks", "days_per_week", "slots_per_day")(ScheduleConfig.apply)