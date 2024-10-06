name         := "drools-scripting"
organization := "fr.janalyse"
description  := "Make it easy to use drools expert system in scripts or small project"

licenses += "NON-AI-APACHE2" -> url(s"https://github.com/non-ai-licenses/non-ai-licenses/blob/main/NON-AI-APACHE2")

scalaVersion       := "3.5.1"
crossScalaVersions := Seq("2.13.15", "3.5.1")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

lazy val versions = new {
  val drools  = "9.44.0.Final"
  val jbpm    = "1.44.1.Final"
  val jackson = "2.18.0"
}

libraryDependencies ++= Seq(
  "org.drools"                     % "drools-core"                   % versions.drools,
  "org.drools"                     % "drools-xml-support"            % versions.drools,
  "org.drools"                     % "drools-compiler"               % versions.drools,
  "org.drools"                     % "drools-decisiontables"         % versions.drools,
  "org.drools"                     % "drools-templates"              % versions.drools,
  "org.drools"                     % "drools-serialization-protobuf" % versions.drools,
  "org.kie.kogito"                 % "jbpm-flow"                     % versions.jbpm,
  "org.kie.kogito"                 % "jbpm-bpmn2"                    % versions.jbpm,
  "org.slf4j"                      % "slf4j-api"                     % "2.0.16",
  "ch.qos.logback"                 % "logback-classic"               % "1.5.8",
  "com.fasterxml.jackson.core"     % "jackson-databind"              % versions.jackson,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"       % versions.jackson,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"         % versions.jackson,
  "org.scala-lang.modules"        %% "scala-collection-compat"       % "2.12.0",
  "org.scalatest"                 %% "scalatest"                     % "3.2.19" % "test"
)

Test / fork := true // Required to avoid "logger conflict" between sbt and code tests

Test / testOptions += {
  val rel = scalaVersion.value.split("[.]").take(2).mkString(".")
  Tests.Argument(
    "-oDF", // -oW to remove colors
    "-u",
    s"target/junitresults/scala-$rel/"
  )
}

homepage   := Some(url("https://github.com/dacr/drools-scripting"))
scmInfo    := Some(ScmInfo(url(s"https://github.com/dacr/drools-scripting"), s"git@github.com:dacr/drools-scripting.git"))
developers := List(
  Developer(
    id = "dacr",
    name = "David Crosson",
    email = "crosson.david@gmail.com",
    url = url("https://github.com/dacr")
  )
)
