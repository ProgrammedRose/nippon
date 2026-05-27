package editor.ui

import javafx.scene.layout.{VBox, HBox, BorderPane}
import javafx.scene.control.{Label, TextField, Button, ListView, ListCell, ComboBox}
import javafx.geometry.Pos
import javafx.scene.text.Font
import shared.*

class EditorScreen:
  private val root = new BorderPane()
  
  // Верхняя панель
  private val topPanel = new HBox(15)
  topPanel.setStyle("-fx-padding: 15; -fx-background-color: #e0e0e0;")
  topPanel.setAlignment(Pos.CENTER_LEFT)
  
  private val backBtn = new Button("Back")
  backBtn.setStyle(buttonStyle("-fx-background-color: #757575;"))
  
  private val groupLabel = new Label("Group Name:")
  private val groupInput = new TextField()
  groupInput.setPrefWidth(300)
  
  private val saveBtn = new Button("Save & Continue")
  saveBtn.setStyle(buttonStyle("-fx-background-color: #2196F3;"))
  
  topPanel.getChildren.addAll(backBtn, groupLabel, groupInput, saveBtn)
  
  // Центральная панель – выбор недели и список дней
  private val centerPanel = new VBox(15)
  centerPanel.setStyle("-fx-padding: 15;")
  
  private val weekLabel = new Label("Select week type:")
  private val weekCombo = new ComboBox[String]()
  weekCombo.getItems.addAll("Odd Week", "Even Week")
  weekCombo.setValue("Odd Week")
  
  private val daysListView = new ListView[DayBlock]()
  daysListView.setPrefHeight(400)
  daysListView.setCellFactory(_ => new DayBlockCell())
  
  centerPanel.getChildren.addAll(weekLabel, weekCombo, daysListView)
  
  // Нижняя панель с кнопками для слотов
  private val bottomPanel = new HBox(10)
  bottomPanel.setStyle("-fx-padding: 15; -fx-background-color: #f5f5f5;")
  bottomPanel.setAlignment(Pos.CENTER_RIGHT)
  
  private val addSlotBtn = new Button("Add Slot")
  addSlotBtn.setStyle(buttonStyle())
  
  private val editSlotBtn = new Button("Edit Selected")
  editSlotBtn.setStyle(buttonStyle())
  
  private val deleteSlotBtn = new Button("Delete Selected")
  deleteSlotBtn.setStyle(buttonStyle("-fx-background-color: #f44336;"))
  
  bottomPanel.getChildren.addAll(addSlotBtn, editSlotBtn, deleteSlotBtn)
  
  root.setTop(topPanel)
  root.setCenter(centerPanel)
  root.setBottom(bottomPanel)
  
  private def buttonStyle(extra: String = ""): String =
    s"-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #4CAF50; -fx-text-fill: white; $extra"
  
  def getRoot: BorderPane = root
  
  def setScheduleFile(scheduleFile: ScheduleFile, weekType: WeekType): Unit =
    groupInput.setText(scheduleFile.meta.groupName)
    weekCombo.setValue(if weekType == WeekType.Odd then "Odd Week" else "Even Week")
    daysListView.getItems.clear()
    val week = scheduleFile.weeks.find(_.weekType == weekType).get
    daysListView.getItems.addAll(week.days*)
  
  def getBackButton: Button = backBtn
  def getSaveButton: Button = saveBtn
  def getAddSlotButton: Button = addSlotBtn
  def getEditSlotButton: Button = editSlotBtn
  def getDeleteSlotButton: Button = deleteSlotBtn
  def getWeekCombo: ComboBox[String] = weekCombo
  def getDaysListView: ListView[DayBlock] = daysListView
  
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
