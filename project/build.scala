import sbt._
import Keys._
import spray.revolver.RevolverPlugin._
import com.typesafe.sbt.SbtNativePackager._

object MyBuild extends Build {
  import Dependencies._

  lazy val buildSettings = Defaults.defaultSettings ++ 
     Revolver.settings ++ 
     packageArchetype.java_application ++
     Seq(
        scalaVersion := "2.11.1",
        resolvers += Resolver.sonatypeRepo("snapshots"),
        resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
        libraryDependencies ++= Seq(
          json4sNative,
          http4sCore,
          http4sDSL,
          http4sBlaze
        ) //++ jettyDeps
    )

  lazy val myProject = Project(
    id = "my-project",
    base = file("."),
    settings = buildSettings ++ Seq(version := Dependencies.http4sVersion)
  )


  object Dependencies {

    val http4sVersion = "0.2.0-SNAPSHOT"

    lazy val http4sCore  = "org.http4s" %% "http4s-core"    % http4sVersion
    lazy val http4sDSL   = "org.http4s" %% "http4s-dsl"     % http4sVersion
    lazy val http4sBlaze = "org.http4s" %% "http4s-blaze"   % http4sVersion
    lazy val http4sJetty = "org.http4s" %% "http4s-servlet" % http4sVersion

//    lazy val jettyDeps = Seq(http4sJetty, javaxServletApi, jettyServer, jettyServlet)
//
//    lazy val javaxServletApi     = "javax.servlet"             % "javax.servlet-api"       % "3.0.1"
//    lazy val jettyServer         = "org.eclipse.jetty"         % "jetty-server"            % "9.1.0.v20131115"
//    lazy val jettyServlet        = "org.eclipse.jetty"         % "jetty-servlet"           % jettyServer.revision

    val json4sNative = "org.json4s" %% "json4s-native" % "3.2.7"
  }
  
}
