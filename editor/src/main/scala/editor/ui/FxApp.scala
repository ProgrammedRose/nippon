package editor.ui

import javafx.application.{Application, Platform}
import javafx.stage.Stage
import shared.*
import editor.model.AppState
import cats.effect.unsafe.implicits.global

class FxApp extends Application:
  
  override def start(stage: Stage): Unit =
    Platform.runLater { () =>
      ConfigLoader.loadFromResources().unsafeRunSync() match
        case Right(cfg) =>
          val initialState = AppState.empty("", cfg)
          Renderer.render(initialState, stage, cfg)
          stage.setTitle("Schedule Editor")
          stage.show()
        case Left(err) =>
          println(s"Failed to load config: $err")
    }