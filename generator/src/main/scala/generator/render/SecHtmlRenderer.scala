package generator.render


import generator.theme.Theme
import shared.*

/**
 * Альтернативный HTML-рендерер (карточное представление).
 *
 * Входные данные те же (ScheduleFile + Theme).
 * Выход — самодостаточный HTML-документ, где расписание отображается
 * не в виде таблицы, а в виде вертикальных карточек по дням недели.
 */
object SecHtmlRenderer extends Renderer[String]:

  // ── Публичный метод ──────────────────────────────────────────────────────
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

  // ── Сборка HTML-документа ────────────────────────────────────────────────
  private def buildDocument(
                             meta:  Meta,
                             odd:   Week,
                             even:  Week,
                             theme: Theme
                           ): String =
    s"""<!DOCTYPE html>
       |<html lang="ru">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <title>Расписание (карточки) — ${escape(meta.groupName)}</title>
       |  <style>
       |${buildCss(theme)}
       |  </style>
       |</head>
       |<body>
       |  <header class="page-header">
       |    <h1>Расписание группы <span>${escape(meta.groupName)}</span></h1>
       |    <p class="meta-info">Сформировано: ${escape(meta.createdAt)}</p>
       |  </header>
       |  <main class="schedule-main">
       |${renderWeek(odd, theme)}
       |${renderWeek(even, theme)}
       |  </main>
       |</body>
       |</html>""".stripMargin

  // ── CSS ──────────────────────────────────────────────────────────────────
  private def buildCss(t: Theme): String =
    s"""    :root {
       |      --page-bg:          ${t.pageBackground};
       |      --page-text:        ${t.pageText};
       |      --header-bg:        ${t.headerBackground};
       |      --header-text:      ${t.headerText};
       |      --border:           ${t.borderColor};
       |      --radius:           ${t.borderRadius};
       |      --font:             ${t.fontFamily};
       |      --fs-base:          ${t.fontSizeBase};
       |      --fs-small:         ${t.fontSizeSmall};
       |      --badge-bg:         ${t.weekBadgeBackground};
       |      --badge-text:       ${t.weekBadgeText};
       |      --badge-border:     ${t.weekBadgeBorder};
       |      --card-bg:          ${t.emptyBackground};   /* фон карточек */
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
       |    body {
       |      background: var(--page-bg);
       |      color: var(--page-text);
       |      font-family: var(--font);
       |      font-size: var(--fs-base);
       |      line-height: 1.4;
       |    }
       |    .page-header {
       |      background: var(--header-bg);
       |      color: var(--header-text);
       |      padding: 20px 24px;
       |      margin-bottom: 24px;
       |    }
       |    .page-header h1 { font-size: 1.6em; }
       |    .page-header h1 span { opacity: 0.85; }
       |    .meta-info { font-size: var(--fs-small); opacity: 0.7; margin-top: 4px; }
       |    .schedule-main { max-width: 900px; margin: 0 auto; padding: 0 16px 40px; }
       |
       |    .week-section { margin-bottom: 40px; }
       |    .week-heading { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
       |    .week-badge {
       |      background: var(--badge-bg); color: var(--badge-text);
       |      border: 1px solid var(--badge-border); border-radius: 4px;
       |      padding: 2px 10px; font-size: var(--fs-small); font-weight: bold;
       |      text-transform: uppercase;
       |    }
       |    .week-label { font-weight: bold; font-size: 1.2em; }
       |
       |    .day-block { margin-bottom: 20px; }
       |    .day-title {
       |      font-weight: bold; font-size: 1.1em; margin-bottom: 8px;
       |      color: var(--header-text); background: var(--header-bg);
       |      padding: 6px 12px; border-radius: var(--radius);
       |    }
       |    .cards-container { display: flex; flex-direction: column; gap: 8px; }
       |    .card {
       |      background: var(--card-bg);
       |      border: 1px solid var(--border);
       |      border-radius: var(--radius);
       |      padding: 12px;
       |      border-left: 4px solid var(--lesson-accent);
       |      display: grid;
       |      grid-template-columns: 100px 1fr auto;
       |      align-items: center;
       |      gap: 12px;
       |    }
       |    .card.lesson-lk { --lesson-accent: var(--lk-accent); background: var(--lk-bg); color: var(--lk-text); }
       |    .card.lesson-pr { --lesson-accent: var(--pr-accent); background: var(--pr-bg); color: var(--pr-text); }
       |    .card.lesson-lab { --lesson-accent: var(--lab-accent); background: var(--lab-bg); color: var(--lab-text); }
       |    .card-time { font-weight: bold; font-size: var(--fs-small); }
       |    .card-subject { font-weight: bold; }
       |    .card-details { font-size: var(--fs-small); opacity: 0.8; }
       |    .card-badge {
       |      font-size: 10px; text-transform: uppercase;
       |      border: 1px solid currentColor; border-radius: 3px;
       |      padding: 1px 5px; justify-self: end;
       |    }
       |    .card-subgroups { font-size: var(--fs-small); opacity: 0.7; }
       |""".stripMargin

  // ── Рендер недели ────────────────────────────────────────────────────────
  private def renderWeek(week: Week, theme: Theme): String =
    val (label, badge) = week.weekType match
      case WeekType.Odd  => ("Нечётная неделя", "нечётная")
      case WeekType.Even => ("Чётная неделя",   "чётная")

    val daysHtml = week.days.map(renderDay).mkString("\n")

    s"""    <section class="week-section">
       |      <div class="week-heading">
       |        <span class="week-badge">$badge</span>
       |        <span class="week-label">$label</span>
       |      </div>
       |$daysHtml
       |    </section>""".stripMargin

  // ── Рендер одного дня ───────────────────────────────────────────────────
  private def renderDay(dayBlock: DayBlock): String =
    val cards = dayBlock.slots.zipWithIndex.collect {
      case (Some(slot), idx) => renderCard(slot, idx)
    }
    val cardsHtml =
      if cards.nonEmpty then cards.mkString("\n")
      else "<p>Нет занятий</p>"

    s"""      <div class="day-block">
       |        <div class="day-title">${dayName(dayBlock.day)}</div>
       |        <div class="cards-container">
       |$cardsHtml
       |        </div>
       |      </div>""".stripMargin

  // ── Рендер карточки занятия ─────────────────────────────────────────────
  private def renderCard(slot: Slot, index: Int): String =
    val typeClass = lessonTypeClass(slot.lessonType)
    val typeName  = lessonTypeName(slot.lessonType)
    val timeStr   = timeForPair(index + 1)

    val subgroupsHtml =
      if slot.subgroups.isEmpty then ""
      else s"""<div class="card-subgroups">Подгр.: ${slot.subgroups.map(escape).mkString(", ")}</div>"""

    s"""        <div class="card $typeClass">
       |          <div class="card-time">$timeStr</div>
       |          <div class="card-subject">${escape(slot.subject)}</div>
       |          <div class="card-badge">$typeName</div>
       |          <div class="card-details">${escape(slot.room)} &middot; ${escape(slot.teacher)}</div>
       |          $subgroupsHtml
       |        </div>""".stripMargin

  // ── Вспомогательные чистые функции ───────────────────────────────────────
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

  private def timeForPair(pairNum: Int): String = pairNum match
    case 1 => "9:00 – 10:30"
    case 2 => "10:40 – 12:10"
    case 3 => "12:40 – 14:10"
    case 4 => "14:20 – 15:50"
    case 5 => "16:00 – 17:30"
    case 6 => "17:40 – 19:10"
    case _ => s"Пара $pairNum"

  private def escape(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")