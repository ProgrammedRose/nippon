package editor.ui

import javafx.application.Application
import javafx.stage.Stage
import shared.config.ConfigLoader
import shared.ScheduleConfig

class Main extends Application:
  
  override def start(primaryStage: Stage): Unit =
    val cfg: ScheduleConfig = ConfigLoader.load()
    
    val controller = new AppController(primaryStage, cfg)
    controller.start()

object Main:
  def main(args: Array[String]): Unit =
    Application.launch(classOf[Main], args*)