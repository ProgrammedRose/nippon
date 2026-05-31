package shared

/** Тип пары (лекция, практика, лабораторная). */
enum LessonType:
  case Lecture
  case Practice
  case Lab

/** Один временной слот (пара). `None` означает, что пара отсутствует. */
final case class Slot(
                       subject:    String,
                       room:       String,
                       teacher:    String,
                       lessonType: LessonType,
                       subgroups:  List[String]
                     )

enum DayOfWeek:
  case Mon, Tue, Wed, Thu, Fri, Sat

/** Блок одного дня: ровно 6 слотов (индекс = номер пары, `None` = пусто). */
final case class DayBlock(
                           day:   DayOfWeek,
                           slots: Vector[Option[Slot]]
                         ):
  require(slots.length == 6, "DayBlock must contain exactly 6 slots")

object DayBlock:
  /** Создаёт пустой блок для дня (все слоты = `None`). */
  def empty: DayBlock =
    DayBlock(
      day = DayOfWeek.Mon, // временно не важно для UI-инициализации
      slots = Vector.fill(6)(None)
    )

/** Тип недели: нечётная (Odd) или чётная (Even). */
enum WeekType:
  case Odd, Even

/** Одна неделя: ровно 6 дней (пн–сб). */
final case class Week(
                       weekType: WeekType,
                       days:     Vector[DayBlock]
                     ):
  require(days.length == 6, "Week must contain exactly 6 days")

object Week:
  /** Создаёт пустую неделю заданного типа с пустыми `DayBlock` для каждого дня. */
  def empty(t: WeekType, cfg: ScheduleConfig): Week =
    Week(
      t,
      Vector(
        DayBlock(DayOfWeek.Mon, Vector.fill(cfg.slotsPerDay)(None)),
        DayBlock(DayOfWeek.Tue, Vector.fill(cfg.slotsPerDay)(None)),
        DayBlock(DayOfWeek.Wed, Vector.fill(cfg.slotsPerDay)(None)),
        DayBlock(DayOfWeek.Thu, Vector.fill(cfg.slotsPerDay)(None)),
        DayBlock(DayOfWeek.Fri, Vector.fill(cfg.slotsPerDay)(None)),
        DayBlock(DayOfWeek.Sat, Vector.fill(cfg.slotsPerDay)(None))
      )
    )

/** Метаданные расписания (версия, группа, дата создания). */
final case class Meta(
                       version:   String,
                       groupName: String,
                       createdAt: String  // ISO-8601, хранится как строка
                     )

/** Корневой объект – то, что передаётся от Редактора к Генератору. */
final case class ScheduleFile(
                               meta:  Meta,
                               weeks: Vector[Week]
                             ):
  require(weeks.length == 2, "ScheduleFile must contain exactly 2 weeks")