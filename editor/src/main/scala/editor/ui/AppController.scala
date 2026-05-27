package editor.ui

import javafx.scene.Scene
import javafx.stage.Stage
import shared.*
import shared.ScheduleConfig

import java.util.concurrent.atomic.AtomicReference
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

final case class AppState(
                           schedule: ScheduleFile,
                           format: String,
                           style: (String, String, String)
                         )

class AppController(primaryStage: Stage, cfg: ScheduleConfig):

  private val stateRef = AtomicReference(
    AppState(
      ScheduleFactory.empty(
        Meta("1.0", "Group", LocalDateTime.now.toString),
        cfg
      ),
      "html",
      ("Light", "Blue", "Normal")
    )
  )

  private def state: AppState = stateRef.get()
  private def updateState(f: AppState => AppState): Unit =
    stateRef.updateAndGet((t: AppState) => f(t))
  
  private val mainWindow = new MainWindow()
  private val editorScreen = new EditorScreen()
  private val formatScreen = new FormatScreen()
  private val styleScreen = new StyleScreen()
  private val doneScreen = new DoneScreen()
  
  setupEventHandlers()
  
  def setupEventHandlers(): Unit =
    mainWindow.getNewScheduleButton.setOnAction(_ => {
      showEditorScreen()
    })
    
    mainWindow.getOpenScheduleButton.setOnAction(_ =>
      showOpenScheduleDialog()
    )
    
    editorScreen.getSaveButton.setOnAction(_ =>
      updateState(_.copy(schedule = editorScreen.getScheduleFile))
      showFormatScreen()
    )
    
    formatScreen.getNextButton.setOnAction(_ =>
      updateState(_.copy(format = formatScreen.getSelectedFormat))
      showStyleScreen()
    )
    
    formatScreen.getBackButton.setOnAction(_ =>
      showEditorScreen()
    )
    
    styleScreen.getNextButton.setOnAction(_ =>
      updateState(_.copy(
        style = (
          styleScreen.getTheme,
          styleScreen.getColorScheme,
          styleScreen.getFontSize
        )
      ))
      showDoneScreen()
    )
    
    styleScreen.getBackButton.setOnAction(_ =>
      showFormatScreen()
    )
    
    doneScreen.getSaveButton.setOnAction(_ =>
      saveScheduleToFile()
    )
    
    doneScreen.getExportButton.setOnAction(_ =>
      exportSchedule()
    )
    
    doneScreen.getEditButton.setOnAction(_ =>
      showEditorScreen()
    )
  
  def showMainWindow(): Unit =
    primaryStage.setScene(new Scene(mainWindow.getRoot, 800, 600))
    primaryStage.show()
  
  def showEditorScreen(): Unit =
    editorScreen.setScheduleFile(state.schedule)
    primaryStage.setScene(new Scene(editorScreen.getRoot, 1000, 700))
    
  
  def showFormatScreen(): Unit =
    primaryStage.setScene(new Scene(formatScreen.getRoot, 600, 500))
  
  def showStyleScreen(): Unit =
    primaryStage.setScene(new Scene(styleScreen.getRoot, 600, 500))
  
  def showDoneScreen(): Unit =
    val previewText = generatePreview(state.schedule)
    doneScreen.setPreviewText(previewText)
    primaryStage.setScene(new Scene(doneScreen.getRoot, 800, 700))
  
  def showOpenScheduleDialog(): Unit =
    println("Open dialog - not implemented yet")
  
  private def saveScheduleToFile(): Unit =
    val timestamp = LocalDateTime.now()
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    
    val filename = s"schedule_${state.schedule.meta.groupName}_$timestamp.json"
    
    JsonEncoder.saveScheduleToFile(state.schedule, filename) match
      case Right(_) =>
        println(s"Schedule saved to $filename")
        showMainWindow()
      case Left(error) =>
        println(s"Error saving schedule: $error")
  
  private def exportSchedule(): Unit =
    println(s"Exporting schedule as ${state.format} with style: ${state.style}")
  
  private def generatePreview(scheduleFile: ScheduleFile): String =
    scheduleFile.weeks.indices.map { idx =>
      val week = scheduleFile.weeks(idx)
      val weekType = if idx == 0 then "ODD" else "EVEN"
      
      val daysInfo = week.days.map { dayBlock =>
        
        val dayName = dayBlock.day match
          case DayOfWeek.Mon => "Monday"
          case DayOfWeek.Tue => "Tuesday"
          case DayOfWeek.Wed => "Wednesday"
          case DayOfWeek.Thu => "Thursday"
          case DayOfWeek.Fri => "Friday"
          case DayOfWeek.Sat => "Saturday"
        
        val slotsInfo = dayBlock.slots.indices.map { i =>
          dayBlock.slots(i) match
            case Some(slot) => s"Pair ${i + 1}: ${slot.subject} (${slot.room}) - ${slot.teacher}"
            case None => s"Pair ${i + 1}: [Empty]"
        }.mkString("\n")
        
        s"$dayName:\n$slotsInfo"
      }.mkString("\n")
      
      s"$weekType Week:\n$daysInfo"
    }.mkString("\n\n")
  
  def start(): Unit =
    primaryStage.setTitle("Schedule Editor")
    showMainWindow()