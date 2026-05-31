package editor.ui

import cats.effect.{IO, IOApp}
import javafx.application.Application

object Main extends IOApp.Simple:
  def run: IO[Unit] = IO:
    Application.launch(classOf[FxApp], null)