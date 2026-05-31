package editor.ui

import cats.effect.unsafe.implicits.global
import editor.model.{Actions, AppState}
import editor.serialization.{JsonDecoder, JsonEncoder}
import editor.validation.Validator
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.{BorderPane, HBox, Priority, VBox}
import javafx.stage.{FileChooser, Stage}
import shared.*

import java.io.File
import scala.sys.process.Process

object Renderer:
  
  // стартовая функция, вызываемая в FxApp
  def render(state: AppState, stage: Stage, cfg: ScheduleConfig): Unit =
    val root = renderEditorScreen(state, stage, cfg)
    stage.setScene(Scene(root, 1280, 760))
  
  
  private def renderEditorScreen(state: AppState, stage: Stage, cfg: ScheduleConfig): Parent =
    val root = BorderPane()
    root.setStyle(s"-fx-background-color: ${cfg.colors.oddWeekBg};")
    
    // заголовок, поле ввода группы и кнопки управления файлами
    val title = Label("Schedule Editor")
    title.setStyle(s"-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: ${cfg.colors.text};")
    
    val groupField = TextField(state.schedule.meta.groupName)
    groupField.setPromptText("Название группы")
    groupField.setStyle(
      s"""
         | -fx-background-color: ${cfg.colors.evenWeekBg};
         | -fx-text-fill: ${cfg.colors.text};
         | -fx-background-radius: 10;
         | -fx-padding: 10;
         | -fx-font-size: 14px;
         | -fx-border-color: ${cfg.colors.border};
         | -fx-border-radius: 10;
         |""".stripMargin
    )
    // синхронизация имени группы с состоянием приложения
    groupField.textProperty.addListener((_, _, newValue) =>
      val updated = state.copy(schedule = state.schedule.copy(meta = state.schedule.meta.copy(groupName = newValue)))
      render(updated, stage, cfg)
    )
    
    def createButton(text: String, color: String): Button =
      val btn = Button(text)
      btn.setStyle(
        s"""
           | -fx-background-color: $color;
           | -fx-text-fill: white;
           | -fx-font-size: 13px;
           | -fx-font-weight: bold;
           | -fx-background-radius: 10;
           | -fx-padding: 10 16 10 16;
           | -fx-cursor: hand;
           |""".stripMargin
      )
      btn
    
    val loadButton = createButton("Загрузить JSON", cfg.colors.room)
    val saveButton = createButton("Сохранить JSON", cfg.colors.lessonBg)
    val htmlButton = createButton("Сгенерировать HTML", cfg.colors.teacher)
    
    // вспомогательные функции определены после renderEditorScreen
    loadButton.setOnAction(_ => onLoad(state, stage, cfg))
    saveButton.setOnAction(_ => onSave(state, stage, cfg))
    htmlButton.setOnAction(_ => onGenerate(state, cfg, stage))
    
    val buttons = HBox(10, loadButton, saveButton, htmlButton)
    val top = VBox(15, title, groupField, buttons)
    top.setPadding(Insets(20))
    
    // выбор типа недели (чётная / нечётная) с обновлением отображения
    val weekSelector = ComboBox[WeekType]()
    weekSelector.getItems.addAll(WeekType.Odd, WeekType.Even)
    weekSelector.setValue(state.currentWeekType)
    weekSelector.setStyle(s"-fx-background-color: ${cfg.colors.evenWeekBg}; -fx-mark-color: white; -fx-background-radius: 10; -fx-font-size: 14px;")
    
    def weekCell = new ListCell[WeekType]:
      override def updateItem(item: WeekType, empty: Boolean): Unit =
        super.updateItem(item, empty)
        if empty || item == null then setText(null)
        else setText(item match { case WeekType.Odd => "Нечётная неделя"; case WeekType.Even => "Чётная неделя" })
        setStyle(s"-fx-background-color: ${cfg.colors.evenWeekBg}; -fx-text-fill: white;")
    
    weekSelector.setButtonCell(weekCell)
    weekSelector.setCellFactory(_ => weekCell)
    weekSelector.setOnAction(_ => render(Actions.setWeekType(weekSelector.getValue)(state), stage, cfg))
    
    // данные текущей недели (отображаемой)
    val currentWeek = state.schedule.weeks.find(_.weekType == state.currentWeekType).get
    
    // список дней
    val dayList = ListView[DayBlock]()
    dayList.getItems.addAll(currentWeek.days *)
    dayList.setCellFactory(_ => DayBlockCell(cfg))
    dayList.setPrefWidth(420)
    dayList.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;")
    if state.selectedDayIndex >= 0 then
      dayList.getSelectionModel.select(state.selectedDayIndex)
      dayList.scrollTo(state.selectedDayIndex)
    
    // список пар (текстовое представление слотов)
    val slotList = ListView[String]()
    slotList.setStyle(
      s"""
         | -fx-background-color: ${cfg.colors.evenWeekBg};
         | -fx-control-inner-background: ${cfg.colors.evenWeekBg};
         | -fx-background-radius: 14;
         | -fx-padding: 10;
         |""".stripMargin
    )
    val slotValues =
      if state.selectedDayIndex >= 0 then
        val selectedDay = currentWeek.days(state.selectedDayIndex)
        selectedDay.slots.zipWithIndex.map { case (maybeSlot, idx) =>
          val time = cfg.lessonTimes.find(_.number == idx + 1).map(t => s"${t.start}-${t.end}").getOrElse("")
          maybeSlot match
            case Some(slot) => s"${idx + 1}. [$time] ${slot.subject} / ${slot.teacher} / ${slot.room}"
            case None => s"${idx + 1}. [$time] Пусто"
        }
      else Vector.empty
    slotList.getItems.addAll(slotValues *)
    if state.selectedSlotIndex >= 0 then
      slotList.getSelectionModel.select(state.selectedSlotIndex)
      slotList.scrollTo(state.selectedSlotIndex)
    
    // обработка кликов по дню и по слоту для обновления выбранных индексов в состоянии
    dayList.setOnMouseClicked(_ => onDaySelected(state, stage, cfg, dayList.getSelectionModel.getSelectedIndex))
    slotList.setOnMouseClicked(_ => onSlotSelected(state, stage, cfg, slotList.getSelectionModel.getSelectedIndex))
    
    // кнопки добавления, редактирования, удаления пары
    val addButton = createButton("+ Добавить", cfg.colors.lessonBg)
    val editButton = createButton("Изменить", cfg.colors.teacher)
    val deleteButton = createButton("✕ Удалить", cfg.colors.pairNumber)
    
    addButton.setOnAction(_ => onAdd(state, stage, cfg, dayList, slotList, currentWeek))
    editButton.setOnAction(_ => onEdit(state, stage, cfg, dayList, slotList, currentWeek))
    deleteButton.setOnAction(_ => onDelete(state, stage, cfg, dayList, slotList))
    
    val actions = HBox(10, addButton, editButton, deleteButton)
    
    // расположение: список дней слева, список пар справа, кнопки снизу
    val body = HBox(20, dayList, slotList)
    body.setPadding(Insets(20))
    HBox.setHgrow(slotList, Priority.ALWAYS)
    
    val center = VBox(20, weekSelector, body, actions)
    center.setPadding(Insets(0, 20, 20, 20))
    
    root.setTop(top)
    root.setCenter(center)
    root
  
  // Вспомогательные методы для обработчиков
  private def onLoad(state: AppState, stage: Stage, cfg: ScheduleConfig): Unit =
    val chooser = FileChooser()
    val file = chooser.showOpenDialog(stage)
    if file != null then
      JsonDecoder.loadScheduleFromFile(file).unsafeRunSync() match
        case Right(schedule) => render(Actions.loadSchedule(schedule)(state), stage, cfg)
        case Left(error)     => showError(error.toString)
  
  private def onSave(state: AppState, stage: Stage, cfg: ScheduleConfig): Unit =
    Validator.validateScheduleFile(state.schedule) match
      case Left(error) => showError(error.toString)
      case Right(_) => saveScheduleToFile(state.schedule, stage, cfg)
  
  private def saveScheduleToFile(schedule: ScheduleFile, stage: Stage, cfg: ScheduleConfig): Unit =
    val chooser = new FileChooser()
    chooser.setInitialFileName("schedule.json")
    Option(chooser.showSaveDialog(stage)).foreach { file =>
      JsonEncoder.saveScheduleToFile(schedule, file).unsafeRunSync() match
        case Right(_) => showInfo("JSON успешно сохранён")
        case Left(error) => showError(error)
    }
  
  private def onGenerate(state: AppState, cfg: ScheduleConfig, stage: Stage): Unit =
    Validator.validateScheduleFile(state.schedule) match
      case Left(error) => showError(error.toString)
      case Right(_) => generateHtmlFromSchedule(state.schedule, cfg, stage)
  
  private def generateHtmlFromSchedule(schedule: ScheduleFile, cfg: ScheduleConfig, stage: Stage): Unit =
    val tempJson = File.createTempFile("tempSchedule", ".json")
    JsonEncoder.saveScheduleToFile(schedule, tempJson).unsafeRunSync() match
      case Left(error) => showError(error)
      case Right(_) => askOutputFileAndRunGenerator(tempJson, stage)
  
  private def askOutputFileAndRunGenerator(tempJson: File, stage: Stage): Unit =
    val chooser = new FileChooser()
    chooser.setInitialFileName("schedule.html")
    Option(chooser.showSaveDialog(stage)).foreach { outputFile =>
      val jarPath = "generator/target/scala-3.3.7/generator.jar"
      val command = List(
        "java", "-jar", jarPath,
        "--input", tempJson.getAbsolutePath,
        "--output", outputFile.getAbsolutePath,
        "--theme", "dark",
        "--format", "html"
      )
      val exitCode = Process(command).!
      if exitCode == 0 then showInfo("HTML успешно сгенерирован")
      else showError(s"Generator завершился с кодом: $exitCode")
    }
  
  private def onDaySelected(state: AppState, stage: Stage, cfg: ScheduleConfig, idx: Int): Unit =
    if idx >= 0 then render(Actions.selectDay(idx)(state), stage, cfg)
  
  private def onSlotSelected(state: AppState, stage: Stage, cfg: ScheduleConfig, idx: Int): Unit =
    if idx >= 0 then render(Actions.selectSlot(idx)(state), stage, cfg)
  
  private def onAdd(state: AppState, stage: Stage, cfg: ScheduleConfig,
                    dayList: ListView[DayBlock], slotList: ListView[String],
                    currentWeek: Week): Unit =
    val dayIdx = dayList.getSelectionModel.getSelectedIndex
    val slotIdx = slotList.getSelectionModel.getSelectedIndex
    
    if dayIdx < 0 then
      showError("Сначала выберите день")
    else if slotIdx < 0 then
      showError("Сначала выберите слот (кликните по времени)")
    else
      val day = currentWeek.days(dayIdx)
      if slotIdx >= day.slots.length then
        showError("Внутренняя ошибка: слот не соответствует дню")
      else if day.slots(slotIdx).isDefined then
        showError("Этот слот уже занят")
      else
        val dialog = SlotEditorDialog(None, cfg)
        dialog.showDialogAndWait().foreach { slot =>
          render(Actions.saveSlot(dayIdx, slotIdx, slot)(state), stage, cfg)
        }
  
  private def onEdit(state: AppState, stage: Stage, cfg: ScheduleConfig, dayList: ListView[DayBlock], slotList: ListView[String], currentWeek: Week): Unit =
    val dayIdx = dayList.getSelectionModel.getSelectedIndex
    val slotIdx = slotList.getSelectionModel.getSelectedIndex
    if dayIdx >= 0 && slotIdx >= 0 then
      currentWeek.days(dayIdx).slots(slotIdx).foreach { slot =>
        val dialog = SlotEditorDialog(Some(slot), cfg)
        dialog.showDialogAndWait().foreach { updated =>
          render(Actions.saveSlot(dayIdx, slotIdx, updated)(state), stage, cfg)
        }
      }
  
  private def onDelete(state: AppState, stage: Stage, cfg: ScheduleConfig, dayList: ListView[DayBlock], slotList: ListView[String]): Unit =
    val dayIdx = dayList.getSelectionModel.getSelectedIndex
    val slotIdx = slotList.getSelectionModel.getSelectedIndex
    if dayIdx >= 0 && slotIdx >= 0 then
      render(Actions.deleteSlot(dayIdx, slotIdx)(state), stage, cfg)
  
  private def showError(message: String): Unit =
    val alert = Alert(Alert.AlertType.ERROR)
    alert.setTitle("Ошибка")
    alert.setHeaderText("Ошибка")
    alert.setContentText(message)
    alert.showAndWait()
  
  private def showInfo(message: String): Unit =
    val alert = Alert(Alert.AlertType.INFORMATION)
    alert.setTitle("Успех")
    alert.setHeaderText("Успех")
    alert.setContentText(message)
    alert.showAndWait()