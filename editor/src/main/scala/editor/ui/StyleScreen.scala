package editor.ui

import javafx.scene.layout.{VBox, HBox, GridPane}
import javafx.scene.control.{Label, ComboBox, Button}
import javafx.geometry.Pos
import javafx.scene.text.Font

/**
 * Выбор стиля оформления (FR4)
 */
class StyleScreen {
  private val root = new VBox(20)
  root.setStyle("-fx-padding: 30;")

  private val titleLabel = new Label("Choose Style Theme")
  titleLabel.setFont(new Font(24))
  titleLabel.setStyle("-fx-font-weight: bold;")

  private val gridPane = new GridPane()
  gridPane.setHgap(15)
  gridPane.setVgap(15)

  // Theme selection
  val themeLabel = new Label("Theme:")
  val themeCombo = new ComboBox[String]()
  themeCombo.getItems.addAll("Light", "Dark", "Modern", "Classic")
  themeCombo.setValue("Light")
  gridPane.add(themeLabel, 0, 0)
  gridPane.add(themeCombo, 1, 0)

  // Color scheme
  val colorLabel = new Label("Color Scheme:")
  val colorCombo = new ComboBox[String]()
  colorCombo.getItems.addAll("Blue", "Green", "Red", "Purple", "Orange")
  colorCombo.setValue("Blue")
  gridPane.add(colorLabel, 0, 1)
  gridPane.add(colorCombo, 1, 1)

  // Font size
  val fontLabel = new Label("Font Size:")
  val fontCombo = new ComboBox[String]()
  fontCombo.getItems.addAll("Small", "Normal", "Large")
  fontCombo.setValue("Normal")
  gridPane.add(fontLabel, 0, 2)
  gridPane.add(fontCombo, 1, 2)

  private val buttonBox = new HBox(10)
  buttonBox.setAlignment(Pos.CENTER_RIGHT)

  private val nextBtn = new Button("Next")
  nextBtn.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-background-color: #4CAF50; -fx-text-fill: white;")

  private val backBtn = new Button("Back")
  backBtn.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-background-color: #757575; -fx-text-fill: white;")

  buttonBox.getChildren.addAll(backBtn, nextBtn)

  root.getChildren.addAll(titleLabel, gridPane, buttonBox)

  def getRoot: VBox = root
  def getNextButton: Button = nextBtn
  def getBackButton: Button = backBtn
  def getTheme: String = themeCombo.getValue
  def getColorScheme: String = colorCombo.getValue
  def getFontSize: String = fontCombo.getValue
}
