name := "drools-scripting"
organization :="fr.janalyse"
version := "7.26.0.Final" // This library version is sync with drools release

scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "org.drools" % "drools-core" % version.value,
  "org.drools" % "drools-compiler" % version.value,
  "org.slf4j" % "slf4j-api" % "1.7.28",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
)
