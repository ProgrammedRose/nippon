package editor.ui

import javafx.scene.layout.{VBox, HBox}
import javafx.scene.control.{Label, RadioButton, ToggleGroup, Button}
import javafx.geometry.Pos
import javafx.scene.text.Font

/**
 * Выбор формата вывода (FR3)
 */
class FormatScreen {
  private val root = new VBox(20)
  root.setStyle("-fx-padding: 30;")

  private val titleLabel = new Label("Choose Export Format")
  titleLabel.setFont(new Font(24))
  titleLabel.setStyle("-fx-font-weight: bold;")

  private val formatGroup = new ToggleGroup()

  private val htmlRadio = new RadioButton("HTML (Web)")
  htmlRadio.setToggleGroup(formatGroup)
  htmlRadio.setSelected(true)
  htmlRadio.setStyle("-fx-font-size: 14;")

  private val svgRadio = new RadioButton("SVG (Vector)")
  svgRadio.setToggleGroup(formatGroup)
  svgRadio.setStyle("-fx-font-size: 14;")

  private val pdfRadio = new RadioButton("PDF (Document)")
  pdfRadio.setToggleGroup(formatGroup)
  pdfRadio.setStyle("-fx-font-size: 14;")

  private val optionsBox = new VBox(15)
  optionsBox.getChildren.addAll(htmlRadio, svgRadio, pdfRadio)

  private val buttonBox = new HBox(10)
  buttonBox.setAlignment(Pos.CENTER_RIGHT)

  private val nextBtn = new Button("Next")
  nextBtn.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-background-color: #4CAF50; -fx-text-fill: white;")

  private val backBtn = new Button("Back")
  backBtn.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-background-color: #757575; -fx-text-fill: white;")

  buttonBox.getChildren.addAll(backBtn, nextBtn)

  root.getChildren.addAll(titleLabel, optionsBox, buttonBox)

  def getRoot: VBox = root
  def getNextButton: Button = nextBtn
  def getBackButton: Button = backBtn
  def getSelectedFormat: String = {
    if (htmlRadio.isSelected) "html"
    else if (svgRadio.isSelected) "svg"
    else if (pdfRadio.isSelected) "pdf"
    else "html"
  }
}
