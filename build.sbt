name         := "drools-scripting"
organization := "fr.janalyse"
homepage     := Some(new URL("https://github.com/dacr/drools-scripting"))
scmInfo      := Some(ScmInfo(url(s"https://github.com/dacr/drools-scripting"), s"git@github.com:dacr/drools-scripting.git"))

licenses += "NON-AI-APACHE2" -> url(s"https://github.com/non-ai-licenses/non-ai-licenses/blob/main/NON-AI-APACHE2")

scalaVersion := "3.3.0"
scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

crossScalaVersions := Seq("2.12.13", "2.13.11", "3.3.0")
// 2.12 : generates java 8 bytecodes && JVM8 required for compilation
// 2.13  : generates java 8 bytecodes && JVM8 required for compilation

Test / fork := true // Required to avoid "logger conflict" between sbt and code tests

lazy val versions = new {
  val drools = "8.40.0.Final"
  val jbpm   = "1.40.0.Final"
}

libraryDependencies ++= Seq(
  "org.drools"              % "drools-core"                   % versions.drools,
  "org.drools"              % "drools-xml-support"            % versions.drools,
  "org.drools"              % "drools-compiler"               % versions.drools,
  "org.drools"              % "drools-decisiontables"         % versions.drools,
  "org.drools"              % "drools-templates"              % versions.drools,
  "org.drools"              % "drools-serialization-protobuf" % versions.drools,
  "org.kie.kogito"          % "jbpm-flow"                     % versions.jbpm,
  "org.kie.kogito"          % "jbpm-bpmn2"                    % versions.jbpm,
  // "com.google.protobuf"     % "protobuf-java"                 % "3.16.0", // to remove some startup WARNINGS (illegal reflective access)
  "org.slf4j"               % "slf4j-api"                     % "2.0.7",
  "ch.qos.logback"          % "logback-classic"               % "1.4.8",
  "com.owlike"              % "genson"                        % "1.6",
  "org.scala-lang.modules" %% "scala-collection-compat"       % "2.11.0",
  "org.scalatest"          %% "scalatest"                     % "3.2.16" % "test"
)

Test / testOptions += {
  val rel = scalaVersion.value.split("[.]").take(2).mkString(".")
  Tests.Argument(
    "-oDF", // -oW to remove colors
    "-u",
    s"target/junitresults/scala-$rel/"
  )
}
