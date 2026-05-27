package editor.ui

import javafx.scene.Scene
import javafx.stage.Stage
import shared.*
import shared.ScheduleConfig
import editor.serialization.JsonEncoder
import editor.serialization.JsonDecoder
import editor.model.{AppState, Actions}
import cats.effect.unsafe.implicits.global
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppController(primaryStage: Stage, cfg: ScheduleConfig):

  private val initialState = AppState.empty("Group", cfg)
  
  private val mainWindow = new MainWindow()
  private val editorScreen = new EditorScreen()
  private val formatScreen = new FormatScreen()
  private val styleScreen = new StyleScreen()
  private val doneScreen = new DoneScreen()
  
  def start(): Unit =
    primaryStage.setTitle("Schedule Editor")
    renderMain(initialState)
  
  // === Main render dispatcher ===
  private def renderMain(state: AppState): Unit =
    if state.isEditorMode then renderEditor(state)
    else renderStart(state)
  
  // === Start Screen ===
  private def renderStart(state: AppState): Unit =
    mainWindow.getNewScheduleButton.setOnAction(_ =>
      val newState = Actions.createNewSchedule(cfg)(state)
      renderMain(newState)
    )
    
    mainWindow.getOpenScheduleButton.setOnAction(_ =>
      val fileChooser = new javafx.stage.FileChooser()
      val file = fileChooser.showOpenDialog(primaryStage)
      if file != null then
        JsonDecoder.loadScheduleFromFile(file).unsafeRunSync() match
          case Right(schedule) =>
            val newState = Actions.loadSchedule(schedule)(state)
            renderMain(newState)
          case Left(err) => println(s"Error loading file: $err")
    )
    
    primaryStage.setScene(new Scene(mainWindow.getRoot, 800, 600))
    primaryStage.show()
  
  // === Editor Screen ===
  private def renderEditor(state: AppState): Unit =
    editorScreen.setScheduleFile(state.schedule, state.currentWeekType)
    
    editorScreen.getWeekCombo.setOnAction(_ =>
      val selectedWeek = if editorScreen.getWeekCombo.getValue == "Odd Week" 
        then WeekType.Odd else WeekType.Even
      val newState = Actions.setWeekType(selectedWeek)(state)
      renderEditor(newState)
    )
    
    editorScreen.getBackButton.setOnAction(_ =>
      val newState = Actions.backToStart(state)
      renderMain(newState)
    )
    
    editorScreen.getSaveButton.setOnAction(_ =>
      val newSchedule = editorScreen.getScheduleFile
      val newState = state.copy(schedule = newSchedule)
      renderFormat(newState)
    )
    
    // Slot CRUD handlers
    editorScreen.getAddSlotButton.setOnAction(_ =>
      showSlotEditor(state, None, -1)
    )
    
    editorScreen.getEditSlotButton.setOnAction(_ =>
      if state.selectedDayIndex >= 0 && state.selectedSlotIndex >= 0 then
        val day = state.schedule.weeks
          .find(_.weekType == state.currentWeekType)
          .get
          .days(state.selectedDayIndex)
        val slot = day.slots(state.selectedSlotIndex)
        showSlotEditor(state, slot, state.selectedSlotIndex)
      else
        println("Please select a slot first")
    )
    
    editorScreen.getDeleteSlotButton.setOnAction(_ =>
      if state.selectedDayIndex >= 0 && state.selectedSlotIndex >= 0 then
        val newState = Actions.deleteSlot(state.selectedDayIndex, state.selectedSlotIndex)(state)
        renderEditor(newState)
      else
        println("Please select a slot first")
    )
    
    primaryStage.setScene(new Scene(editorScreen.getRoot, 1000, 700))
  
  // === Format Selection Screen ===
  private def renderFormat(state: AppState): Unit =
    formatScreen.getNextButton.setOnAction(_ =>
      val format = formatScreen.getSelectedFormat
      val newState = Actions.setExportFormat(format)(state)
      renderStyle(newState)
    )
    
    formatScreen.getBackButton.setOnAction(_ =>
      renderEditor(state)
    )
    
    primaryStage.setScene(new Scene(formatScreen.getRoot, 600, 500))
  
  // === Style Selection Screen ===
  private def renderStyle(state: AppState): Unit =
    styleScreen.getNextButton.setOnAction(_ =>
      val theme = styleScreen.getTheme
      val color = styleScreen.getColorScheme
      val font = styleScreen.getFontSize
      val newState = state
        .copy(theme = theme)
        .copy(colorScheme = color)
        .copy(fontSize = font)
      renderDone(newState)
    )
    
    styleScreen.getBackButton.setOnAction(_ =>
      renderFormat(state)
    )
    
    primaryStage.setScene(new Scene(styleScreen.getRoot, 600, 500))
  
  // === Done Screen ===
  private def renderDone(state: AppState): Unit =
    val previewText = generatePreview(state.schedule)
    doneScreen.setPreviewText(previewText)
    
    doneScreen.getSaveButton.setOnAction(_ =>
      saveScheduleToFile(state)
    )
    
    doneScreen.getExportButton.setOnAction(_ =>
      println(s"Exporting as ${state.exportFormat} with theme: ${state.theme}, color: ${state.colorScheme}, font: ${state.fontSize}")
    )
    
    doneScreen.getEditButton.setOnAction(_ =>
      renderEditor(state)
    )
    
    primaryStage.setScene(new Scene(doneScreen.getRoot, 800, 700))
  
  // === Slot Editor Dialog ===
  private def showSlotEditor(state: AppState, existingSlot: Option[Slot], slotIdx: Int): Unit =
    val dialog = new SlotEditorDialog(existingSlot)
    val result = dialog.showAndWait()
    result.ifPresent { slot =>
      val dayIdx = state.selectedDayIndex
      if dayIdx >= 0 then
        val newState = Actions.saveSlot(dayIdx, slotIdx, slot)(state)
        renderEditor(newState)
    }
  
  // === File Operations ===
  private def saveScheduleToFile(state: AppState): Unit =
    val timestamp = LocalDateTime.now()
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val filename = s"schedule_${state.schedule.meta.groupName}_$timestamp.json"
    val file = new File(filename)
    
    JsonEncoder.saveScheduleToFile(state.schedule, file).unsafeRunSync() match
      case Right(_) =>
        println(s"Schedule saved to $filename")
        renderMain(initialState)
      case Left(error) =>
        println(s"Error saving: $error")
  
  // === Preview Generation ===
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