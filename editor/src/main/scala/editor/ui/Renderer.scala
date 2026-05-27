package editor.ui

import javafx.stage.Stage
import javafx.scene.{Scene, Parent}
import javafx.scene.layout.{VBox, HBox, BorderPane}
import javafx.scene.control.{Label, TextField, Button, ListView, ComboBox}
import javafx.geometry.Pos
import shared.*
import cats.effect.unsafe.implicits.global
import editor.model.{AppState, Actions}
import editor.serialization.JsonDecoder
import editor.serialization.JsonEncoder
import java.io.File

object Renderer:
  
  def render(state: AppState, stage: Stage, cfg: ScheduleConfig): Unit =
    val root = if !state.isEditorMode then renderStartScreen(state, stage, cfg)
    else renderEditorScreen(state, stage, cfg)
    stage.setScene(new Scene(root, 1000, 700))
  
  private def renderStartScreen(state: AppState, stage: Stage, cfg: ScheduleConfig): Parent =
    val groupInput = new TextField()
    groupInput.setPromptText("Название группы")
    groupInput.setText(state.groupNameInput)
    
    groupInput.textProperty().addListener((_, _, newVal) =>
      val newState = Actions.setGroupName(newVal)(state)
      render(newState, stage, cfg)
    )
    
    val createBtn = new Button("Создать")
    createBtn.setOnAction(_ =>
      val newState = Actions.createNewSchedule(cfg)(state)
      render(newState, stage, cfg)
    )
    
    val loadBtn = new Button("Загрузить")
    loadBtn.setOnAction(_ =>
      val fileChooser = new javafx.stage.FileChooser()
      val file = fileChooser.showOpenDialog(stage)
      if file != null then
        JsonDecoder.loadScheduleFromFile(file).unsafeRunSync() match
          case Right(schedule) =>
            val newState = Actions.loadSchedule(schedule)(state)
            render(newState, stage, cfg)
          case Left(err) => println(s"Ошибка: $err")
    )
    
    val box = new VBox(10, new Label("Генератор расписания"), groupInput, createBtn, loadBtn)
    box.setAlignment(Pos.CENTER)
    box
  
  private def renderEditorScreen(state: AppState, stage: Stage, cfg: ScheduleConfig): Parent =
    val topBar = new HBox(10)
    topBar.setStyle(s"-fx-padding: 10; -fx-background-color: ${cfg.colors.oddWeekBg};")
    val groupLabel = new Label(s"Group: ${state.schedule.meta.groupName}")
    groupLabel.setStyle(s"-fx-text-fill: ${cfg.colors.text};")
    val backBtn = new Button("← Back")
    backBtn.setStyle(s"-fx-font-size: 12; -fx-padding: 8; -fx-background-color: ${cfg.colors.border}; -fx-text-fill: ${cfg.colors.text};")
    backBtn.setOnAction(_ =>
      val newState = Actions.backToStart(state)
      render(newState, stage, cfg)
    )
    val saveBtn = new Button("Save JSON")
    saveBtn.setStyle(s"-fx-font-size: 12; -fx-padding: 8; -fx-background-color: ${cfg.colors.room}; -fx-text-fill: ${cfg.colors.text};")
    saveBtn.setOnAction(_ =>
      val fileChooser = new javafx.stage.FileChooser()
      fileChooser.setInitialFileName("schedule.json")
      val file = fileChooser.showSaveDialog(stage)
      if file != null then
        JsonEncoder.saveScheduleToFile(state.schedule, file).unsafeRunSync() match
          case Right(_) => println(s"Schedule saved: ${file.getAbsolutePath}")
          case Left(e) => println(s"Error: $e")
    )
    topBar.getChildren.addAll(groupLabel, backBtn, saveBtn)
    
    val weekSelector = new ComboBox[WeekType]()
    weekSelector.getItems.addAll(WeekType.Odd, WeekType.Even)
    weekSelector.setValue(state.currentWeekType)
    weekSelector.setStyle(s"-fx-font-size: 12; -fx-background-color: ${cfg.colors.oddWeekBg}; -fx-text-fill: ${cfg.colors.text};")
    weekSelector.setOnAction(_ =>
      val newState = Actions.setWeekType(weekSelector.getValue)(state)
      render(newState, stage, cfg)
    )
    
    val currentWeek = state.schedule.weeks.find(_.weekType == state.currentWeekType).get
    val dayListView = new javafx.scene.control.ListView[DayBlock]()
    dayListView.getItems.addAll(currentWeek.days*)
    dayListView.setCellFactory(_ => new DayBlockCell(cfg))
    dayListView.setPrefHeight(300)
    dayListView.setStyle(s"-fx-background-color: ${cfg.colors.oddWeekBg};")
    if state.selectedDayIndex >= 0 then
      dayListView.getSelectionModel.select(state.selectedDayIndex)
    
    val slotListView = new javafx.scene.control.ListView[String]()
    slotListView.setPrefHeight(300)
    slotListView.setPrefWidth(380)
    slotListView.setStyle(s"-fx-background-color: ${cfg.colors.oddWeekBg}; -fx-border-color: ${cfg.colors.border}; -fx-text-fill: ${cfg.colors.text};")
    
    def fillSlots(dayIndex: Int): Unit =
      slotListView.getItems.clear()
      if dayIndex >= 0 then
        val day = currentWeek.days(dayIndex)
        day.slots.indices.foreach { idx =>
          val timeOpt = cfg.lessonTimes.find(_.number == idx + 1)
          val timeStr = timeOpt.map(t => s" (${t.start}-${t.end})").getOrElse("")
          val title = day.slots(idx) match
            case Some(slot) => s"${idx + 1}${timeStr}. ${slot.subject} (${slot.room}) - ${slot.teacher}"
            case None => s"${idx + 1}${timeStr}. [Empty]"
          slotListView.getItems.add(title)
        }
        if state.selectedSlotIndex >= 0 then
          slotListView.getSelectionModel.select(state.selectedSlotIndex)
    
    fillSlots(state.selectedDayIndex)
    
    dayListView.setOnMouseClicked(_ =>
      val selectedDay = dayListView.getSelectionModel.getSelectedIndex
      if selectedDay >= 0 then
        val newState = Actions.selectDay(selectedDay)(state)
        render(newState, stage, cfg)
    )
    
    slotListView.setOnMouseClicked(_ =>
      val selectedSlot = slotListView.getSelectionModel.getSelectedIndex
      if selectedSlot >= 0 then
        val newState = Actions.selectSlot(selectedSlot)(state)
        render(newState, stage, cfg)
    )
    
    val buttonPanel = new HBox(10)
    buttonPanel.setStyle(s"-fx-padding: 10; -fx-background-color: ${cfg.colors.evenWeekBg};")
    
    val addSlotBtn = new Button("+ Add Slot")
    addSlotBtn.setStyle(s"-fx-font-size: 12; -fx-padding: 8; -fx-background-color: ${cfg.colors.lessonBg}; -fx-text-fill: ${cfg.colors.text};")
    addSlotBtn.setOnAction(_ =>
      val selectedDay = state.selectedDayIndex
      if selectedDay >= 0 then
        val day = currentWeek.days(selectedDay)
        val emptyIndex = day.slots.indexWhere(_.isEmpty)
        if emptyIndex >= 0 then
          val dialog = new SlotEditorDialog(None, cfg)
          dialog.showDialogAndWait().foreach { slot =>
            val newState = Actions.saveSlot(selectedDay, emptyIndex, slot)(state)
            render(newState, stage, cfg)
          }
        else println("Selected day is full")
      else println("Select a day first")
    )
    
    val editSlotBtn = new Button("✎ Edit Selected")
    editSlotBtn.setStyle(s"-fx-font-size: 12; -fx-padding: 8; -fx-background-color: ${cfg.colors.teacher}; -fx-text-fill: ${cfg.colors.text};")
    editSlotBtn.setOnAction(_ =>
      val selectedDay = state.selectedDayIndex
      val selectedSlot = state.selectedSlotIndex
      if selectedDay >= 0 && selectedSlot >= 0 then
        val day = currentWeek.days(selectedDay)
        day.slots(selectedSlot) match
          case Some(existingSlot) =>
            val dialog = new SlotEditorDialog(Some(existingSlot), cfg)
            dialog.showDialogAndWait().foreach { slot =>
              val newState = Actions.saveSlot(selectedDay, selectedSlot, slot)(state)
              render(newState, stage, cfg)
            }
          case None => println("Select a filled slot first")
      else println("Select a day and slot first")
    )
    
    val deleteSlotBtn = new Button("✕ Delete Selected")
    deleteSlotBtn.setStyle(s"-fx-font-size: 12; -fx-padding: 8; -fx-background-color: ${cfg.colors.pairNumber}; -fx-text-fill: ${cfg.colors.text};")
    deleteSlotBtn.setOnAction(_ =>
      val selectedDay = state.selectedDayIndex
      val selectedSlot = state.selectedSlotIndex
      if selectedDay >= 0 && selectedSlot >= 0 then
        val newState = Actions.deleteSlot(selectedDay, selectedSlot)(state)
        render(newState, stage, cfg)
      else println("Select a day and slot first")
    )
    
    buttonPanel.getChildren.addAll(addSlotBtn, editSlotBtn, deleteSlotBtn)
    
    val body = new HBox(10)
    body.getChildren.addAll(dayListView, slotListView)
    
    val center = new VBox(10)
    center.setStyle(s"-fx-padding: 10; -fx-background-color: ${cfg.colors.oddWeekBg};")
    center.getChildren.addAll(weekSelector, body, buttonPanel)
    
    val root = new BorderPane()
    root.setTop(topBar)
    root.setCenter(center)
    root