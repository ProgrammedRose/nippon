package shared

import io.circe.yaml.parser
import shared.ScheduleConfig

import scala.io.Source
import scala.util.Using

object ConfigLoader:
  def load(): Either[String, ScheduleConfig] =
    Option(getClass.getResourceAsStream("/unused_structure.yaml"))
      .toRight("Config file '/unused_structure.yaml' not found")
      .flatMap { stream =>
        Using(Source.fromInputStream(stream)) { source => source.mkString }
          .toEither
          .left.map(_.getMessage)
      }
      .flatMap { yamlStr =>
        parser.parse(yamlStr).left.map(err => s"Invalid YAML: $err")
      }
      .flatMap { json =>
        json.hcursor.downField("schedule").as[ScheduleConfig]
          .left.map(err => s"Invalid config format: $err")
      }