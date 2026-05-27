package shared

import io.circe.{Decoder, Encoder, Json}

final case class LessonTime(
                             number: Int,
                             start: String,
                             end: String
                           )

object LessonTime:
  val decoder: Decoder[LessonTime] = Decoder.instance { cursor =>
    for
      number <- cursor.downField("number").as[Int]
      start  <- cursor.downField("start").as[String]
      end    <- cursor.downField("end").as[String]
    yield LessonTime(number, start, end)
  }
  
  val encoder: Encoder[LessonTime] = Encoder.instance { lt =>
    Json.obj(
      "number" -> Json.fromInt(lt.number),
      "start"  -> Json.fromString(lt.start),
      "end"    -> Json.fromString(lt.end)
    )
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
    yield ColorsConfig(oddWeekBg, evenWeekBg, lessonBg, emptySlotBg, text, dayHeader, room, teacher, pairNumber, border)
  }
  
  val encoder: Encoder[ColorsConfig] = Encoder.instance { cc =>
    Json.obj(
      "odd_week_bg"   -> Json.fromString(cc.oddWeekBg),
      "even_week_bg"  -> Json.fromString(cc.evenWeekBg),
      "lesson_bg"     -> Json.fromString(cc.lessonBg),
      "empty_slot_bg" -> Json.fromString(cc.emptySlotBg),
      "text"          -> Json.fromString(cc.text),
      "day_header"    -> Json.fromString(cc.dayHeader),
      "room"          -> Json.fromString(cc.room),
      "teacher"       -> Json.fromString(cc.teacher),
      "pair_number"   -> Json.fromString(cc.pairNumber),
      "border"        -> Json.fromString(cc.border)
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
  
  val encoder: Encoder[ScheduleConfig] = Encoder.instance { cfg =>
    Json.obj(
      "weeks"          -> Json.fromInt(cfg.weeks),
      "days_per_week"  -> Json.fromInt(cfg.daysPerWeek),
      "slots_per_day"  -> Json.fromInt(cfg.slotsPerDay),
      "lesson_times"   -> Encoder.encodeList(LessonTime.encoder)(cfg.lessonTimes),
      "colors"         -> ColorsConfig.encoder(cfg.colors)
    )
  }