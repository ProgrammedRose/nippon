package editor.ui

import javafx.application.Application
import javafx.stage.Stage

/**
 * Точка входа приложения
 */
class Main extends Application:
  override def start(primaryStage: Stage): Unit =
    val controller = new AppController(primaryStage)
    controller.start()

object Main:
  def main(args: Array[String]): Unit =
    Application.launch(classOf[Main], args*)
