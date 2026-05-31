package editor.model

import shared.*

/** Состояние редактора расписания.
 *
 * @param schedule          расписание целиком (две недели + метаданные)
 * @param currentWeekType   какая неделя показывается в UI: [[WeekType.Odd]] или [[WeekType.Even]]
 * @param selectedDayIndex  индекс выбранного дня в текущей неделе (0–5, -1 = ничего не выбрано)
 * @param selectedSlotIndex индекс выбранной пары в выбранном дне (0–5, -1 = ничего не выбрано)
 * @param isEditorMode      режим редактирования, можно/нельзя
 * @param groupNameInput    текущее значение поля ввода имени группы
 * @param exportFormat      формат экспорта для генератора ("html", "svg", "sec", "thrd")
 * @param theme             имя цветовой темы для генератора (передаётся в --theme)
 * @param colorScheme       цветовая схема UI редактора (не влияет на генерацию)
 * @param fontSize          размер шрифта в UI редактора ("Small", "Normal", "Large")
 * @param slotEditMode      флаг, открыта ли форма редактирования слота (отдельный режим внутри isEditorMode)
 * @param editingSlotIndex  индекс слота, который сейчас редактируется (используется вместе с slotEditMode)
 */
case class AppState(
                     schedule: ScheduleFile,
                     currentWeekType: WeekType,
                     selectedDayIndex: Int,
                     selectedSlotIndex: Int,
                     isEditorMode: Boolean,
                     groupNameInput: String,
                     exportFormat: String = "html",
                     theme: String = "Light",
                     colorScheme: String = "Blue",
                     fontSize: String = "Normal",
                     slotEditMode: Boolean = false,
                     editingSlotIndex: Int = -1
                   )

object AppState:
  /** 
   * @param groupName имя группы (будет подставлено в метаданные)
   * @param cfg       конфигурация расписания (количество дней, слотов и т.д.)
   */
  def empty(groupName: String, cfg: ScheduleConfig): AppState =
    val schedule = ScheduleFactory.empty(
      Meta("1.0", groupName, java.time.LocalDateTime.now.toString),
      cfg
    )
    AppState(
      schedule = schedule,
      currentWeekType = WeekType.Odd,
      selectedDayIndex = -1,
      selectedSlotIndex = -1,
      isEditorMode = false,
      groupNameInput = groupName
    )