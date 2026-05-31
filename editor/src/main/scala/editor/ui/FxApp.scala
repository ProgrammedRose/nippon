package editor.ui

import javafx.application.Application
import javafx.stage.Stage

import shared.*
import editor.model.AppState

import cats.effect.unsafe.implicits.global

/** запускатся из Main, подгружает Config в GUI */
class FxApp extends Application:
  
  override def start(stage: Stage): Unit =
    ConfigLoader.loadFromResources().unsafeRunSync() match
      case Right(cfg) =>
        val initial = AppState.empty("", cfg)
        
        Renderer.render(initial, stage, cfg)
        stage.setTitle("Schedule Editor")
        stage.show()
      
      case Left(err) =>
        println(err)