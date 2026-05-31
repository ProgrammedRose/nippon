package generator.parser

import cats.effect.IO
import io.circe.*
import io.circe.parser.*
import shared.*

// ─────────────────────────────────────────────────────────────────────────────
// Ошибки декодирования
// ─────────────────────────────────────────────────────────────────────────────

sealed trait DecodeError:
  def message: String

object DecodeError:
  final case class IoError(path: String, cause: Throwable) extends DecodeError:
    def message: String = s"Cannot read file '$path': ${cause.getMessage}"

  final case class ParseError(details: String) extends DecodeError:
    def message: String = s"JSON parse error: $details"

  final case class SchemaError(details: String) extends DecodeError:
    def message: String = s"Schema mismatch: $details"

// ─────────────────────────────────────────────────────────────────────────────
// Circe-декодеры
// ─────────────────────────────────────────────────────────────────────────────

private object Decoders:

  given Decoder[LessonType] = Decoder.decodeString.emap: raw =>
    raw.toLowerCase match
      case "lecture"  => Right(LessonType.Lecture)
      case "practice" => Right(LessonType.Practice)
      case "lab"      => Right(LessonType.Lab)
      case _          => Left(s"Unknown lesson type: '$raw'. Expected: lecture | practice | lab")

  given Decoder[Slot] = Decoder.forProduct5(
    "subject", "room", "teacher", "lessonType", "subgroups"
  )(Slot.apply)

  given Decoder[Option[Slot]] = Decoder.decodeOption[Slot]

  given Decoder[DayOfWeek] = Decoder.decodeString.emap: raw =>
    raw.toLowerCase match
      case "mon" => Right(DayOfWeek.Mon)
      case "tue" => Right(DayOfWeek.Tue)
      case "wed" => Right(DayOfWeek.Wed)
      case "thu" => Right(DayOfWeek.Thu)
      case "fri" => Right(DayOfWeek.Fri)
      case "sat" => Right(DayOfWeek.Sat)
      case _     => Left(s"Unknown day: '$raw'")

  given Decoder[DayBlock] = Decoder.instance { cursor =>
    for
      day   <- cursor.get[DayOfWeek]("day")
      slots <- cursor.get[Vector[Option[Slot]]]("slots")
      block <- Either.cond(
        slots.length == 6,
        DayBlock(day, slots),
        DecodingFailure(s"'slots' must have 6 entries, got ${slots.length}", cursor.history)
      )
    yield block
  }

  given Decoder[WeekType] = Decoder.decodeString.emap: raw =>
    raw.toLowerCase match
      case "odd"  => Right(WeekType.Odd)
      case "even" => Right(WeekType.Even)
      case _      => Left(s"Unknown week type: '$raw'")

  given Decoder[Week] = Decoder.instance { cursor =>
    for
      weekType <- cursor.get[WeekType]("weekType")
      days     <- cursor.get[Vector[DayBlock]]("days")
      week     <- Either.cond(
        days.length == 6,
        Week(weekType, days),
        DecodingFailure(s"'days' must have 6 entries, got ${days.length}", cursor.history)
      )
    yield week
  }

  given Decoder[Meta] = Decoder.forProduct3(
    "version", "groupName", "createdAt"
  )(Meta.apply)

  given Decoder[ScheduleFile] = Decoder.instance { cursor =>
    for
      meta  <- cursor.get[Meta]("meta")
      weeks <- cursor.get[Vector[Week]]("weeks")
      file  <- Either.cond(
        weeks.length == 2,
        ScheduleFile(meta, weeks),
        DecodingFailure(s"'weeks' must have 2 entries, got ${weeks.length}", cursor.history)
      )
    yield file
  }

// ─────────────────────────────────────────────────────────────────────────────
// Публичный API
// ─────────────────────────────────────────────────────────────────────────────

object JsonDecoder:
  import Decoders.given

  /** Чистая функция: строка → Either. Никаких IO, тестируется напрямую. */
  def fromString(json: String): Either[DecodeError, ScheduleFile] =
    parse(json) match
      case Left(failure)    => Left(DecodeError.ParseError(failure.message))
      case Right(jsonValue) =>
        jsonValue.as[ScheduleFile].left.map { f =>
          DecodeError.SchemaError(formatFailure(f))
        }

  /** IO-обёртка: чтение файла — это побочный эффект, описываем честно. */
  def fromFile(path: java.nio.file.Path): IO[Either[DecodeError, ScheduleFile]] =
    IO(java.nio.file.Files.readString(path))
      .attempt
      .map {
        case Left(ex)      => Left(DecodeError.IoError(path.toString, ex))
        case Right(content) => fromString(content)
      }

  def fromFilePath(pathStr: String): IO[Either[DecodeError, ScheduleFile]] =
    fromFile(java.nio.file.Path.of(pathStr))

  private def formatFailure(f: DecodingFailure): String =
    val path = CursorOp.opsToPath(f.history)
    if path.isEmpty then f.message else s"${f.message} (at: $path)"