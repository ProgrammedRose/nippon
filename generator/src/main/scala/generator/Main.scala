package generator

import cats.effect.{ExitCode, IO, IOApp}
import generator.output.{FileSaver, SaveError}
import generator.parser.{DecodeError, JsonDecoder}
import generator.render.{HtmlRenderer, RenderError, Renderer, SecHtmlRenderer, SvgRenderer, ThirdHtmlRenderer}
import generator.theme.{Theme, ThemeError, ThemeLoader}
import shared.ScheduleFile

import scala.annotation.tailrec

/** Аргументы командной строки генератора. */
final case class Args(
                       inputPath:  String,
                       outputPath: String,
                       themeName:  String,
                       format:     String
                     )

/** Точка входа в генератор расписаний. */
object Main extends IOApp:
  
  override def run(args: List[String]): IO[ExitCode] =
    parseArgs(args) match
      case Left(usageError) =>
        for
          _ <- printError(usageError)
          _ <- printUsage
        yield ExitCode.Error
      case Right(cliArgs) =>
        pipeline(cliArgs).map {
          case Right(_)    => ExitCode.Success
          case Left(error) => ExitCode.Error
        }
  
  /** Загрузка → тема → рендер → сохранение. */
  private def pipeline(args: Args): IO[Either[AppError, Unit]] =
    for
      scheduleE <- loadSchedule(args.inputPath)
      themeE    <- loadTheme(args.themeName)
      resultE   <- IO.pure {
        (scheduleE, themeE) match
          case (Right(schedule), Right(theme)) =>
            selectRenderer(args.format).flatMap { renderer =>
              renderer.render(schedule, theme) match
                case Right(content) => Right((content, args.outputPath))
                case Left(err)      => Left(AppError.Render(err))
            }
          case (Left(err), _) => Left(err)
          case (_, Left(err)) => Left(err)
      }
      saveE <- resultE match
        case Left(err) => IO.pure(Left(err))
        case Right((content, path)) => saveResult(path, content)
    yield saveE
  
  private def loadSchedule(inputPath: String): IO[Either[AppError, ScheduleFile]] =
    JsonDecoder.fromFilePath(inputPath).flatMap {
      case Left(err) =>
        for
          _ <- printError(s"[json] ${err.message}")
        yield Left(AppError.Decode(err))
      case Right(schedule) =>
        for
          _ <- printInfo(s"Schedule is loaded for group: ${schedule.meta.groupName}")
        yield Right(schedule)
    }
  
  private def loadTheme(themeName: String): IO[Either[AppError, Theme]] =
    ThemeLoader.load(themeName).flatMap {
      case Left(err) =>
        for
          _ <- printError(s"[theme] ${err.message}")
        yield Left(AppError.Theme(err))
      case Right(theme) =>
        for
          _ <- printInfo(s"Loaded theme: ${theme.name}")
        yield Right(theme)
    }
  
  private def saveResult(outputPath: String, content: String): IO[Either[AppError, Unit]] =
    FileSaver.saveToPath(outputPath, content).flatMap {
      case Left(err) =>
        for
          _ <- printError(s"[save] ${err.message}")
        yield Left(AppError.Save(err))
      case Right(_) =>
        for
          _ <- printSuccess(s"File saved: $outputPath")
        yield Right(())
    }
  
  /** Выбирает рендерер по строке формата. */
  private def selectRenderer(format: String): Either[AppError, Renderer[String]] =
    format.toLowerCase match
      case "html" => Right(HtmlRenderer)
      case "svg"  => Right(SvgRenderer)
      case "sec"  => Right(SecHtmlRenderer)
      case "thrd" => Right(ThirdHtmlRenderer)
      case other  => Left(AppError.Render(RenderError.NotImplemented(other)))
  
  /** Парсит аргументы командной строки. */
  private def parseArgs(args: List[String]): Either[String, Args] = {
    @tailrec
    def loop(rem: List[String], acc: Map[String, String]): Either[String, Map[String, String]] =
      rem match
        case Nil => Right(acc)
        case flag :: value :: rest =>
          if (!flag.startsWith("--")) Left(s"Invalid flag: $flag")
          else loop(rest, acc + (flag -> value))
        case _ => Left("Odd number of arguments") // сработает, если остался один элемент
    
    loop(args, Map.empty).flatMap { map =>
      for
        input  <- map.get("--input").toRight("Missing --input")
        output <- map.get("--output").toRight("Missing --output")
        theme   = map.getOrElse("--theme", "default")
        format  = map.getOrElse("--format", "html")
      yield Args(input, output, theme, format)
    }
  }
  
  private def printInfo(msg: String): IO[Unit] = IO.println(msg)
  private def printSuccess(msg: String): IO[Unit] = IO.println(s"\n$msg")
  private def printError(msg: String): IO[Unit] = IO.println(msg)
  private def printUsage: IO[Unit] = IO.println {
    """
      |How to use:
      |  generator --input <path to JSON> --output <file path> [--theme default] [--format html]
      |
      |Themes:   default, dark
      |Files: html (svg — in development)
      |
      |Example:
      |  generator --input schedule.json --output out/schedule.html --theme dark
      |""".stripMargin
  }

/** Объединённая ошибка приложения. */
sealed trait AppError

object AppError:
  final case class Decode(error: DecodeError) extends AppError
  final case class Theme(error: ThemeError)   extends AppError
  final case class Render(error: RenderError) extends AppError
  final case class Save(error: SaveError)     extends AppError