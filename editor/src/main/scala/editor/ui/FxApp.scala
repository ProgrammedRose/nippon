package editor.ui

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import javafx.application.Application
import javafx.stage.Stage
import shared.*
import editor.model.AppState

class FxApp extends Application:
  
  private var runtime: IORuntime = _
  
  override def start(stage: Stage): Unit =
    runtime = IORuntime.global
    
    val program = for
      configOrError <- ConfigLoader.loadFromResources()
      _ <- configOrError match
        case Right(cfg) =>
          val initialState = AppState.empty("", cfg)
          IO(Renderer.render(initialState, stage, cfg))
        case Left(err) =>
          IO(println(s"Failed to load config: $err"))
    yield ()
    
    program.unsafeRunSync()(runtime)