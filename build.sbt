ThisBuild / scalaVersion := "3.8.3"

lazy val root = (project in file("."))
  .settings(
    name := "Nippon"
  )
lazy val shared = project.in(file("shared"))

lazy val generator = project.in(file("generator"))
  .dependsOn(shared)

// В своем проекте ты скорее всего уже эту штуку сделал:
/*
lazy val editor = project.in(file("editor"))
  .dependsOn(shared)
 */