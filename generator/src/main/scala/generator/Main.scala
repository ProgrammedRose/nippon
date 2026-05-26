package generator


import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.either.*

import generator.output.{FileSaver, SaveError}
import generator.parser.{DecodeError, JsonDecoder}
import generator.render.{HtmlRenderer, RenderError, Renderer, SvgRenderer}
import generator.theme.{Theme, ThemeError, ThemeLoader}
import shared.ScheduleFile

import java.nio.file.Path

// ─────────────────────────────────────────────────────────────────────────────
// Аргументы командной строки
// ─────────────────────────────────────────────────────────────────────────────

final case class Args(
                       inputPath:  String,
                       outputPath: String,
                       themeName:  String,
                       format:     String
                     )

// ─────────────────────────────────────────────────────────────────────────────
// Точка входа
// ─────────────────────────────────────────────────────────────────────────────

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    parseArgs(args) match
      case Left(usageError) =>
        printError(usageError) *>
          printUsage *>
          IO.pure(ExitCode.Error)

      case Right(cliArgs) =>
        pipeline(cliArgs).map:
          case Right(_)    => ExitCode.Success
          case Left(error) => ExitCode.Error

  // ── Пайплайн ────────────────────────────────────────────────────────────────
  //
  // Каждый шаг возвращает IO[Either[AppError, A]].
  // flatMap разворачивает IO, для Either используем .flatMap внутри IO.
  // При первой же Left вся цепочка «замыкается» — следующие шаги не выполняются.

  private def pipeline(args: Args): IO[Either[AppError, Unit]] =
    for
      scheduleE <- loadSchedule(args.inputPath)
      themeE    <- loadTheme(args.themeName)
      resultE   <- IO.pure:
        for
          schedule <- scheduleE
          theme    <- themeE
          renderer <- selectRenderer(args.format)
          content  <- renderer.render(schedule, theme).leftMap(AppError.Render.apply)
        yield (content, args.outputPath)
      saveE     <- resultE match
        case Left(err)              => IO.pure(Left(err))
        case Right((content, path)) => saveResult(path, content)
    yield saveE

  // ── Шаги пайплайна ──────────────────────────────────────────────────────────

  private def loadSchedule(inputPath: String): IO[Either[AppError, ScheduleFile]] =
    JsonDecoder
      .fromFilePath(inputPath)
      .flatMap:
        case Left(err)       =>
          printError(s"[json] ${err.message}") *>
            IO.pure(Left(AppError.Decode(err)))
        case Right(schedule) =>
          printInfo(s"Расписание загружено: ${schedule.meta.groupName}") *>
            IO.pure(Right(schedule))

  private def loadTheme(themeName: String): IO[Either[AppError, Theme]] =
    ThemeLoader
      .load(themeName)
      .flatMap:
        case Left(err)    =>
          printError(s"[theme] ${err.message}") *>
            IO.pure(Left(AppError.Theme(err)))
        case Right(theme) =>
          printInfo(s"Тема загружена: ${theme.name}") *>
            IO.pure(Right(theme))

  private def saveResult(outputPath: String, content: String): IO[Either[AppError, Unit]] =
    FileSaver
      .saveToPath(outputPath, content)
      .flatMap:
        case Left(err) =>
          printError(s"[save] ${err.message}") *>
            IO.pure(Left(AppError.Save(err)))
        case Right(_)  =>
          printSuccess(s"Файл сохранён: $outputPath") *>
            IO.pure(Right(()))

  // ── Выбор рендерера (чистая функция) ─────────────────────────────────────────
  //
  // FR3.2: добавление нового формата = одна строка в этом match.
  // Никакие другие файлы менять не нужно.

  private def selectRenderer(format: String): Either[AppError, Renderer[String]] =
    format.toLowerCase match
      case "html" => Right(HtmlRenderer)
      case "svg"  => Right(SvgRenderer)
      case other  =>
        Left(AppError.Render(RenderError.NotImplemented(other)))

  // ── Парсинг аргументов CLI (чистая функция) ──────────────────────────────────

  private def parseArgs(args: List[String]): Either[String, Args] =

    // Рекурсивно обходим список аргументов, собирая Map флагов
    def collect(
                 remaining: List[String],
                 acc:       Map[String, String]
               ): Either[String, Map[String, String]] =
      remaining match
        case Nil                           => Right(acc)
        case flag :: value :: rest
          if flag.startsWith("--")        => collect(rest, acc + (flag -> value))
        case flag :: _ if flag.startsWith("--") =>
          Left(s"Флаг '$flag' указан без значения")
        case unknown :: _                  =>
          Left(s"Неизвестный аргумент: '$unknown'")

    collect(args, Map.empty).flatMap: flags =>
      for
        input  <- flags.get("--input").toRight("Не указан обязательный флаг --input")
        output <- flags.get("--output").toRight("Не указан обязательный флаг --output")
        theme   = flags.getOrElse("--theme",  "default")
        format  = flags.getOrElse("--format", "html")
      yield Args(input, output, theme, format)

  // ── Вывод в консоль (IO-функции) ──────────────────────────────────────────────

  private def printInfo(msg: String):    IO[Unit] = IO.println(s"  ✓ $msg")
  private def printSuccess(msg: String): IO[Unit] = IO.println(s"\n✔ $msg")
  private def printError(msg: String):   IO[Unit] = IO.println(s"✗ $msg")
  private def printUsage: IO[Unit] = IO.println:
    """
      |Использование:
      |  generator --input <путь к JSON> --output <путь к файлу> [--theme default] [--format html]
      |
      |Доступные темы:   default, dark
      |Доступные форматы: html (svg — в разработке)
      |
      |Пример:
      |  generator --input schedule.json --output out/schedule.html --theme dark
      |""".stripMargin

// ─────────────────────────────────────────────────────────────────────────────
// Единая иерархия ошибок приложения
// ─────────────────────────────────────────────────────────────────────────────

// Оборачивает ошибки всех модулей в один тип —
// пайплайн оперирует только AppError, не зная деталей каждого слоя.

sealed trait AppError

object AppError:
  final case class Decode(error: DecodeError) extends AppError
  final case class Theme(error: ThemeError)   extends AppError
  final case class Render(error: RenderError) extends AppError
  final case class Save(error: SaveError)     extends AppError