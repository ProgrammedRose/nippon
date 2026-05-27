// shared/ConfigLoader.scala
package shared

import cats.effect.IO
import io.circe.yaml.parser
import java.io.{File, FileInputStream}
import scala.io.Source
import scala.util.Using

object ConfigLoader:
  
  // Пробуем загрузить из resources, если не работает — из корня проекта
  def loadFromResources(): IO[Either[String, ScheduleConfig]] =
    IO.blocking {
      val stream = Option(getClass.getClassLoader.getResourceAsStream("config.yaml"))
        .orElse(Option(ClassLoader.getSystemResourceAsStream("config.yaml")))
        .toRight("Config file 'config.yaml' not found in resources")
      
      stream.flatMap { s =>
        Using(Source.fromInputStream(s)) { source =>
          source.mkString
        }.toEither.left.map(_.getMessage)
      }
    }.flatMap {
      case Right(yamlStr) => IO.pure(parseYaml(yamlStr))
      case Left(err) =>
        // Fallback: пытаемся загрузить из файловой системы
        IO.blocking {
          val file = new File("shared/src/main/resources/config.yaml")
          if file.exists() then
            Using(Source.fromFile(file)) { source =>
              source.mkString
            }.toEither.left.map(_.getMessage)
          else Left(s"Config file not found: $err")
        }.flatMap {
          case Right(yamlStr) => IO.pure(parseYaml(yamlStr))
          case Left(err2) => IO.pure(Left(err2))
        }
    }
  
  private def parseYaml(yamlStr: String): Either[String, ScheduleConfig] =
    parser.parse(yamlStr) match
      case Right(json) =>
        json.hcursor.downField("schedule").as[ScheduleConfig](ScheduleConfig.decoder)
          .left.map(err => s"Invalid config format: ${err.getMessage}")
      case Left(err) => Left(s"Invalid YAML: ${err.getMessage}")