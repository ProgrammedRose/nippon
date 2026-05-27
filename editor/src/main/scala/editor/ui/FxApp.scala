package editor.ui

import cats.effect.{IO, Ref}
import cats.effect.unsafe.IORuntime
import javafx.application.Application
import javafx.stage.Stage
import shared.ScheduleConfig

class FxApp extends Application:
  
  private var runtime: IORuntime = _
  
  override def start(stage: Stage): Unit =
    runtime = IORuntime.global
    val program = for
      stateRef <- Ref.of[IO, AppState](AppState.empty("", ScheduleConfig(2, 6, 6)))
      _ <- Renderer.render(stateRef, stage, ScheduleConfig(2, 6, 6))
    yield ()
    program.unsafeRunSync()(runtime)
