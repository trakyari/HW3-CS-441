ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

Global / excludeLintKeys += test / fork
Global / excludeLintKeys += run / mainClass

val scalaTestVersion = "3.2.11"
val typeSafeConfigVersion = "1.4.2"
val logbackVersion = "1.2.10"
val sfl4sVersion = "2.0.0-alpha5"
val hadoopCommonVersion = "3.3.2"
val hadoopHdfsClientVersion = "3.3.2"
val logbackClassicVersion = "1.4.7"
val guavaVersion = "31.1-jre"
val AkkaVersion = "2.8.0"
val AkkaHttpVersion = "10.5.0"
val guavaAdapter2jGraphtVersion = "1.5.2"
val jGraphTlibVersion = "1.5.2"

lazy val commonDependencies = Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "org.scalatestplus" %% "mockito-4-2" % "3.2.12.0-RC2" % Test,
  "com.typesafe" % "config" % typeSafeConfigVersion,
  "com.google.guava" % "guava" % guavaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "org.jgrapht" % "jgrapht-guava" % guavaAdapter2jGraphtVersion,
  "org.jgrapht" % "jgrapht-core" % jGraphTlibVersion
).map(_.exclude("org.slf4j", "*"))

lazy val root = (project in file("."))
  .settings(
    name := "HW3",
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= Seq ("ch.qos.logback" % "logback-classic" % logbackClassicVersion),
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
