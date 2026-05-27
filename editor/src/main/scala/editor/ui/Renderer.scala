// editor/ui/Renderer.scala (без IO, без unsafe)
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
    else renderEditorScreen(state, stage)
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
  
  private def renderEditorScreen(state: AppState, stage: Stage): Parent =
    val topBar = new HBox(10)
    val groupLabel = new Label(state.schedule.meta.groupName)
    val backBtn = new Button("Назад")
    backBtn.setOnAction(_ =>
      val newState = Actions.backToStart(state)
      render(newState, stage, ScheduleConfig(2, 6, 6))
    )
    val saveBtn = new Button("Сохранить JSON")
    saveBtn.setOnAction(_ =>
      val fileChooser = new javafx.stage.FileChooser()
      fileChooser.setInitialFileName("schedule.json")
      val file = fileChooser.showSaveDialog(stage)
      if file != null then
        JsonEncoder.saveScheduleToFile(state.schedule, file).unsafeRunSync() match
          case Right(_) => println("Сохранено")
          case Left(e) => println(s"Ошибка: $e")
    )
    topBar.getChildren.addAll(groupLabel, backBtn, saveBtn)
    
    val weekSelector = new ComboBox[WeekType]()
    weekSelector.getItems.addAll(WeekType.Odd, WeekType.Even)
    weekSelector.setValue(state.currentWeekType)
    weekSelector.setOnAction(_ =>
      val newState = Actions.setWeekType(weekSelector.getValue)(state)
      render(newState, stage, ScheduleConfig(2, 6, 6))
    )
    
    val currentWeek = state.schedule.weeks.find(_.weekType == state.currentWeekType).get
    val listView = new javafx.scene.control.ListView[DayBlock]()
    listView.getItems.addAll(currentWeek.days*)
    listView.setCellFactory(_ => new DayBlockCell())
    
    val center = new VBox(10, weekSelector, listView)
    val root = new BorderPane()
    root.setTop(topBar)
    root.setCenter(center)
    root