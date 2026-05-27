package editor.ui

import javafx.scene.Scene
import javafx.scene.layout.{VBox, HBox}
import javafx.scene.control.{Label, TextField, ComboBox, Button, CheckBox, Alert}
import javafx.geometry.Pos
import javafx.stage.Stage
import shared.{LessonType, Slot}
import editor.validation.Validator

class SlotEditorDialog(initialSlot: Option[Slot]) extends Stage:
  
  private val subjectField = new TextField()
  private val roomField = new TextField()
  private val teacherField = new TextField()
  
  private val lessonTypeCombo = new ComboBox[LessonType]()
  lessonTypeCombo.getItems.addAll(LessonType.Lecture, LessonType.Practice, LessonType.Lab)
  lessonTypeCombo.setValue(LessonType.Lecture)
  
  private val subgroupAll = new CheckBox("Вся группа")
  private val subgroup1 = new CheckBox("1 п/гр")
  private val subgroup2 = new CheckBox("2 п/гр")
  private val subgroup3 = new CheckBox("3 п/гр")
  private val subgroup4 = new CheckBox("4 п/гр")
  
  initialSlot.foreach { slot =>
    subjectField.setText(slot.subject)
    roomField.setText(slot.room)
    teacherField.setText(slot.teacher)
    lessonTypeCombo.setValue(slot.lessonType)
    
    val subs = slot.subgroups.toSet
    subgroupAll.setSelected(subs.isEmpty)
    subgroup1.setSelected(subs.contains("1"))
    subgroup2.setSelected(subs.contains("2"))
    subgroup3.setSelected(subs.contains("3"))
    subgroup4.setSelected(subs.contains("4"))
  }
  
  private val saveBtn = new Button("Сохранить")
  private val cancelBtn = new Button("Отмена")
  
  private val root = new VBox(10)
  root.setStyle("-fx-padding: 15;")
  root.getChildren.addAll(
    new Label("Предмет:"), subjectField,
    new Label("Аудитория:"), roomField,
    new Label("Преподаватель:"), teacherField,
    new Label("Тип занятия:"), lessonTypeCombo,
    new Label("Подгруппы:"),
    new HBox(10, subgroupAll, subgroup1, subgroup2, subgroup3, subgroup4),
    new HBox(10, saveBtn, cancelBtn)
  )
  
  setTitle(if initialSlot.isDefined then "Редактировать занятие" else "Добавить занятие")
  setScene(new Scene(root, 400, 450))
  
  private var result: Option[Slot] = None
  
  saveBtn.setOnAction { _ =>
    val subgroups = (subgroup1.isSelected, subgroup2.isSelected, subgroup3.isSelected, subgroup4.isSelected) match
      case (s1, s2, s3, s4) =>
        List(
          if s1 then Some("1") else None,
          if s2 then Some("2") else None,
          if s3 then Some("3") else None,
          if s4 then Some("4") else None
        ).flatten
    
    val finalSubgroups = if subgroupAll.isSelected then List.empty[String] else subgroups
    
    val slot = Slot(
      subject = subjectField.getText(),
      room = roomField.getText(),
      teacher = teacherField.getText(),
      lessonType = lessonTypeCombo.getValue,
      subgroups = finalSubgroups
    )
    
    Validator.validateSlot(slot) match
      case Right(validSlot) =>
        result = Some(validSlot)
        close()
      case Left(error) =>
        val alert = new Alert(Alert.AlertType.ERROR)
        alert.setTitle("Ошибка валидации")
        alert.setHeaderText("Некорректные данные")
        val message = error match
          case Validator.EmptyField(field) => s"Поле '$field' не может быть пустым"
          case Validator.InvalidSlot(errors) =>
            errors.map {
              case Validator.EmptyField(f) => s"Поле '$f' не может быть пустым"
              case e => e.toString
            }.mkString("\n")
          case _ => "Проверьте введённые данные"
        alert.setContentText(message)
        alert.showAndWait()
  }
  
  cancelBtn.setOnAction(_ => close())
  
  def showDialogAndWait(): Option[Slot] =
    super.showAndWait()
    result