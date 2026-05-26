ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

val javafxVersion = "21.0.2"
val os = System.getProperty("os.name").toLowerCase() match {
  case n if n.contains("win") => "win"
  case n if n.contains("mac") => "mac"
  case _ => "linux"
}

lazy val root = (project in file("."))
  .settings(
    name := "ScheduleGenUI"
  )
  .aggregate(shared, generator, editor)

lazy val shared = project.in(file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-library" % "3.3.7"
    )
  )

lazy val generator = project.in(file("generator"))
  .dependsOn(shared)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-library" % "3.3.7"
    )
  )

lazy val editor = project.in(file("editor"))
  .dependsOn(shared)
  .settings(
    name := "editor",
    
    // JavaFX dependencies
    libraryDependencies ++= Seq(
      "org.openjfx" % "javafx-controls" % javafxVersion,
      "org.openjfx" % "javafx-fxml" % javafxVersion,
      "org.openjfx" % "javafx-graphics" % javafxVersion,
      "org.openjfx" % "javafx-base" % javafxVersion,
      "org.openjfx" % "javafx-swing" % javafxVersion,
      // Platform-specific
      "org.openjfx" % s"javafx-graphics" % javafxVersion classifier s"$os",
      "org.openjfx" % s"javafx-controls" % javafxVersion classifier s"$os",
      "org.openjfx" % s"javafx-fxml" % javafxVersion classifier s"$os",
      "org.openjfx" % s"javafx-base" % javafxVersion classifier s"$os",
      "org.openjfx" % s"javafx-swing" % javafxVersion classifier s"$os"
    ),

    fork := true,
    Compile / javacOptions ++= Seq(
      "--module-path", "/path/to/javafx-sdk/lib",
      "--add-modules", "javafx.controls,javafx.fxml"
    )
  )