ThisBuild / scalaVersion := "3.8.3"

lazy val root = (project in file("."))
  .settings(
    name := "Nippon"
  )
lazy val shared = project.in(file("shared"))


//libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.4"
val circeVersion = "0.14.15"
lazy val generator = project.in(file("generator"))
  .dependsOn(shared)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      "org.typelevel" %% "cats-effect" % "3.7.0"
    )
  )