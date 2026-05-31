package generator.render

import generator.theme.Theme
import shared.ScheduleFile

/**
 * Заглушка SVG-рендерера (FR3.2).
 *
 * Архитектурный слот: когда SVG-формат будет реализован,
 * этот файл заменяется без изменений в остальном коде.
 * Main.scala выбирает рендерер по имени формата — новый формат
 * регистрируется там одной строкой.
 */
object SvgRenderer extends Renderer[String]:
  override def render(
                       schedule: ScheduleFile,
                       theme:    Theme
                     ): Either[RenderError, String] =
    Left(RenderError.NotImplemented("SVG"))