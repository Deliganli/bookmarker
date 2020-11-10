name := "bookmarker"

version := "0.1"

scalaVersion := "2.13.3"

val cats = Seq(
  "org.typelevel" %% "cats-core",
  "org.typelevel" %% "cats-effect"
).map(_ % "2.2.0")

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % "0.13.0")

val scalatest = Seq(
  "org.scalactic" %% "scalactic" % "3.2.0",
  "org.scalatest" %% "scalatest" % "3.2.0" % Test
)

libraryDependencies ++= Seq(
  "com.github.scopt"       %% "scopt"     % "4.0.0-RC2",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
) ++ cats ++ circe ++ scalatest

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-language:higherKinds", "-language:postfixOps", "-feature")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
