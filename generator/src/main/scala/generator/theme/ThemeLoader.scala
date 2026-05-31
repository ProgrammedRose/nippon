package generator.theme

import cats.effect.IO
import io.circe.{Decoder, Json}
import io.circe.parser.parse
import scala.io.{Codec, Source}
import scala.util.Using

/** Чуть более подробный вывод ошибок */
sealed trait ThemeError:
  def message: String

object ThemeError:
  final case class NotFound(name: String) extends ThemeError:
    def message: String =
      s"Theme '$name' not found. Available themes: ${ThemeLoader.builtinNames.mkString(", ")}"
  
  final case class ParseError(name: String, details: String) extends ThemeError:
    def message: String = s"Cannot parse theme '$name': $details"

/** Основной функционал загрузчика */
object ThemeLoader:
  // внизу страницы
  import ThemeDecoders.given
  val builtinNames: List[String] = List("default", "dark")
  
  def load(name: String): IO[Either[ThemeError, Theme]] =
    loadFromClasspath(name.toLowerCase).flatMap {
      case Some(result) => IO.pure(result)
      case None         => loadFromFilesystem(name)
    }
  
  def loadDefault(): IO[Either[ThemeError, Theme]] =
    load("default")
  
  private def loadFromClasspath(name: String): IO[Option[Either[ThemeError, Theme]]] =
    IO.blocking {
      Option(getClass.getClassLoader.getResourceAsStream(s"themes/$name.json"))
    }.flatMap {
      case None => IO.pure(None)
      case Some(stream) =>
        IO.blocking {
          Using.resource(Source.fromInputStream(stream)(Codec.UTF8)) { src =>
            src.mkString
          }
        }.attempt.map {
          case Left(ex)       => Some(Left(ThemeError.ParseError(name, ex.getMessage)))
          case Right(content) => Some(parseThemeJson(name, content))
        }
    }
  
  private def loadFromFilesystem(path: String): IO[Either[ThemeError, Theme]] =
    IO.blocking {
      val file = new java.io.File(path)
      if file.exists() && file.canRead then
        Using(Source.fromFile(file, "UTF-8"))(_.mkString).toEither match {
          case Left(ex) => Left(ThemeError.ParseError(path, ex.getMessage))
          case Right(content) => parseThemeJson(path, content)
        }
      else
        Left(ThemeError.NotFound(path))
    }
  
  private def parseThemeJson(name: String, json: String): Either[ThemeError, Theme] =
    parse(json) match
      case Left(err) => Left(ThemeError.ParseError(name, err.getMessage))
      case Right(j)  =>
        j.as[Theme] match
          case Left(err) => Left(ThemeError.ParseError(name, err.getMessage))
          case Right(t)  => Right(t)

private object ThemeDecoders:
  
  given Decoder[LessonColors] =
    Decoder.forProduct3("background", "text", "accent")(LessonColors.apply)
  
  given Decoder[LessonPalette] =
    Decoder.forProduct3("lecture", "practice", "lab")(LessonPalette.apply)
  
  given Decoder[Theme] = Decoder.forProduct19(
    "name", "description",
    "pageBackground", "pageText",
    "headerBackground", "headerText", "headerFontWeight",
    "borderColor", "borderRadius", "cellPadding", "emptyBackground",
    "fontFamily", "fontSizeBase", "fontSizeSmall", "fontSizeMeta",
    "weekBadgeBackground", "weekBadgeText", "weekBadgeBorder",
    "lessons"
  )(Theme.apply)