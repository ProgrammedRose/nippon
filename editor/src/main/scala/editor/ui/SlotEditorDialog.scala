package editor.ui

import javafx.scene.control.{Dialog, TextField, ComboBox, Label, ButtonType}
import javafx.scene.layout.{VBox, HBox}
import javafx.geometry.Pos
import javafx.scene.text.Font
import shared.*

class SlotEditorDialog(existingSlot: Option[Slot]) extends Dialog[Slot]:
  
  setTitle(if existingSlot.isDefined then "Edit Slot" else "Create Slot")
  setHeaderText(if existingSlot.isDefined then "Edit lesson information" else "Add new lesson")
  
  private val contentPane = new VBox(10)
  contentPane.setStyle("-fx-padding: 20;")
  
  // Subject field
  private val subjectLabel = new Label("Subject:")
  subjectLabel.setFont(new Font(12))
  private val subjectField = new TextField()
  subjectField.setPromptText("e.g., Mathematics, Physics")
  subjectField.setPrefWidth(300)
  existingSlot.foreach(slot => subjectField.setText(slot.subject))
  val subjectBox = new HBox(10, subjectLabel, subjectField)
  subjectBox.setAlignment(Pos.CENTER_LEFT)
  
  // Room field
  private val roomLabel = new Label("Room:")
  roomLabel.setFont(new Font(12))
  private val roomField = new TextField()
  roomField.setPromptText("e.g., 101A")
  roomField.setPrefWidth(300)
  existingSlot.foreach(slot => roomField.setText(slot.room))
  val roomBox = new HBox(10, roomLabel, roomField)
  roomBox.setAlignment(Pos.CENTER_LEFT)
  
  // Teacher field
  private val teacherLabel = new Label("Teacher:")
  teacherLabel.setFont(new Font(12))
  private val teacherField = new TextField()
  teacherField.setPromptText("e.g., Dr. Smith")
  teacherField.setPrefWidth(300)
  existingSlot.foreach(slot => teacherField.setText(slot.teacher))
  val teacherBox = new HBox(10, teacherLabel, teacherField)
  teacherBox.setAlignment(Pos.CENTER_LEFT)
  
  // Lesson type combo
  private val typeLabel = new Label("Type:")
  typeLabel.setFont(new Font(12))
  private val typeCombo = new ComboBox[String]()
  typeCombo.getItems.addAll("Lecture", "Practice", "Lab")
  typeCombo.setValue("Lecture")
  existingSlot.foreach { slot =>
    val typeStr = slot.lessonType match
      case LessonType.Lecture => "Lecture"
      case LessonType.Practice => "Practice"
      case LessonType.Lab => "Lab"
    typeCombo.setValue(typeStr)
  }
  val typeBox = new HBox(10, typeLabel, typeCombo)
  typeBox.setAlignment(Pos.CENTER_LEFT)
  
  // Subgroups field (comma-separated)
  private val subgroupsLabel = new Label("Subgroups:")
  subgroupsLabel.setFont(new Font(12))
  private val subgroupsField = new TextField()
  subgroupsField.setPromptText("e.g., 1, 2, 3 (comma-separated)")
  subgroupsField.setPrefWidth(300)
  existingSlot.foreach(slot => subgroupsField.setText(slot.subgroups.mkString(", ")))
  val subgroupsBox = new HBox(10, subgroupsLabel, subgroupsField)
  subgroupsBox.setAlignment(Pos.CENTER_LEFT)
  
  contentPane.getChildren.addAll(subjectBox, roomBox, teacherBox, typeBox, subgroupsBox)
  
  setDialogPane(new javafx.scene.control.DialogPane())
  getDialogPane.setContent(contentPane)
  getDialogPane.getButtonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
  
  // Result converter: extract form data into Slot
  setResultConverter { button =>
    if button == ButtonType.OK then
      val subject = subjectField.getText.trim
      val room = roomField.getText.trim
      val teacher = teacherField.getText.trim
      val lessonType = typeCombo.getValue match
        case "Lecture" => LessonType.Lecture
        case "Practice" => LessonType.Practice
        case "Lab" => LessonType.Lab
        case _ => LessonType.Lecture
      
      val subgroups = subgroupsField.getText.trim
        .split(",")
        .map(_.trim)
        .filter(_.nonEmpty)
        .toList
      
      Slot(
        subject = subject,
        room = room,
        teacher = teacher,
        lessonType = lessonType,
        subgroups = if subgroups.nonEmpty then subgroups else List("All")
      )
    else
      null.asInstanceOf[Slot]
  }
