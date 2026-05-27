package editor.model

enum Subgroup:
  case All, First, Second, Third, Fourth

object Subgroup:
  def fromString(s: String): Option[Subgroup] = s match
    case "" | "All" => Some(All)
    case "1" => Some(First)
    case "2" => Some(Second)
    case "3" => Some(Third)
    case "4" => Some(Fourth)
    case _ => None
  
  def toString(sg: Subgroup): String = sg match
    case All    => ""
    case First  => "1"
    case Second => "2"
    case Third  => "3"
    case Fourth => "4"
  
  def toDisplay(sg: Subgroup): String = sg match
    case All    => "вся группа"
    case First  => "1 п/гр"
    case Second => "2 п/гр"
    case Third  => "3 п/гр"
    case Fourth => "4 п/гр"