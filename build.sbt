ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.0"

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.28.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % "test"

lazy val root = (project in file("."))
  .settings(
    name := "DuoCBZReader",
    idePackagePrefix := Some("be.afront.reader")
  )

Compile / mainClass := Some("be.afront.reader.DuoCBZReader")

import sbtassembly.AssemblyPlugin.autoImport._


assembly / assemblyJarName := "duoCBZReader.jar"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
