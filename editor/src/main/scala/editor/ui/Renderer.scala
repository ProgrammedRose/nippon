package editor.ui

import cats.effect.{IO, Ref}
import javafx.stage.Stage
import javafx.scene.{Scene, Parent}
import javafx.scene.layout.{VBox, HBox, BorderPane}
import javafx.scene.control.{Label, TextField, Button, ListView, ComboBox}
import javafx.geometry.Pos
import shared.*
import editor.model.{AppState, Actions}
import editor.serialization.JsonDecoder

object Renderer:
  
  def render(stateRef: Ref[IO, AppState], stage: Stage, cfg: ScheduleConfig): IO[Unit] =
    for
      state <- stateRef.get
      scene <- createScene(state, stateRef, stage, cfg)
      _ <- IO(stage.setScene(scene))
      _ <- IO(stage.show())
    yield ()
  
  private def createScene(state: AppState, stateRef: Ref[IO, AppState], stage: Stage, cfg: ScheduleConfig): IO[Scene] =
    val root = if !state.isEditorMode then renderStartScreen(state, stateRef, stage, cfg)
    else renderEditorScreen(state, stateRef, stage)
    IO(new Scene(root, 1000, 700))
  
  private def renderStartScreen(state: AppState, stateRef: Ref[IO, AppState], stage: Stage, cfg: ScheduleConfig): Parent =
    val groupInput = new TextField()
    groupInput.setPromptText("Название группы")
    groupInput.setText(state.groupNameInput)
    
    groupInput.textProperty().addListener((_, _, newVal) =>
      stateRef.update(Actions.setGroupName(newVal)).flatMap(_ =>
        render(stateRef, stage, cfg)
      ).unsafeRunSync() // единственный unsafe в UI-колбэках, иначе никак с JavaFX
    )
    
    val createBtn = new Button("Создать")
    createBtn.setOnAction(_ =>
      stateRef.update(Actions.createNewSchedule(cfg)).flatMap(_ =>
        render(stateRef, stage, cfg)
      ).unsafeRunSync()
    )
    
    val loadBtn = new Button("Загрузить")
    loadBtn.setOnAction(_ =>
      val fileChooser = new javafx.stage.FileChooser()
      val file = fileChooser.showOpenDialog(stage)
      if file != null then
        (for
          result <- JsonDecoder.loadScheduleFromFile(file)
          _ <- result match
            case Right(schedule) => stateRef.update(Actions.loadSchedule(schedule))
            case Left(err) => IO(println(s"Ошибка: $err"))
          _ <- render(stateRef, stage, cfg)
        yield ()).unsafeRunSync()
    )
    
    val box = new VBox(10, new Label("Генератор расписания"), groupInput, createBtn, loadBtn)
    box.setAlignment(Pos.CENTER)
    box
  
  private def renderEditorScreen(state: AppState, stateRef: Ref[IO, AppState], stage: Stage): Parent =
    val topBar = new HBox(10)
    val groupLabel = new Label(state.schedule.meta.groupName)
    val backBtn = new Button("Назад")
    backBtn.setOnAction(_ =>
      stateRef.update(Actions.backToStart).flatMap(_ =>
        render(stateRef, stage, ScheduleConfig(2, 6, 6))
      ).unsafeRunSync()
    )
    val saveBtn = new Button("Сохранить JSON")
    saveBtn.setOnAction(_ =>
      val fileChooser = new javafx.stage.FileChooser()
      fileChooser.setInitialFileName("schedule.json")
      val file = fileChooser.showSaveDialog(stage)
      if file != null then
        (for
          state <- stateRef.get
          result <- Actions.saveToJson(file, state.schedule)
          _ <- IO(result.foreach(println))
        yield ()).unsafeRunSync()
    )
    topBar.getChildren.addAll(groupLabel, backBtn, saveBtn)
    
    val weekSelector = new ComboBox[WeekType]()
    weekSelector.getItems.addAll(WeekType.Odd, WeekType.Even)
    weekSelector.setValue(state.currentWeekType)
    weekSelector.setOnAction(_ =>
      stateRef.update(Actions.setWeekType(weekSelector.getValue)).flatMap(_ =>
        render(stateRef, stage, ScheduleConfig(2, 6, 6))
      ).unsafeRunSync()
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