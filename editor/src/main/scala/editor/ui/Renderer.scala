package editor.ui

import cats.effect.unsafe.implicits.global
import editor.model.{Actions, AppState}
import editor.serialization.{JsonDecoder, JsonEncoder}
import editor.validation.Validator
import javafx.geometry.{Insets, Pos}
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.{BorderPane, HBox, Priority, VBox}
import javafx.stage.{FileChooser, Stage}
import shared.*

import java.io.File
import scala.sys.process.Process

object Renderer:
  
  def render(state: AppState, stage: Stage, cfg: ScheduleConfig): Unit =
    val root = renderEditorScreen(state, stage, cfg)
    
    stage.setScene(Scene(root, 1280, 760))
  
  private def renderEditorScreen(
                                  state: AppState,
                                  stage: Stage,
                                  cfg: ScheduleConfig
                                ): Parent =
    
    val root = BorderPane()
    
    root.setStyle(
      s"""
         | -fx-background-color: ${cfg.colors.oddWeekBg};
         |""".stripMargin
    )
    
    // ─────────────────────────────────────────────────────────────
    // TOP BAR
    // ─────────────────────────────────────────────────────────────
    
    val title = Label("Schedule Editor")
    
    title.setStyle(
      s"""
         | -fx-font-size: 24px;
         | -fx-font-weight: bold;
         | -fx-text-fill: ${cfg.colors.text};
         |""".stripMargin
    )
    
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
    
    groupField.textProperty.addListener((_, _, newValue) =>
      val updatedState =
        state.copy(
          schedule =
            state.schedule.copy(
              meta =
                state.schedule.meta.copy(
                  groupName = newValue
                )
            )
        )
      
      render(updatedState, stage, cfg)
    )
    
    def createButton(text: String, color: String): Button =
      val button = Button(text)
      
      button.setStyle(
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
      
      button
    
    val loadButton =
      createButton("Загрузить JSON", cfg.colors.room)
    
    val saveButton =
      createButton("Сохранить JSON", cfg.colors.lessonBg)
    
    val htmlButton =
      createButton("Сгенерировать HTML", cfg.colors.teacher)
    
    // ─────────────────────────────────────────────────────────────
    // LOAD
    // ─────────────────────────────────────────────────────────────
    
    loadButton.setOnAction(_ =>
      val chooser = FileChooser()
      
      val file = chooser.showOpenDialog(stage)
      
      if file != null then
        JsonDecoder.loadScheduleFromFile(file).unsafeRunSync() match
          case Right(schedule) =>
            val newState =
              Actions.loadSchedule(schedule)(state)
            
            render(newState, stage, cfg)
          
          case Left(error) =>
            showError(error.toString)
    )
    
    // ─────────────────────────────────────────────────────────────
    // SAVE JSON
    // ─────────────────────────────────────────────────────────────
    
    saveButton.setOnAction(_ =>
      Validator.validateScheduleFile(state.schedule) match
        case Left(error) =>
          showError(error.toString)
        
        case Right(_) =>
          val chooser = FileChooser()
          
          chooser.setInitialFileName("schedule.json")
          
          val file = chooser.showSaveDialog(stage)
          
          if file != null then
            JsonEncoder
              .saveScheduleToFile(state.schedule, file)
              .unsafeRunSync() match
              
              case Right(_) =>
                showInfo("JSON успешно сохранён")
              
              case Left(error) =>
                showError(error.toString)
    )
    
    // ─────────────────────────────────────────────────────────────
    // GENERATE HTML
    // ─────────────────────────────────────────────────────────────
    
    htmlButton.setOnAction(_ =>
      Validator.validateScheduleFile(state.schedule) match
        case Left(error) =>
          showError(error.toString)
        
        case Right(_) =>
          val tempJson = File.createTempFile("tempTEST", ".json", File("\\C:\\Users\\nonro\\Desktop\\"))
          println(s"TEMP: ${tempJson.getAbsolutePath}")

          JsonEncoder
            .saveScheduleToFile(state.schedule, tempJson)
            .unsafeRunSync() match
            
            case Left(error) =>
              showError(error)
            
            case Right(_) =>
              val chooser = FileChooser()
              
              chooser.setInitialFileName("schedule.html")
              
              //val outputFile =
              //  chooser.showSaveDialog(stage)
              val outputFile = File.createTempFile("tempTEST222", ".html", File("\\C:\\Users\\nonro\\Desktop\\"))

              if outputFile != null then

                val command = List(
                  //"cmd", "/c", "sbt",
                  s"generator/run --input ${tempJson.getAbsolutePath} --output ${outputFile.getAbsolutePath} --theme dark --format html"
                )

                //println(s"OUTPUT^ ${outputFile.getAbsolutePath}")
                
                val exitCode =
                  Process(command).!
                
                if exitCode == 0 then
                  showInfo("HTML успешно сгенерирован")
                else
                  showError(
                    s"Generator завершился с кодом: $exitCode"
                  )
    )
    
    val buttons = HBox(10)
    
    buttons.getChildren.addAll(
      loadButton,
      saveButton,
      htmlButton
    )
    
    val top = VBox(15)
    
    top.setPadding(Insets(20))
    
    top.getChildren.addAll(
      title,
      groupField,
      buttons
    )
    
    // ─────────────────────────────────────────────────────────────
    // WEEK SELECTOR
    // ─────────────────────────────────────────────────────────────
    
    val weekSelector = ComboBox[WeekType]()
    
    weekSelector.getItems.addAll(
      WeekType.Odd,
      WeekType.Even
    )
    
    weekSelector.setValue(state.currentWeekType)
    
    weekSelector.setStyle(
      s"""
         | -fx-background-color: ${cfg.colors.evenWeekBg};
         | -fx-mark-color: white;
         | -fx-background-radius: 10;
         | -fx-font-size: 14px;
         |""".stripMargin
    )
    
    weekSelector.setButtonCell(
      new ListCell[WeekType]:
        override def updateItem(
                                 item: WeekType,
                                 empty: Boolean
                               ): Unit =
          super.updateItem(item, empty)
          
          if empty || item == null then
            setText(null)
          else
            setText(
              item match
                case WeekType.Odd  => "Нечётная неделя"
                case WeekType.Even => "Чётная неделя"
            )
          
          setStyle(
            s"""
               | -fx-background-color: ${cfg.colors.evenWeekBg};
               | -fx-text-fill: white;
               |""".stripMargin
          )
    )
    
    weekSelector.setCellFactory(_ =>
      new ListCell[WeekType]:
        override def updateItem(
                                 item: WeekType,
                                 empty: Boolean
                               ): Unit =
          super.updateItem(item, empty)
          
          if empty || item == null then
            setText(null)
          else
            setText(
              item match
                case WeekType.Odd  => "Нечётная неделя"
                case WeekType.Even => "Чётная неделя"
            )
          
          setStyle(
            s"""
               | -fx-background-color: ${cfg.colors.evenWeekBg};
               | -fx-text-fill: white;
               |""".stripMargin
          )
    )
    
    weekSelector.setOnAction(_ =>
      val updatedState =
        Actions.setWeekType(
          weekSelector.getValue
        )(state)
      
      render(updatedState, stage, cfg)
    )
    
    // ─────────────────────────────────────────────────────────────
    // DATA
    // ─────────────────────────────────────────────────────────────
    
    val currentWeek =
      state.schedule.weeks
        .find(_.weekType == state.currentWeekType)
        .get
    
    // ─────────────────────────────────────────────────────────────
    // DAY LIST
    // ─────────────────────────────────────────────────────────────
    
    val dayList = ListView[DayBlock]()
    
    dayList.getItems.addAll(currentWeek.days*)
    
    dayList.setCellFactory(_ =>
      DayBlockCell(cfg)
    )
    
    dayList.setPrefWidth(420)
    
    dayList.setStyle(
      s"""
         | -fx-background-color: transparent;
         | -fx-control-inner-background: transparent;
         |""".stripMargin
    )
    
    if state.selectedDayIndex >= 0 then
      dayList.getSelectionModel.select(
        state.selectedDayIndex
      )
      
      dayList.scrollTo(
        state.selectedDayIndex
      )
    
    // ─────────────────────────────────────────────────────────────
    // SLOT LIST
    // ─────────────────────────────────────────────────────────────
    
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
        val selectedDay =
          currentWeek.days(state.selectedDayIndex)
        
        selectedDay.slots.zipWithIndex.map:
          case (maybeSlot, index) =>
            val time =
              cfg.lessonTimes
                .find(_.number == index + 1)
                .map(t => s"${t.start}-${t.end}")
                .getOrElse("")
            
            maybeSlot match
              case Some(slot) =>
                s"${index + 1}. [$time] ${slot.subject} / ${slot.teacher} / ${slot.room}"
              
              case None =>
                s"${index + 1}. [$time] Пусто"
      else
        Vector.empty
    
    slotList.getItems.addAll(slotValues*)
    
    if state.selectedSlotIndex >= 0 then
      slotList.getSelectionModel.select(
        state.selectedSlotIndex
      )
      
      slotList.scrollTo(
        state.selectedSlotIndex
      )
    
    // ─────────────────────────────────────────────────────────────
    // EVENTS
    // ─────────────────────────────────────────────────────────────
    
    dayList.setOnMouseClicked(_ =>
      val selectedIndex =
        dayList.getSelectionModel
          .getSelectedIndex
      
      if selectedIndex >= 0 then
        val updatedState =
          Actions.selectDay(selectedIndex)(state)
        
        render(updatedState, stage, cfg)
    )
    
    slotList.setOnMouseClicked(_ =>
      val selectedIndex =
        slotList.getSelectionModel
          .getSelectedIndex
      
      if selectedIndex >= 0 then
        val updatedState =
          Actions.selectSlot(selectedIndex)(state)
        
        render(updatedState, stage, cfg)
    )
    
    // ─────────────────────────────────────────────────────────────
    // ACTION BUTTONS
    // ─────────────────────────────────────────────────────────────
    
    val addButton =
      createButton("+ Добавить", cfg.colors.lessonBg)
    
    val editButton =
      createButton("✎ Изменить", cfg.colors.teacher)
    
    val deleteButton =
      createButton("✕ Удалить", cfg.colors.pairNumber)

    addButton.setOnAction(_ =>
      val currentDayIndex = dayList.getSelectionModel.getSelectedIndex

      if currentDayIndex >= 0 then
        val day = currentWeek.days(currentDayIndex)
        val emptyIndex = day.slots.indexWhere(_.isEmpty)

        if emptyIndex >= 0 then
          val dialog = SlotEditorDialog(None, cfg)
          dialog.showDialogAndWait().foreach: slot =>
            val updatedState =
              Actions.saveSlot(currentDayIndex, emptyIndex, slot)(state)
            render(updatedState, stage, cfg)
        else
          showError("Все слоты в этом дне заняты")
    )

    editButton.setOnAction(_ =>
      val currentDayIndex = dayList.getSelectionModel.getSelectedIndex
      val currentSlotIndex = slotList.getSelectionModel.getSelectedIndex

      if currentDayIndex >= 0 && currentSlotIndex >= 0 then
        val maybeSlot = currentWeek.days(currentDayIndex).slots(currentSlotIndex)
        maybeSlot.foreach: slot =>
          val dialog = SlotEditorDialog(Some(slot), cfg)
          dialog.showDialogAndWait().foreach: updatedSlot =>
            val updatedState =
              Actions.saveSlot(currentDayIndex, currentSlotIndex, updatedSlot)(state)
            render(updatedState, stage, cfg)
    )

    deleteButton.setOnAction(_ =>
      val currentDayIndex = dayList.getSelectionModel.getSelectedIndex
      val currentSlotIndex = slotList.getSelectionModel.getSelectedIndex

      if currentDayIndex >= 0 && currentSlotIndex >= 0 then
        val updatedState =
          Actions.deleteSlot(currentDayIndex, currentSlotIndex)(state)
        render(updatedState, stage, cfg)
    )
    
    val actions = HBox(10)
    
    actions.getChildren.addAll(
      addButton,
      editButton,
      deleteButton
    )
    
    // ─────────────────────────────────────────────────────────────
    // BODY
    // ─────────────────────────────────────────────────────────────
    
    val body = HBox(20)
    
    body.setPadding(Insets(20))
    
    HBox.setHgrow(slotList, Priority.ALWAYS)
    
    body.getChildren.addAll(
      dayList,
      slotList
    )
    
    val center = VBox(20)
    
    center.setPadding(Insets(0, 20, 20, 20))
    
    center.getChildren.addAll(
      weekSelector,
      body,
      actions
    )
    
    root.setTop(top)
    root.setCenter(center)
    
    root
  
  // ─────────────────────────────────────────────────────────────
  // ALERTS
  // ─────────────────────────────────────────────────────────────
  
  private def showError(message: String): Unit =
    val alert =
      Alert(Alert.AlertType.ERROR)
    
    alert.setTitle("Ошибка")
    alert.setHeaderText("Ошибка")
    alert.setContentText(message)
    
    alert.showAndWait()
  
  private def showInfo(message: String): Unit =
    val alert =
      Alert(Alert.AlertType.INFORMATION)
    
    alert.setTitle("Успех")
    alert.setHeaderText("Успех")
    alert.setContentText(message)
    
    alert.showAndWait()