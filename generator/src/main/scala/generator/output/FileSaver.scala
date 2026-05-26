package generator.output


import cats.effect.IO

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

// ─────────────────────────────────────────────────────────────────────────────
// Ошибки записи
// ─────────────────────────────────────────────────────────────────────────────

sealed trait SaveError:
  def message: String

object SaveError:
  /** Директория не существует или нет прав на запись */
  final case class DirectoryNotAccessible(path: String, cause: Throwable) extends SaveError:
    def message: String =
      s"Cannot write to '${path}': ${cause.getMessage}"

  /** Файл уже существует и перезапись не разрешена */
  final case class FileAlreadyExists(path: String) extends SaveError:
    def message: String =
      s"File already exists: '$path'. Use overwrite = true to replace it."

  /** Любая другая IO-ошибка при записи */
  final case class WriteError(path: String, cause: Throwable) extends SaveError:
    def message: String =
      s"Failed to write '$path': ${cause.getMessage}"

// ─────────────────────────────────────────────────────────────────────────────
// Публичный API
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Чистая IO-обёртка над записью файла на диск (FR6.3).
 *
 * Не содержит бизнес-логики — только описание эффекта записи.
 * Реальное исполнение происходит в Main через IOApp.
 */
object FileSaver:

  /**
   * Записывает строку в файл по указанному пути.
   *
   * @param path      путь к файлу (включая имя и расширение)
   * @param content   содержимое файла (HTML, SVG и т.п.)
   * @param overwrite разрешить перезапись существующего файла (по умолчанию true)
   * @return          IO[Either[SaveError, Unit]]
   */
  def save(
            path:      Path,
            content:   String,
            overwrite: Boolean = true
          ): IO[Either[SaveError, Unit]] =
    for
      parentOk <- ensureParentExists(path)
      result   <- parentOk match
        case Left(err) => IO.pure(Left(err))
        case Right(_)  => writeFile(path, content, overwrite)
    yield result

  /** Удобный алиас: принимает строковый путь вместо Path */
  def saveToPath(
                  pathStr:   String,
                  content:   String,
                  overwrite: Boolean = true
                ): IO[Either[SaveError, Unit]] =
    save(Path.of(pathStr), content, overwrite)

  // ── Приватные IO-функции ───────────────────────────────────────────────────

  /**
   * Создаёт родительские директории, если их нет.
   * Если path не имеет родителя (файл в корне) — пропускает.
   */
  private def ensureParentExists(path: Path): IO[Either[SaveError, Unit]] =
    IO {
      Option(path.getParent) match
        case None         => Right(())
        case Some(parent) =>
          if Files.exists(parent) then Right(())
          else
            Files.createDirectories(parent)
            Right(())
    }.handleErrorWith { ex =>
      IO.pure(Left(SaveError.DirectoryNotAccessible(path.toString, ex)))
    }

  /** Выполняет непосредственную запись файла. */
  private def writeFile(
                         path:      Path,
                         content:   String,
                         overwrite: Boolean
                       ): IO[Either[SaveError, Unit]] =
    if !overwrite && Files.exists(path) then
      IO.pure(Left(SaveError.FileAlreadyExists(path.toString)))
    else
      IO {
        val options =
          if overwrite then
            Array(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
          else
            Array(StandardOpenOption.CREATE_NEW)

        Files.write(path, content.getBytes(StandardCharsets.UTF_8), options*)
        ()
      }.attempt.map {
        case Left(ex)  => Left(SaveError.WriteError(path.toString, ex))
        case Right(()) => Right(())
      }