ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "DuoCBZReader",
    idePackagePrefix := Some("be.afront.reader")
  )
