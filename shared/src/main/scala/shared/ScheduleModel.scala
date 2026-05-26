package shared

// Це было написано Клодом, это еще одна точка
// соприкосновения, от которой мы оба должны отталкиваться.

// Тип пары
enum LessonType:
  case Lecture  // лк
  case Practice // пр
  case Lab      // лаб

// Один временной слот (пара). None означает, что пара отсутствует.
final case class Slot(
                       subject:    String,
                       room:       String,
                       teacher:    String,
                       lessonType: LessonType,
                       subgroups:  List[String]
                     )

// День недели
enum DayOfWeek:
  case Mon, Tue, Wed, Thu, Fri, Sat

// Блок одного дня: ровно 6 слотов (индекс = номер пары, None = пусто)
final case class DayBlock(
                           day:   DayOfWeek,
                           slots: Vector[Option[Slot]]  // длина всегда 6
                         ):
  require(slots.length == 6, "DayBlock must contain exactly 6 slots")

// Тип недели
enum WeekType:
  case Odd  // нечётная
  case Even // чётная

// Одна неделя: ровно 6 дней (пн–сб)
final case class Week(
                       weekType: WeekType,
                       days:     Vector[DayBlock]   // длина всегда 6
                     ):
  require(days.length == 6, "Week must contain exactly 6 days")

// Метаданные расписания
final case class Meta(
                       version:    String,
                       groupName:  String,
                       createdAt:  String           // ISO-8601, хранится как строка
                     )

// Корневой объект — то, что передаётся от Редактора к Генератору
final case class ScheduleFile(
                               meta:  Meta,
                               weeks: Vector[Week]          // ровно 2: [odd, even]
                             ):
  require(weeks.length == 2, "ScheduleFile must contain exactly 2 weeks")