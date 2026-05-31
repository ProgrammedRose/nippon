package shared

import io.circe.{Decoder, Encoder, Json}

/** Класс для чтения времени начала/конца одной пары */
final case class LessonTime(
                             number: Int,
                             start: String,
                             end: String
                           )
/** Методы для библиотеки circle, считывающие этот класс из файла */
object LessonTime:
  val decoder: Decoder[LessonTime] = Decoder.instance { cursor =>
    for
      number <- cursor.downField("number").as[Int]
      start  <- cursor.downField("start").as[String]
      end    <- cursor.downField("end").as[String]
    yield LessonTime(number, start, end)
  }

final case class ColorsConfig(
                               oddWeekBg: String,
                               evenWeekBg: String,
                               lessonBg: String,
                               emptySlotBg: String,
                               text: String,
                               dayHeader: String,
                               room: String,
                               teacher: String,
                               pairNumber: String,
                               border: String
                             )

object ColorsConfig:
  val decoder: Decoder[ColorsConfig] = Decoder.instance { cursor =>
    for
      oddWeekBg   <- cursor.downField("odd_week_bg").as[String]
      evenWeekBg  <- cursor.downField("even_week_bg").as[String]
      lessonBg    <- cursor.downField("lesson_bg").as[String]
      emptySlotBg <- cursor.downField("empty_slot_bg").as[String]
      text        <- cursor.downField("text").as[String]
      dayHeader   <- cursor.downField("day_header").as[String]
      room        <- cursor.downField("room").as[String]
      teacher     <- cursor.downField("teacher").as[String]
      pairNumber  <- cursor.downField("pair_number").as[String]
      border      <- cursor.downField("border").as[String]
    yield ColorsConfig(
      oddWeekBg, evenWeekBg, 
      lessonBg, emptySlotBg, 
      text, dayHeader, 
      room, teacher, 
      pairNumber, border
    )
  }

final case class ScheduleConfig(
                                 weeks: Int,
                                 daysPerWeek: Int,
                                 slotsPerDay: Int,
                                 lessonTimes: List[LessonTime],
                                 colors: ColorsConfig
                               )

object ScheduleConfig:
  val decoder: Decoder[ScheduleConfig] = Decoder.instance { cursor =>
    for
      weeks       <- cursor.downField("weeks").as[Int]
      daysPerWeek <- cursor.downField("days_per_week").as[Int]
      slotsPerDay <- cursor.downField("slots_per_day").as[Int]
      lessonTimes <- cursor.downField("lesson_times").as[List[LessonTime]](Decoder.decodeList(LessonTime.decoder))
      colors      <- cursor.downField("colors").as[ColorsConfig](ColorsConfig.decoder)
    yield ScheduleConfig(weeks, daysPerWeek, slotsPerDay, lessonTimes, colors)
  }