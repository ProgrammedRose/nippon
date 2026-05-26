ThisBuild / scalaVersion := "3.3.7"

val circeVersion = "0.14.15"
val catsEffectVersion = "3.7.0"
val javafxVersion = "21.0.2"

lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown OS")
}

lazy val shared = project
  .in(file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      "io.circe"      %% "circe-core"    % circeVersion,
      "io.circe"      %% "circe-generic" % circeVersion,
      "io.circe"      %% "circe-parser"  % circeVersion,
      "io.circe" %% "circe-yaml" % "0.15.1",
      "org.typelevel" %% "cats-effect"   % catsEffectVersion
    )
  )
lazy val generator = project
  .in(file("generator"))
  .dependsOn(shared)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml"    % "2.2.0",
      "io.circe"               %% "circe-parser" % circeVersion
    )
  )

lazy val editor = project
  .in(file("editor"))
  .dependsOn(shared)
  .settings(
    libraryDependencies ++= Seq(
      "org.openjfx" % "javafx-base"     % javafxVersion classifier osName,
      "org.openjfx" % "javafx-controls" % javafxVersion classifier osName,
      "org.openjfx" % "javafx-fxml"     % javafxVersion classifier osName
    ),
    
    fork := true,
    
    Compile / mainClass := Some("editor.ui.Main"),
    
    run / javaOptions ++= {
      val javafxJars =
        (Compile / dependencyClasspath).value.files
          .filter(_.getName.contains("javafx"))
      
      Seq(
        "--module-path",
        javafxJars.mkString(java.io.File.pathSeparator),
        
        "--add-modules",
        "javafx.controls,javafx.fxml"
      )
    }
  )