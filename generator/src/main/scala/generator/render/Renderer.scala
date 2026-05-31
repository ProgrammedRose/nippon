package generator.render


import generator.theme.Theme
import shared.ScheduleFile

// ─────────────────────────────────────────────────────────────────────────────
// Ошибки рендеринга
// ─────────────────────────────────────────────────────────────────────────────

sealed trait RenderError:
  def message: String

object RenderError:
  /** Формат заявлен, но ещё не реализован (SVG, PNG и т.п.) */
  final case class NotImplemented(format: String) extends RenderError:
    def message: String = s"Renderer for format '$format' is not implemented yet"

  /** Данные модели не позволяют построить корректный документ */
  final case class InvalidModel(details: String) extends RenderError:
    def message: String = s"Cannot render: $details"

// ─────────────────────────────────────────────────────────────────────────────
// Абстракция рендерера
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Чистая функция: доменная модель + тема → документ типа A.
 *
 * Параметр A — тип результата:
 *   - HtmlRenderer: A = String (готовый HTML-документ)
 *   - SvgRenderer:  A = String (SVG-разметка)
 *   - PngRenderer:  A = Array[Byte] (бинарный файл)
 *
 * render не имеет побочных эффектов и не выполняет IO.
 * Запись результата на диск — обязанность FileSaver.
 *
 * FR3.2: архитектура допускает добавление новых форматов
 * без изменения ядра — достаточно добавить новую реализацию trait.
 */
trait Renderer[A]:
  def render(schedule: ScheduleFile, theme: Theme): Either[RenderError, A]