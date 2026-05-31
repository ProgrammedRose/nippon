package editor.serialization

import cats.effect.IO
import io.circe.Decoder
import io.circe.parser.decode
import shared.*
import cats.implicits.catsSyntaxEither
import java.io.File
import scala.io.Source
import scala.util.Using

sealed trait DecodeError

object DecodeError:
  case class ParseError(message: String) extends DecodeError

object JsonDecoder:
  import SerializeMappings.*
  
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
      Using.resource(Source.fromFile(file))(_.mkString)
    }.attempt.map {
      case Right(jsonStr) => parseScheduleFile(jsonStr)
      case Left(ex)       => Left(DecodeError.ParseError(s"Failed to read file: ${ex.getMessage}"))
    }
  
  private def parseScheduleFile(jsonStr: String): Either[DecodeError, ScheduleFile] =
    decode[ScheduleFile](jsonStr).leftMap { circeErr =>
      DecodeError.ParseError(circeErr.getMessage)
    }