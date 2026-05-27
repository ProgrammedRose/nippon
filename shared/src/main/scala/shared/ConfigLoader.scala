// shared/ConfigLoader.scala
package shared

import cats.effect.IO
import io.circe.yaml.parser
import java.io.{File, FileInputStream}
import scala.io.Source
import scala.util.Using

object ConfigLoader:
  private val configPath = "config.yaml"
  
  def loadFromFile(file: File): IO[Either[String, ScheduleConfig]] =
    IO.blocking {
      Using(Source.fromFile(file)) { source =>
        source.mkString
      }.toEither.left.map(_.getMessage)
    }.flatMap {
      case Right(yamlStr) => IO.pure(parseYaml(yamlStr))
      case Left(err) => IO.pure(Left(s"Cannot read config file: $err"))
    }
  
  def loadFromResources(): IO[Either[String, ScheduleConfig]] =
    IO.blocking {
      Option(getClass.getClassLoader.getResourceAsStream(configPath))
        .toRight(s"Config file '$configPath' not found in resources")
        .flatMap { stream =>
          Using(Source.fromInputStream(stream)) { source =>
            source.mkString
          }.toEither.left.map(_.getMessage)
        }
    }.flatMap {
      case Right(yamlStr) => IO.pure(parseYaml(yamlStr))
      case Left(err) => IO.pure(Left(err))
    }
  
  private def parseYaml(yamlStr: String): Either[String, ScheduleConfig] =
    parser.parse(yamlStr) match
      case Right(json) =>
        json.hcursor.downField("schedule").as[ScheduleConfig](ScheduleConfig.decoder)
          .left.map(err => s"Invalid config format: ${err.getMessage}")
      case Left(err) => Left(s"Invalid YAML: ${err.getMessage}")