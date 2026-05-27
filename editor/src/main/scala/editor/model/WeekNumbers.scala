package editor.model

object WeekNumbers:
  val minWeek: Int = 1
  val maxWeek: Int = 18
  val allWeeks: List[Int] = (minWeek to maxWeek).toList
  val oddWeeks: List[Int] = allWeeks.filter(_ % 2 == 1)
  val evenWeeks: List[Int] = allWeeks.filter(_ % 2 == 0)
  
  def isValid(week: Int): Boolean = minWeek <= week && week <= maxWeek
  
  def parseFromString(s: String): Either[String, List[Int]] =
    if s.trim.isEmpty then Right(allWeeks)
    else
      s.split(",").toList
        .map(_.trim)
        .flatMap { part =>
          part.split("-").toList match
            case List(single) =>
              single.toIntOption.filter(isValid).toList
            case List(start, end) =>
              (start.toIntOption, end.toIntOption) match
                case (Some(a), Some(b)) if a <= b && isValid(a) && isValid(b) =>
                  (a to b).filter(isValid).toList
                case _ => Nil
            case _ => Nil
        }
        .distinct
        .sorted match
        case Nil => Left("Неверный формат недель")
        case weeks => Right(weeks)