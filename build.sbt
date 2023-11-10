ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

Global / excludeLintKeys += test / fork
Global / excludeLintKeys += run / mainClass

lazy val root = (project in file("."))
  .settings(
    name := "HW3"
  )

compileOrder := CompileOrder.JavaThenScala
test / fork := true
run / fork := true
run / javaOptions ++= Seq(
  "-Xms8G",
  "-Xmx100G",
  "-XX:+UseG1GC"
)

Compile / mainClass := Some("Main")
run / mainClass := Some("Main")

assembly / assemblyJarName := "HW2.jar"

ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
