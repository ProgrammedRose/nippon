package shared.config

import io.circe.*
import io.circe.yaml.parser
import shared.ScheduleConfig

import scala.io.Source

object ConfigLoader:
  def load(): ScheduleConfig =
    val stream = getClass.getResourceAsStream("/structure.yaml")
    val yamlStr = Source.fromInputStream(stream).mkString
    
    parser.parse(yamlStr) match
      case Left(err) =>
        throw new RuntimeException(s"Invalid YAML: $err")
      
      case Right(json) =>
        json.as[ScheduleConfig] match
          case Left(err) =>
            throw new RuntimeException(s"Invalid config format: $err")
          case Right(cfg) =>
            cfg