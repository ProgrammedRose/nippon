package editor.ui

import javafx.scene.layout.{VBox, HBox}
import javafx.scene.control.{Button, Label}
import javafx.geometry.Pos
import javafx.scene.text.Font

/**
 * Стартовый экран (FR1)
 */
class MainWindow {
  private val root = new VBox(20)
  root.setStyle("-fx-padding: 30; -fx-alignment: center;")
  root.setStyle("-fx-background-color: #f5f5f5;")

  private val titleLabel = new Label("Schedule Editor")
  titleLabel.setFont(new Font(32))
  titleLabel.setStyle("-fx-text-fill: #333333;")

  private val descriptionLabel = new Label("Create and edit your schedule easily")
  descriptionLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 14;")

  private val buttonBox = new HBox(15)
  buttonBox.setAlignment(Pos.CENTER)

  private val newScheduleBtn = new Button("New Schedule")
  newScheduleBtn.setStyle(buttonStyle("blue"))
  newScheduleBtn.setPrefSize(150, 40)

  private val openScheduleBtn = new Button("Open Schedule")
  openScheduleBtn.setStyle(buttonStyle("green"))
  openScheduleBtn.setPrefSize(150, 40)

  buttonBox.getChildren.addAll(newScheduleBtn, openScheduleBtn)

  root.getChildren.addAll(
    titleLabel,
    descriptionLabel,
    buttonBox
  )

  private def buttonStyle(color: String): String = {
    val bgColor = color match {
      case "blue"  => "#4CAF50"
      case "green" => "#2196F3"
      case _       => "#757575"
    }
    s"-fx-font-size: 14; -fx-padding: 10; -fx-background-color: $bgColor; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5;"
  }

  def getRoot: VBox = root
  def getNewScheduleButton: Button = newScheduleBtn
  def getOpenScheduleButton: Button = openScheduleBtn
}
