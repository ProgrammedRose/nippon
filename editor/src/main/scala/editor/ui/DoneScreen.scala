package editor.ui

import javafx.scene.layout.{VBox, HBox}
import javafx.scene.control.{Label, Button, TextArea}
import javafx.geometry.Pos
import javafx.scene.text.Font

/**
 * Экран готовности (FR6) - просмотр и сохранение результата
 */
class DoneScreen {
  private val root = new VBox(20)
  root.setStyle("-fx-padding: 30;")

  private val titleLabel = new Label("Schedule Ready!")
  titleLabel.setFont(new Font(28))
  titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;")

  private val messageLabel = new Label("Your schedule has been created successfully.")
  messageLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666666;")

  private val previewLabel = new Label("Preview:")
  previewLabel.setFont(new Font(16))
  previewLabel.setStyle("-fx-font-weight: bold;")

  private val previewArea = new TextArea()
  previewArea.setEditable(false)
  previewArea.setWrapText(true)
  previewArea.setPrefHeight(200)
  previewArea.setText("Your schedule preview will appear here...")

  private val buttonBox = new HBox(10)
  buttonBox.setAlignment(Pos.CENTER_RIGHT)

  private val saveBtn = new Button("Save to File")
  saveBtn.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-background-color: #2196F3; -fx-text-fill: white;")

  private val exportBtn = new Button("Export")
  exportBtn.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-background-color: #4CAF50; -fx-text-fill: white;")

  private val editBtn = new Button("Edit Again")
  editBtn.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-background-color: #757575; -fx-text-fill: white;")

  buttonBox.getChildren.addAll(editBtn, saveBtn, exportBtn)

  root.getChildren.addAll(
    titleLabel,
    messageLabel,
    previewLabel,
    previewArea,
    buttonBox
  )

  def getRoot: VBox = root
  def getSaveButton: Button = saveBtn
  def getExportButton: Button = exportBtn
  def getEditButton: Button = editBtn
  def setPreviewText(text: String): Unit = previewArea.setText(text)
}
