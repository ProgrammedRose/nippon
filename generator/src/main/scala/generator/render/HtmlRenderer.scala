package generator.render

import generator.theme.Theme
import shared.*

/**
 * Рендерер HTML-расписания (FR5).
 *
 * Генерирует самодостаточный HTML-документ: все стили встроены,
 * внешних зависимостей нет — файл открывается в браузере офлайн.
 *
 * Вся логика построена из чистых приватных функций:
 * renderWeek → renderTable → renderRow → renderCell → renderSlot.
 * Каждая принимает данные и возвращает строку — никаких мутаций.
 */
object HtmlRenderer extends Renderer[String]:

  // ── Публичный метод ────────────────────────────────────────────────────────

  override def render(
                       schedule: ScheduleFile,
                       theme:    Theme
                     ): Either[RenderError, String] =
    val odd  = schedule.weeks.find(_.weekType == WeekType.Odd)
    val even = schedule.weeks.find(_.weekType == WeekType.Even)

    (odd, even) match
      case (Some(oddWeek), Some(evenWeek)) =>
        Right(buildDocument(schedule.meta, oddWeek, evenWeek, theme))
      case _ =>
        Left(RenderError.InvalidModel(
          "ScheduleFile must contain both Odd and Even weeks"
        ))

  // ── Сборка документа ────────────────────────────────────────────────────────

  private def buildDocument(
                             meta:    Meta,
                             odd:     Week,
                             even:    Week,
                             theme:   Theme
                           ): String =
    s"""<!DOCTYPE html>
       |<html lang="ru">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <meta name="generator" content="schedule-generator">
       |  <meta name="created-at" content="${meta.createdAt}">
       |  <title>Расписание — ${escape(meta.groupName)}</title>
       |  <style>
       |${buildCss(theme)}
       |  </style>
       |</head>
       |<body>
       |  <header class="page-header">
       |    <div class="header-inner">
       |      <h1 class="group-title">Расписание группы <span>${escape(meta.groupName)}</span></h1>
       |      <p class="meta-info">Сформировано: ${escape(meta.createdAt)}</p>
       |    </div>
       |  </header>
       |  <main class="schedule-main">
       |${renderWeek(odd,  theme)}
       |${renderWeek(even, theme)}
       |  </main>
       |</body>
       |</html>""".stripMargin

  // ── CSS ────────────────────────────────────────────────────────────────────

  private def buildCss(t: Theme): String =
    s"""    :root {
       |      --page-bg:          ${t.pageBackground};
       |      --page-text:        ${t.pageText};
       |      --header-bg:        ${t.headerBackground};
       |      --header-text:      ${t.headerText};
       |      --header-fw:        ${t.headerFontWeight};
       |      --border:           ${t.borderColor};
       |      --radius:           ${t.borderRadius};
       |      --cell-pad:         ${t.cellPadding};
       |      --empty-bg:         ${t.emptyBackground};
       |      --font:             ${t.fontFamily};
       |      --fs-base:          ${t.fontSizeBase};
       |      --fs-small:         ${t.fontSizeSmall};
       |      --fs-meta:          ${t.fontSizeMeta};
       |      --badge-bg:         ${t.weekBadgeBackground};
       |      --badge-text:       ${t.weekBadgeText};
       |      --badge-border:     ${t.weekBadgeBorder};
       |      --lk-bg:            ${t.lessons.lecture.background};
       |      --lk-text:          ${t.lessons.lecture.text};
       |      --lk-accent:        ${t.lessons.lecture.accent};
       |      --pr-bg:            ${t.lessons.practice.background};
       |      --pr-text:          ${t.lessons.practice.text};
       |      --pr-accent:        ${t.lessons.practice.accent};
       |      --lab-bg:           ${t.lessons.lab.background};
       |      --lab-text:         ${t.lessons.lab.text};
       |      --lab-accent:       ${t.lessons.lab.accent};
       |    }
       |
       |    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
       |
       |    body {
       |      background: var(--page-bg);
       |      color:      var(--page-text);
       |      font-family:var(--font);
       |      font-size:  var(--fs-base);
       |      line-height:1.45;
       |    }
       |
       |    /* ── Шапка ── */
       |    .page-header {
       |      background:    var(--header-bg);
       |      color:         var(--header-text);
       |      padding:       24px 32px 20px;
       |      margin-bottom: 28px;
       |    }
       |    .header-inner { max-width: 1200px; margin: 0 auto; }
       |    .group-title {
       |      font-size:   var(--fs-meta);
       |      font-weight: var(--header-fw);
       |      letter-spacing: 0.3px;
       |    }
       |    .group-title span { opacity: 0.85; }
       |    .meta-info {
       |      margin-top: 4px;
       |      font-size:  var(--fs-small);
       |      opacity:    0.6;
       |    }
       |
       |    /* ── Основная область ── */
       |    .schedule-main {
       |      max-width: 1200px;
       |      margin:    0 auto;
       |      padding:   0 24px 48px;
       |    }
       |
       |    /* ── Блок недели ── */
       |    .week-section  { margin-bottom: 40px; }
       |    .week-heading  {
       |      display:     flex;
       |      align-items: center;
       |      gap:         12px;
       |      margin-bottom: 14px;
       |    }
       |    .week-badge {
       |      display:       inline-block;
       |      background:    var(--badge-bg);
       |      color:         var(--badge-text);
       |      border:        1px solid var(--badge-border);
       |      border-radius: 4px;
       |      font-size:     var(--fs-small);
       |      font-weight:   var(--header-fw);
       |      padding:       3px 10px;
       |      letter-spacing:0.5px;
       |      text-transform:uppercase;
       |    }
       |    .week-label {
       |      font-size:   var(--fs-base);
       |      font-weight: var(--header-fw);
       |      color:       var(--page-text);
       |    }
       |
       |    /* ── Таблица ── */
       |    .schedule-table {
       |      width:           100%;
       |      border-collapse: collapse;
       |      border:          1px solid var(--border);
       |      border-radius:   var(--radius);
       |      overflow:        hidden;
       |      table-layout:    fixed;
       |    }
       |
       |    /* Заголовочная строка (дни недели) */
       |    .schedule-table thead th {
       |      background:  var(--header-bg);
       |      color:       var(--header-text);
       |      font-weight: var(--header-fw);
       |      padding:     var(--cell-pad);
       |      text-align:  center;
       |      border:      1px solid var(--border);
       |      font-size:   var(--fs-base);
       |    }
       |    .schedule-table thead th.num-col {
       |      width: 42px;
       |      font-size: var(--fs-small);
       |    }
       |
       |    /* Номер пары (первый столбец) */
       |    .schedule-table tbody th {
       |      background:  var(--header-bg);
       |      color:       var(--header-text);
       |      font-weight: var(--header-fw);
       |      font-size:   var(--fs-small);
       |      text-align:  center;
       |      padding:     var(--cell-pad);
       |      border:      1px solid var(--border);
       |      width:       42px;
       |      vertical-align: middle;
       |    }
       |
       |    /* Обычная ячейка */
       |    .schedule-table td {
       |      border:         1px solid var(--border);
       |      padding:        0;
       |      vertical-align: top;
       |    }
       |
       |    /* Пустая ячейка */
       |    .cell-empty {
       |      background: var(--empty-bg);
       |      height:     60px;
       |    }
       |
       |    /* Ячейка с занятием */
       |    .cell-lesson {
       |      padding:        var(--cell-pad);
       |      border-left:    3px solid var(--lesson-accent);
       |      background:     var(--lesson-bg);
       |      color:          var(--lesson-text);
       |      height:         100%;
       |      min-height:     60px;
       |    }
       |
       |    /* Лекция */
       |    .lesson-lk {
       |      --lesson-bg:     var(--lk-bg);
       |      --lesson-text:   var(--lk-text);
       |      --lesson-accent: var(--lk-accent);
       |    }
       |    /* Практика */
       |    .lesson-pr {
       |      --lesson-bg:     var(--pr-bg);
       |      --lesson-text:   var(--pr-text);
       |      --lesson-accent: var(--pr-accent);
       |    }
       |    /* Лабораторная */
       |    .lesson-lab {
       |      --lesson-bg:     var(--lab-bg);
       |      --lesson-text:   var(--lab-text);
       |      --lesson-accent: var(--lab-accent);
       |    }
       |
       |    /* Содержимое ячейки занятия */
       |    .lesson-subject {
       |      font-weight: var(--header-fw);
       |      font-size:   var(--fs-base);
       |      line-height: 1.3;
       |      margin-bottom: 4px;
       |    }
       |    .lesson-meta {
       |      font-size: var(--fs-small);
       |      opacity:   0.75;
       |      display:   flex;
       |      flex-wrap: wrap;
       |      gap:       4px 10px;
       |      margin-bottom: 4px;
       |    }
       |    .lesson-type-badge {
       |      display:       inline-block;
       |      font-size:     10px;
       |      font-weight:   var(--header-fw);
       |      text-transform:uppercase;
       |      letter-spacing:0.6px;
       |      border:        1px solid currentColor;
       |      border-radius: 3px;
       |      padding:       1px 5px;
       |      opacity:       0.7;
       |    }
       |    .lesson-subgroups {
       |      font-size: var(--fs-small);
       |      opacity:   0.65;
       |    }""".stripMargin

  // ── Рендер недели ──────────────────────────────────────────────────────────

  private def renderWeek(week: Week, theme: Theme): String =
    val (label, badge) = week.weekType match
      case WeekType.Odd  => ("Нечётная неделя", "нечётная")
      case WeekType.Even => ("Чётная неделя",   "чётная")

    s"""    <section class="week-section" aria-label="$label">
       |      <div class="week-heading">
       |        <span class="week-badge">$badge</span>
       |        <span class="week-label">$label</span>
       |      </div>
       |${renderTable(week)}
       |    </section>""".stripMargin

  // ── Рендер таблицы ────────────────────────────────────────────────────────

  private def renderTable(week: Week): String =
    val headerRow = renderHeaderRow(week.days.map(_.day))
    val bodyRows  = (0 until 6).map(i => renderRow(i, week.days)).mkString("\n")

    s"""      <table class="schedule-table">
       |        <thead>
       |$headerRow
       |        </thead>
       |        <tbody>
       |$bodyRows
       |        </tbody>
       |      </table>""".stripMargin

  // ── Рендер строки заголовка (дни) ─────────────────────────────────────────

  private def renderHeaderRow(days: Vector[DayOfWeek]): String =
    val dayCells = days.map(d => s"""          <th scope="col">${dayName(d)}</th>""")
      .mkString("\n")

    s"""          <tr>
       |            <th class="num-col" scope="col">№</th>
       |$dayCells
       |          </tr>""".stripMargin

  // ── Рендер строки пар (одного номера пары по всем дням) ───────────────────

  private def renderRow(slotIndex: Int, days: Vector[DayBlock]): String =
    val pairNum   = slotIndex + 1
    val dataCells = days.map(day => renderCell(day.slots(slotIndex))).mkString("\n")

    s"""          <tr>
       |            <th scope="row">$pairNum</th>
       |$dataCells
       |          </tr>""".stripMargin

  // ── Рендер одной ячейки ───────────────────────────────────────────────────

  private def renderCell(slot: Option[Slot]): String =
    slot match
      case None       => """            <td><div class="cell-empty" aria-label="Нет занятия"></div></td>"""
      case Some(lesson) => s"""            <td>${renderSlot(lesson)}</td>"""

  // ── Рендер содержимого ячейки с занятием ──────────────────────────────────

  private def renderSlot(slot: Slot): String =
    val typeClass = lessonTypeClass(slot.lessonType)
    val typeName  = lessonTypeName(slot.lessonType)

    val subgroupsHtml =
      if slot.subgroups.isEmpty then ""
      else
        val subs = slot.subgroups.map(escape).mkString(", ")
        s"""\n          <div class="lesson-subgroups">Подгр.: $subs</div>"""

    s"""    <div class="cell-lesson $typeClass">
       |      <div class="lesson-subject">${escape(slot.subject)}</div>
       |      <div class="lesson-meta">
       |        <span>${escape(slot.room)}</span>
       |        <span>${escape(slot.teacher)}</span>
       |      </div>
       |      <span class="lesson-type-badge">$typeName</span>$subgroupsHtml
       |    </div>""".stripMargin

  // ── Вспомогательные функции-словари ───────────────────────────────────────

  private def dayName(day: DayOfWeek): String = day match
    case DayOfWeek.Mon => "Понедельник"
    case DayOfWeek.Tue => "Вторник"
    case DayOfWeek.Wed => "Среда"
    case DayOfWeek.Thu => "Четверг"
    case DayOfWeek.Fri => "Пятница"
    case DayOfWeek.Sat => "Суббота"

  private def lessonTypeClass(t: LessonType): String = t match
    case LessonType.Lecture  => "lesson-lk"
    case LessonType.Practice => "lesson-pr"
    case LessonType.Lab      => "lesson-lab"

  private def lessonTypeName(t: LessonType): String = t match
    case LessonType.Lecture  => "лк"
    case LessonType.Practice => "пр"
    case LessonType.Lab      => "лаб"

  // Экранирование HTML-спецсимволов — защита от XSS в данных расписания
  private def escape(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")