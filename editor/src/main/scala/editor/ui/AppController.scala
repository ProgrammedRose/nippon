package editor.ui

import javafx.stage.Stage
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import shared.*
import editor.serialization.JsonEncoder
import editor.validation.Validator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Контроллер для навигации между экранами и управления состоянием
 */
class AppController(primaryStage: Stage):
  private var currentScheduleFile: ScheduleFile = ScheduleFile(
    ScheduleFile.Meta("1.0", "Group", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)),
    Vector()
  )
  private var currentFormat: String = "html"
  private var currentStyle: (String, String, String) = ("Light", "Blue", "Normal")

  private val mainWindow = new MainWindow()
  private val editorScreen = new EditorScreen()
  private val formatScreen = new FormatScreen()
  private val styleScreen = new StyleScreen()
  private val doneScreen = new DoneScreen()

  // Регистрируем обработчики событий
  setupEventHandlers()

  def setupEventHandlers(): Unit =
    // MainWindow handlers
    mainWindow.getNewScheduleButton.setOnAction(_ => showEditorScreen())
    mainWindow.getOpenScheduleButton.setOnAction(_ => showOpenScheduleDialog())

    // EditorScreen handlers
    editorScreen.getSaveButton.setOnAction(_ => {
      currentScheduleFile = editorScreen.getScheduleFile
      showFormatScreen()
    })

    // FormatScreen handlers
    formatScreen.getNextButton.setOnAction(_ => {
      currentFormat = formatScreen.getSelectedFormat
      showStyleScreen()
    })
    formatScreen.getBackButton.setOnAction(_ => showEditorScreen())

    // StyleScreen handlers
    styleScreen.getNextButton.setOnAction(_ => {
      currentStyle = (
        styleScreen.getTheme,
        styleScreen.getColorScheme,
        styleScreen.getFontSize
      )
      showDoneScreen()
    })
    styleScreen.getBackButton.setOnAction(_ => showFormatScreen())

    // DoneScreen handlers
    doneScreen.getSaveButton.setOnAction(_ => saveScheduleToFile())
    doneScreen.getExportButton.setOnAction(_ => exportSchedule())
    doneScreen.getEditButton.setOnAction(_ => showEditorScreen())

  def showMainWindow(): Unit =
    val scene = new Scene(mainWindow.getRoot, 800, 600)
    primaryStage.setScene(scene)

  def showEditorScreen(): Unit =
    editorScreen.setScheduleFile(currentScheduleFile)
    val scene = new Scene(editorScreen.getRoot, 1000, 700)
    primaryStage.setScene(scene)

  def showFormatScreen(): Unit =
    val scene = new Scene(formatScreen.getRoot, 600, 500)
    primaryStage.setScene(scene)

  def showStyleScreen(): Unit =
    val scene = new Scene(styleScreen.getRoot, 600, 500)
    primaryStage.setScene(scene)

  def showDoneScreen(): Unit =
    val previewText = generatePreview(currentScheduleFile)
    doneScreen.setPreviewText(previewText)
    val scene = new Scene(doneScreen.getRoot, 800, 700)
    primaryStage.setScene(scene)

  def showOpenScheduleDialog(): Unit =
    // Здесь можно добавить FileChooser для открытия JSON файлов
    println("Open dialog - not implemented yet")

  private def saveScheduleToFile(): Unit =
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val filename = s"schedule_${currentScheduleFile.meta.groupName}_$timestamp.json"
    JsonEncoder.saveScheduleToFile(currentScheduleFile, filename) match
      case Right(_) =>
        println(s"Schedule saved to $filename")
        showMainWindow()
      case Left(error) =>
        println(s"Error saving schedule: $error")

  private def exportSchedule(): Unit =
    println(s"Exporting schedule as $currentFormat with style: $currentStyle")
    // Здесь будет интеграция с генератором для экспорта в HTML/SVG

  private def generatePreview(scheduleFile: ScheduleFile): String =
    val weeksInfo = scheduleFile.weeks.zipWithIndex.map { case (week, idx) =>
      val weekType = if idx == 0 then "ODD" else "EVEN"
      val daysInfo = week.days.map { dayBlock =>
        val dayName = dayBlock.day match
          case DayOfWeek.Mon => "Monday"
          case DayOfWeek.Tue => "Tuesday"
          case DayOfWeek.Wed => "Wednesday"
          case DayOfWeek.Thu => "Thursday"
          case DayOfWeek.Fri => "Friday"
          case DayOfWeek.Sat => "Saturday"
        val slotsInfo = dayBlock.slots.zipWithIndex.map { case (optSlot, idx) =>
          optSlot match
            case Some(slot) => s"    Pair ${idx + 1}: ${slot.subject} (${slot.room}) - ${slot.teacher}"
            case None => s"    Pair ${idx + 1}: [Empty]"
        }.mkString("\n")
        s"  $dayName:\n$slotsInfo"
      }.mkString("\n")
      s"$weekType Week:\n$daysInfo"
    }.mkString("\n\n")

    s"""
       |SCHEDULE FILE
       |Group: ${scheduleFile.meta.groupName}
       |Version: ${scheduleFile.meta.version}
       |Created: ${scheduleFile.meta.createdAt}
       |
       |$weeksInfo
       |
       |Format: $currentFormat
       |Theme: ${currentStyle._1}
       |Color: ${currentStyle._2}
       |Font Size: ${currentStyle._3}
       """.stripMargin

  def start(): Unit =
    primaryStage.setTitle("Schedule Editor")
    showMainWindow()
