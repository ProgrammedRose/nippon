package editor.serialization

import cats.effect.IO
import io.circe.parser.decode
import io.circe.{Decoder, Error}
import shared._
import java.io.File
import scala.io.Source
import scala.util.Using
import cats.implicits.catsSyntaxEither

sealed trait DecodeError
object DecodeError:
  case class ParseError(message: String) extends DecodeError
  case class ValidationError(message: String) extends DecodeError

object JsonDecoder:
  
  // Встроенные маппинги (без отдельного объекта Mapping)
  private val jsonToWeekType: Map[String, WeekType] = Map(
    "odd" -> WeekType.Odd,
    "even" -> WeekType.Even
  )
  private val jsonToDay: Map[String, DayOfWeek] = Map(
    "mon" -> DayOfWeek.Mon,
    "tue" -> DayOfWeek.Tue,
    "wed" -> DayOfWeek.Wed,
    "thu" -> DayOfWeek.Thu,
    "fri" -> DayOfWeek.Fri,
    "sat" -> DayOfWeek.Sat
  )
  private val jsonToLessonType: Map[String, LessonType] = Map(
    "lecture" -> LessonType.Lecture,
    "practice" -> LessonType.Practice,
    "lab" -> LessonType.Lab
  )
  
  given decodeWeekType: Decoder[WeekType] = Decoder[String].emap { s =>
    jsonToWeekType.get(s).toRight(s"Unknown week type: $s")
  }
  given decodeDayOfWeek: Decoder[DayOfWeek] = Decoder[String].emap { s =>
    jsonToDay.get(s.toLowerCase).toRight(s"Unknown day: $s")
  }
  given decodeLessonType: Decoder[LessonType] = Decoder[String].emap { s =>
    jsonToLessonType.get(s).toRight(s"Unknown lesson type: $s")
  }
  given decodeSlot: Decoder[Slot] = Decoder.forProduct5(
    "subject", "room", "teacher", "lessonType", "subgroups"
  )(Slot.apply)
  given decodeDayBlock: Decoder[DayBlock] = Decoder.forProduct2("day", "slots")(DayBlock.apply)
  given decodeWeek: Decoder[Week] = Decoder.forProduct2("weekType", "days")(Week.apply)
  given decodeMeta: Decoder[Meta] = Decoder.forProduct3("version", "groupName", "createdAt")(Meta.apply)
  given decodeScheduleFile: Decoder[ScheduleFile] = Decoder.forProduct2("meta", "weeks")(ScheduleFile.apply)
  
  def loadScheduleFromFile(file: File): IO[Either[DecodeError, ScheduleFile]] =
    IO.blocking {
      Using(Source.fromFile(file))(_.mkString).toEither.left.map { ex =>
        DecodeError.ParseError(s"Failed to read file: ${ex.getMessage}")
      }
    }.flatMap {
      case Right(jsonStr) => IO.pure(parseScheduleFile(jsonStr))
      case Left(err)      => IO.pure(Left(err))
    }
  
  def parseScheduleFile(jsonStr: String): Either[DecodeError, ScheduleFile] =
    decode[ScheduleFile](jsonStr).leftMap { circeErr =>
      DecodeError.ParseError(circeErr.getMessage)
    }