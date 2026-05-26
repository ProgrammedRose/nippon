package generator.theme


import cats.effect.IO
import cats.syntax.either.*
import io.circe.*
import io.circe.parser.*

// ─────────────────────────────────────────────────────────────────────────────
// Ошибки загрузки темы
// ─────────────────────────────────────────────────────────────────────────────

sealed trait ThemeError:
  def message: String

object ThemeError:
  final case class NotFound(name: String) extends ThemeError:
    def message: String =
      s"Theme '$name' not found. Available themes: ${ThemeLoader.builtinNames.mkString(", ")}"

  final case class ParseError(name: String, details: String) extends ThemeError:
    def message: String = s"Cannot parse theme '$name': $details"

// ─────────────────────────────────────────────────────────────────────────────
// Circe-декодеры
// ─────────────────────────────────────────────────────────────────────────────

private object ThemeDecoders:

  given Decoder[LessonColors] = Decoder.forProduct3(
    "background", "text", "accent"
  )(LessonColors.apply)

  given Decoder[LessonPalette] = Decoder.forProduct3(
    "lecture", "practice", "lab"
  )(LessonPalette.apply)

  given Decoder[Theme] = Decoder.forProduct19(
    "name", "description",
    "pageBackground", "pageText",
    "headerBackground", "headerText", "headerFontWeight",
    "borderColor", "borderRadius", "cellPadding", "emptyBackground",
    "fontFamily", "fontSizeBase", "fontSizeSmall", "fontSizeMeta",
    "weekBadgeBackground", "weekBadgeText", "weekBadgeBorder",
    "lessons"
  )(Theme.apply)

// ─────────────────────────────────────────────────────────────────────────────
// Загрузчик тем
// ─────────────────────────────────────────────────────────────────────────────

object ThemeLoader:
  import ThemeDecoders.given

  val builtinNames: List[String] = List("default", "dark")

  /**
   * Загружает тему по имени или пути.
   * Возвращает IO — все побочные эффекты (чтение файла) описаны, но не выполнены.
   */
  def load(name: String): IO[Either[ThemeError, Theme]] =
    loadFromClasspath(name.toLowerCase)
      .flatMap {
        case Some(result) => IO.pure(result)
        case None         => loadFromFilesystem(name)
      }

  def loadDefault(): IO[Either[ThemeError, Theme]] =
    load("default")

  def listAvailable(): IO[List[Either[ThemeError, Theme]]] =
    builtinNames
      .foldLeft(IO.pure(List.empty[Either[ThemeError, Theme]])) { (acc, name) =>
        acc.flatMap(list => load(name).map(list :+ _))
      }

  // ── Приватные IO-функции ───────────────────────────────────────────────────

  private def loadFromClasspath(name: String): IO[Option[Either[ThemeError, Theme]]] =
    IO {
      Option(getClass.getClassLoader.getResourceAsStream(s"themes/$name.json"))
    }.flatMap {
      case None         => IO.pure(None)
      case Some(stream) =>
        IO(scala.io.Source.fromInputStream(stream, "UTF-8").mkString)
          .attempt
          .map {
            case Left(ex)      => Some(Left(ThemeError.ParseError(name, ex.getMessage)))
            case Right(content) => Some(parseThemeJson(name, content))
          }
    }

  private def loadFromFilesystem(path: String): IO[Either[ThemeError, Theme]] =
    IO(java.io.File(path))
      .flatMap { file =>
        if file.exists() && file.canRead then
          IO(scala.io.Source.fromFile(file, "UTF-8").mkString)
            .attempt
            .map {
              case Left(ex)       => Left(ThemeError.ParseError(path, ex.getMessage))
              case Right(content) => parseThemeJson(path, content)
            }
        else
          IO.pure(Left(ThemeError.NotFound(path)))
      }

  private def parseThemeJson(name: String, json: String): Either[ThemeError, Theme] =
    parse(json)
      .flatMap(_.as[Theme])
      .leftMap(err => ThemeError.ParseError(name, err.getMessage))