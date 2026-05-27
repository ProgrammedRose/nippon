package editor.ui

import javafx.scene.layout.{VBox, HBox, BorderPane}
import javafx.scene.control.{Label, TextField, Button, ListView, ListCell, ComboBox}
import javafx.geometry.Pos
import javafx.scene.text.Font
import shared.*

import java.util.concurrent.atomic.AtomicReference

class EditorScreen(initialScheduleFile: ScheduleFile | Null = null) {
  private val root = new BorderPane()
  
  // Дефолтное расписание (пустое, но валидное)
  private val defaultSchedule = ScheduleFactory.empty(
    Meta("1.0", "Группа", java.time.LocalDateTime.now.toString),
    ScheduleConfig(2, 6, 6)   // weeks=2, daysPerWeek=6, slotsPerDay=6
  )
  
  // Храним актуальное расписание в атомарной ссылке
  private val currentScheduleFileRef = new AtomicReference[ScheduleFile](
    if initialScheduleFile != null then initialScheduleFile else defaultSchedule
  )
  
  // Верхняя панель
  private val topPanel = new HBox(15)
  topPanel.setStyle("-fx-padding: 15; -fx-background-color: #e0e0e0;")
  topPanel.setAlignment(Pos.CENTER_LEFT)
  
  private val groupLabel = new Label("Group Name:")
  private val groupInput = new TextField()
  groupInput.setText(currentScheduleFileRef.get().meta.groupName)
  groupInput.setPrefWidth(300)
  
  topPanel.getChildren.addAll(groupLabel, groupInput)
  
  // Центральная панель – список дней и пар
  private val centerPanel = new VBox(15)
  centerPanel.setStyle("-fx-padding: 15;")
  
  private val weekLabel = new Label("Select week type:")
  private val weekCombo = new ComboBox[String]()
  weekCombo.getItems.addAll("Odd Week", "Even Week")
  weekCombo.setValue("Odd Week")
  
  private val daysListView = new ListView[DayBlock]()
  daysListView.setPrefHeight(400)
  daysListView.setCellFactory(_ => new DayBlockCell())
  
  // Заполняем список днями из первой (нечётной) недели, если она существует
  val schedule: ScheduleFile = currentScheduleFileRef.get()
  if (schedule.weeks.nonEmpty) {
    daysListView.getItems.addAll(schedule.weeks(0).days*)
  }
  
  centerPanel.getChildren.addAll(weekLabel, weekCombo, daysListView)
  
  // Нижняя панель с кнопками
  private val bottomPanel = new HBox(10)
  bottomPanel.setStyle("-fx-padding: 15; -fx-background-color: #f5f5f5;")
  bottomPanel.setAlignment(Pos.CENTER_RIGHT)
  
  private val addSlotBtn = new Button("Add Slot")
  addSlotBtn.setStyle(buttonStyle())
  
  private val editSlotBtn = new Button("Edit Selected")
  editSlotBtn.setStyle(buttonStyle())
  
  private val deleteSlotBtn = new Button("Delete Selected")
  deleteSlotBtn.setStyle(buttonStyle("-fx-background-color: #f44336;"))
  
  private val saveBtn = new Button("Save Schedule")
  saveBtn.setStyle(buttonStyle("-fx-background-color: #2196F3;"))
  
  bottomPanel.getChildren.addAll(addSlotBtn, editSlotBtn, deleteSlotBtn, saveBtn)
  
  root.setTop(topPanel)
  root.setCenter(centerPanel)
  root.setBottom(bottomPanel)
  
  private def buttonStyle(extra: String = ""): String =
    s"-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #4CAF50; -fx-text-fill: white; $extra"
  
  def getRoot: BorderPane = root
  
  def getScheduleFile: ScheduleFile = currentScheduleFileRef.get()
  
  def setScheduleFile(scheduleFile: ScheduleFile): Unit = {
    currentScheduleFileRef.set(scheduleFile)
    groupInput.setText(scheduleFile.meta.groupName)
    daysListView.getItems.clear()
    if (scheduleFile.weeks.nonEmpty) {
      daysListView.getItems.addAll(scheduleFile.weeks(0).days*)
    }
  }
  
  def getAddSlotButton: Button = addSlotBtn
  def getEditSlotButton: Button = editSlotBtn
  def getDeleteSlotButton: Button = deleteSlotBtn
  def getSaveButton: Button = saveBtn
  def getWeekCombo: ComboBox[String] = weekCombo
  
  // Кастомная ячейка для отображения DayBlock
  private class DayBlockCell extends ListCell[DayBlock] {
    override def updateItem(dayBlock: DayBlock, empty: Boolean): Unit = {
      super.updateItem(dayBlock, empty)
      if (empty || dayBlock == null) {
        setText(null)
        setGraphic(null)
      } else {
        val box = new VBox(5)
        val dayName = dayBlock.day match {
          case DayOfWeek.Mon => "Monday"
          case DayOfWeek.Tue => "Tuesday"
          case DayOfWeek.Wed => "Wednesday"
          case DayOfWeek.Thu => "Thursday"
          case DayOfWeek.Fri => "Friday"
          case DayOfWeek.Sat => "Saturday"
        }
        
        val dayLabel = new Label(dayName)
        dayLabel.setFont(new Font(14))
        dayLabel.setStyle("-fx-font-weight: bold;")
        
        val slotsBox = new VBox(3)
        dayBlock.slots.indices.foreach { idx =>
          val slotText = dayBlock.slots(idx) match {
            case Some(slot) => s"  ${idx + 1}. ${slot.subject} (${slot.room}) - ${slot.teacher}"
            case None => s"  ${idx + 1}. [Empty slot]"
          }
          val slotLabel = new Label(slotText)
          slotsBox.getChildren.add(slotLabel)
        }
        
        box.getChildren.addAll(dayLabel, slotsBox)
        box.setStyle("-fx-padding: 10; -fx-border-color: #cccccc; -fx-border-radius: 5;")
        setGraphic(box)
      }
    }
  }
}