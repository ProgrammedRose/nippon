package editor.serialization

import shared.*
import scala.util.Using
import scala.io.Source
import scala.util.Try

/**
 * Десериализация JSON в модель расписания (shared типы)
 */
object JsonDecoder:

  sealed trait DecodeError
  case class ParseError(message: String) extends DecodeError
  case class ValidationError(message: String) extends DecodeError

  /**
   * Загружает ScheduleFile из JSON-файла
   */
  def loadScheduleFromFile(filePath: String): Either[DecodeError, ScheduleFile] =
    try
      Using(Source.fromFile(filePath)) { source =>
        val jsonString = source.mkString
        parseScheduleFileJson(jsonString)
      }.get
    catch
      case e: Exception => Left(ParseError(s"Failed to load file: ${e.getMessage}"))

  /**
   * Парсит JSON-строку в ScheduleFile
   * Это базовая реализация - для production стоит использовать proper JSON library
   * типа circe, play-json, или ujson
   */
  private def parseScheduleFileJson(json: String): Either[DecodeError, ScheduleFile] =
    try {
      val version = extractField(json, "version").getOrElse("1.0")
      val groupName = extractField(json, "groupName").getOrElse("Unknown")
      val createdAt = extractField(json, "createdAt").getOrElse("")
      
      val meta = shared.ScheduleFile.Meta(version, groupName, createdAt)
      
      // Для полного парсинга weeks нужна нормальная JSON-библиотека
      // На данный момент возвращаем пустой ScheduleFile как placeholder
      val weeks = Vector()
      
      Right(ScheduleFile(meta, weeks))
    } catch {
      case e: Exception => Left(ParseError(s"JSON parsing failed: ${e.getMessage}"))
    }

  /**
   * Простой хелпер для извлечения строкового значения из JSON
   * Внимание: это очень примитивный парсер, используется только для demo
   * 
   * TODO: Заменить на circe/play-json/ujson для production
   */
  private def extractField(json: String, fieldName: String): Option[String] =
    val pattern = s""""$fieldName"\\s*:\\s*"([^"]*)"""".r
    pattern.findFirstMatchIn(json).map(_.group(1))
