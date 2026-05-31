package generator.render


import generator.theme.Theme
import shared.*

/**
 * Альтернативный HTML-рендерер: таблица «Дни × Недели».
 *
 * Строки — дни (Пн–Сб), столбцы — чётная и нечётная недели.
 * В ячейках — список пар с деталями. Время не выводится (только номер пары).
 * Оформление тёмное, фиксированное (Theme игнорируется).
 */
object ThirdHtmlRenderer extends Renderer[String]:

  override def render(
                       schedule: ScheduleFile,
                       theme:    Theme
                     ): Either[RenderError, String] =
    val odd  = schedule.weeks.find(_.weekType == WeekType.Odd)
    val even = schedule.weeks.find(_.weekType == WeekType.Even)

    (odd, even) match
      case (Some(oddWeek), Some(evenWeek)) =>
        Right(buildDocument(schedule.meta, oddWeek, evenWeek))
      case _ =>
        Left(RenderError.InvalidModel(
          "ScheduleFile must contain both Odd and Even weeks"
        ))

  // ── Сборка HTML-документа ───────────────────────────────────────────────
  private def buildDocument(meta: Meta, odd: Week, even: Week): String =
    s"""<!DOCTYPE html>
       |<html lang="ru">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <title>Расписание — ${escape(meta.groupName)}</title>
       |  <style>
       |${buildCss()}
       |  </style>
       |</head>
       |<body>
       |  <header class="header">
       |    <h1>Расписание группы ${escape(meta.groupName)}</h1>
       |    <p class="meta">Сформировано: ${escape(meta.createdAt)}</p>
       |  </header>
       |  <main>
       |    ${renderTable(odd, even)}
       |  </main>
       |</body>
       |</html>""".stripMargin

  // ── Тёмный CSS (жёстко зашит) ───────────────────────────────────────────
  private def buildCss(): String =
    """    :root {
      |      --bg: #121212;
      |      --surface: #1e1e1e;
      |      --text: #e0e0e0;
      |      --text-secondary: #b0b0b0;
      |      --border: #333;
      |      --odd-accent: #4a90d9;
      |      --even-accent: #d97a4a;
      |      --font: 'Segoe UI', system-ui, sans-serif;
      |    }
      |    * { box-sizing: border-box; margin: 0; padding: 0; }
      |    body {
      |      background: var(--bg);
      |      color: var(--text);
      |      font-family: var(--font);
      |      padding: 24px;
      |    }
      |    .header {
      |      background: var(--surface);
      |      padding: 16px 20px;
      |      border-radius: 8px;
      |      margin-bottom: 24px;
      |    }
      |    .header h1 { font-size: 1.6em; font-weight: 600; }
      |    .header .meta { font-size: 0.85em; color: var(--text-secondary); margin-top: 4px; }
      |    table {
      |      width: 100%;
      |      border-collapse: collapse;
      |      background: var(--surface);
      |      border-radius: 8px;
      |      overflow: hidden;
      |    }
      |    th, td {
      |      border: 1px solid var(--border);
      |      padding: 12px;
      |      vertical-align: top;
      |    }
      |    th {
      |      background: #2a2a2a;
      |      font-weight: 600;
      |      text-align: center;
      |      color: var(--text);
      |    }
      |    .day-label {
      |      background: #2a2a2a;
      |      font-weight: 600;
      |      text-align: center;
      |      width: 60px;
      |    }
      |    .lesson {
      |      padding: 8px 8px 8px 12px;
      |      margin-bottom: 8px;
      |      border-left: 4px solid #888;
      |      background: rgba(255,255,255,0.03);
      |      border-radius: 4px;
      |    }
      |    .lesson:last-child { margin-bottom: 0; }
      |    .lesson.odd { border-left-color: var(--odd-accent); }
      |    .lesson.even { border-left-color: var(--even-accent); }
      |    .lesson .pair-num {
      |      font-size: 0.8em;
      |      color: var(--text-secondary);
      |      margin-bottom: 4px;
      |    }
      |    .lesson .subject {
      |      font-weight: 600;
      |      font-size: 1em;
      |      margin-bottom: 4px;
      |    }
      |    .lesson .meta {
      |      font-size: 0.8em;
      |      color: var(--text-secondary);
      |      display: flex;
      |      flex-wrap: wrap;
      |      gap: 6px;
      |      align-items: center;
      |    }
      |    .type-badge {
      |      display: inline-block;
      |      background: #3a3a3a;
      |      color: #ccc;
      |      padding: 1px 6px;
      |      font-size: 0.75em;
      |      border-radius: 3px;
      |      text-transform: uppercase;
      |      letter-spacing: 0.5px;
      |    }
      |    .subgroups {
      |      font-size: 0.75em;
      |      color: var(--text-secondary);
      |      margin-top: 4px;
      |    }
      |    .empty-cell {
      |      color: #666;
      |      font-style: italic;
      |    }
      |""".stripMargin

  // ── Таблица дни × недели ────────────────────────────────────────────────
  private def renderTable(odd: Week, even: Week): String =
    val days = Vector(
      DayOfWeek.Mon, DayOfWeek.Tue, DayOfWeek.Wed,
      DayOfWeek.Thu, DayOfWeek.Fri, DayOfWeek.Sat
    )
    val oddMap  = weekToDayMap(odd)
    val evenMap = weekToDayMap(even)

    val rows = days.map { day =>
      val oddSlots  = oddMap.getOrElse(day, Vector.empty)
      val evenSlots = evenMap.getOrElse(day, Vector.empty)
      s"""        <tr>
         |          <td class="day-label">${dayName(day)}</td>
         |          <td>${renderCell(oddSlots, "odd")}</td>
         |          <td>${renderCell(evenSlots, "even")}</td>
         |        </tr>""".stripMargin
    }.mkString("\n")

    s"""      <table>
       |        <thead>
       |          <tr>
       |            <th></th>
       |            <th>Нечётная неделя</th>
       |            <th>Чётная неделя</th>
       |          </tr>
       |        </thead>
       |        <tbody>
       |$rows
       |        </tbody>
       |      </table>""".stripMargin

  // ── Вспомогательные функции ─────────────────────────────────────────────

  /** Строит Map[DayOfWeek, Vector[(номер_пары, Slot)]] для недели */
  private def weekToDayMap(week: Week): Map[DayOfWeek, Vector[(Int, Slot)]] =
    week.days.flatMap { dayBlock =>
      val slots = dayBlock.slots.zipWithIndex.collect {
        case (Some(slot), idx) => (idx + 1, slot) // idx 0 → пара 1
      }
      if (slots.isEmpty) None
      else Some(dayBlock.day -> slots.toVector)
    }.toMap

  private def renderCell(slots: Vector[(Int, Slot)], weekClass: String): String =
    if (slots.isEmpty)
      """            <div class="empty-cell">—</div>"""
    else
      slots.map { case (pairNum, slot) =>
        s"""            <div class="lesson $weekClass">
           |              <div class="pair-num">Пара $pairNum</div>
           |              <div class="subject">${escape(slot.subject)}</div>
           |              <div class="meta">
           |                <span class="type-badge">${lessonTypeName(slot.lessonType)}</span>
           |                <span>${escape(slot.teacher)}</span>
           |                <span>${escape(slot.room)}</span>
           |              </div>
           |${if (slot.subgroups.nonEmpty) s"""              <div class="subgroups">Подгр.: ${slot.subgroups.map(escape).mkString(", ")}</div>""" else ""}
           |            </div>""".stripMargin
      }.mkString("\n")

  private def dayName(day: DayOfWeek): String = day match
    case DayOfWeek.Mon => "Пн"
    case DayOfWeek.Tue => "Вт"
    case DayOfWeek.Wed => "Ср"
    case DayOfWeek.Thu => "Чт"
    case DayOfWeek.Fri => "Пт"
    case DayOfWeek.Sat => "Сб"

  private def lessonTypeName(t: LessonType): String = t match
    case LessonType.Lecture  => "лекция"
    case LessonType.Practice => "практика"
    case LessonType.Lab      => "лаб."

  private def escape(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")