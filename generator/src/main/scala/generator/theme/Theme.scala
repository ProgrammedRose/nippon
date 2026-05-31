package generator.theme

/**
 * Палитра цветов для одного типа пары.
 * background — фон ячейки, text — цвет текста, accent — левая полоска-акцент.
 */
final case class LessonColors(
                               background: String,
                               text:       String,
                               accent:     String
                             )

/**
 * Цвета для каждого типа занятия.
 * Используются в HtmlRenderer при рендере ячейки пары.
 */
final case class LessonPalette(
                                lecture:  LessonColors,
                                practice: LessonColors,
                                lab:      LessonColors
                              )

/**
 * Полная тема оформления расписания.
 *
 * Все цветовые значения — валидные CSS-строки (hex, rgb, hsl, named).
 * Все размеры — валидные CSS-строки («14px», «1rem», «600» для font-weight).
 *
 * Поля сгруппированы по зонам применения, чтобы HtmlRenderer мог
 * подставлять их напрямую в CSS-переменные без дополнительной логики.
 */
final case class Theme(

                        // ── Мета ──────────────────────────────────────────────────────────────────
                        name:        String,   // человекочитаемое имя («Default», «Dark»)
                        description: String,   // короткое описание для UI

                        // ── Страница ──────────────────────────────────────────────────────────────
                        pageBackground: String,  // фон всей страницы
                        pageText:       String,  // основной цвет текста

                        // ── Шапка таблицы (строка с днями недели, столбец с номерами пар) ─────────
                        headerBackground: String,
                        headerText:       String,
                        headerFontWeight: String,  // «600», «700» и т.п.

                        // ── Сетка таблицы ─────────────────────────────────────────────────────────
                        borderColor:       String,
                        borderRadius:      String,  // скругление углов таблицы, напр. «8px»
                        cellPadding:       String,  // внутренние отступы ячейки, напр. «8px 10px»
                        emptyBackground:   String,  // фон пустой ячейки (нет пары)

                        // ── Типографика ───────────────────────────────────────────────────────────
                        fontFamily:       String,   // CSS font-family стек
                        fontSizeBase:     String,   // базовый размер, напр. «14px»
                        fontSizeSmall:    String,   // мелкий текст (кабинет, преподаватель), напр. «12px»
                        fontSizeMeta:     String,   // заголовок расписания, напр. «20px»

                        // ── Метка типа недели («Нечётная» / «Чётная») ────────────────────────────
                        weekBadgeBackground: String,
                        weekBadgeText:       String,
                        weekBadgeBorder:     String,

                        // ── Цвета по типам занятий ────────────────────────────────────────────────
                        lessons: LessonPalette
                      )