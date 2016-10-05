name := "http4s-demo"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

val rhoVersion = "0.12.0a"

val http4sVersion = "0.14.8a"

lazy val http4sdemo = (project in file("."))
  .enablePlugins(SbtTwirl)

libraryDependencies ++= Seq(
  "org.http4s"     %% "rho-swagger"           % rhoVersion,
  "org.http4s"     %% "http4s-dsl"            % http4sVersion,
  "org.http4s"     %% "http4s-twirl"          % http4sVersion,
  "org.http4s"     %% "http4s-blaze-server"   % http4sVersion,
  "org.http4s"     %% "http4s-json4s-jackson" % http4sVersion,

  "ch.qos.logback" % "logback-classic"        % "1.1.3"
)
