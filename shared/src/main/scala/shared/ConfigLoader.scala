package shared

import cats.effect.IO
import io.circe.yaml.parser
import scala.io.Source
import scala.util.Using

/** Загрузчик конфигурации расписания из YAML-файла в ресурсах.
 * Файл должен лежать по пути `shared/src/main/resources/config.yaml`
 * и соответствовать схеме [[ScheduleConfig]].
 */
object ConfigLoader:
  
  /** Загружает и парсит `config.yaml` из classpath
   * @return `IO` с `Right(config)`, если успех,
   *         или `Left(errorMessage)` в случае ошибки
   *         (файл не найден, YAML невалиден, поля не совпадают)
   */
  def loadFromResources(): IO[Either[String, ScheduleConfig]] =
    IO.blocking {
      Option(getClass.getResourceAsStream("/config.yaml"))
        .toRight("Config file 'config.yaml' not found in resources")
        .flatMap { stream =>
          Using(Source.fromInputStream(stream)) { source =>
            source.mkString
          }.toEither.left.map(_.getMessage)
        }
        .flatMap(parseYaml)
    }
  
  private def parseYaml(yamlStr: String): Either[String, ScheduleConfig] =
    parser.parse(yamlStr) match
      case Right(json) =>
        json.hcursor
          .downField("schedule")
          .as[ScheduleConfig](ScheduleConfig.decoder)
          // на этом моменте имеем Either[DecodingFailure, ScheduleConfig]
          // так как обязались слева вернуть строку, преобразуем Left:
          .left.map(err => s"Invalid config format: ${err.getMessage}")
      case Left(err) =>
        Left(s"Invalid YAML: ${err.getMessage}")