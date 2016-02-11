
import sbt._
import Keys._
import spray.revolver.RevolverPlugin._
import com.typesafe.sbt.SbtNativePackager._

import play.twirl.sbt.SbtTwirl

object MyBuild extends Build {
  import Dependencies._

  lazy val buildSettings = Revolver.settings ++
     packageArchetype.java_application ++
     Seq(
        scalaVersion := "2.11.7",
        resolvers += Resolver.sonatypeRepo("snapshots"),
        resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
        libraryDependencies ++= Seq(
          http4sDSL,
          http4sTwirl,
          http4sBlaze,
          rhoSwagger,
          http4s_jackson,
          logbackClassic 
        )
    )

  lazy val myProject = Project(
    id = "my-project",
    base = file("."),
    settings = buildSettings :+ (version := Dependencies.http4sVersion)
  ).enablePlugins(SbtTwirl)


  object Dependencies {

    val http4sVersion = "0.12.1"
    val rhoVersion = "0.9.0"

    lazy val rhoSwagger     = "org.http4s"     %% "rho-swagger"           % rhoVersion
    lazy val http4sDSL      = "org.http4s"     %% "http4s-dsl"            % http4sVersion
    lazy val http4sTwirl    = "org.http4s"     %% "http4s-twirl"          % http4sVersion
    lazy val http4sBlaze    = "org.http4s"     %% "http4s-blaze-server"   % http4sVersion
    lazy val http4s_jackson = "org.http4s"     %% "http4s-json4s-jackson" % http4sVersion

    lazy val logbackClassic = "ch.qos.logback" % "logback-classic"        % "1.1.3"
  }
  
}
