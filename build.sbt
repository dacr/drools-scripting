name := "drools-scripting"
organization := "fr.janalyse"
homepage := Some(new URL("https://github.com/dacr/drools-scripting"))
licenses += "Apache 2" -> url(s"http://www.apache.org/licenses/LICENSE-2.0.txt")
scmInfo := Some(ScmInfo(url(s"https://github.com/dacr/drools-scripting"), s"git@github.com:dacr/drools-scripting.git"))


scalaVersion := "2.13.1"
scalacOptions ++= Seq( "-deprecation", "-unchecked", "-feature")

crossScalaVersions := Seq("2.12.11", "2.13.1")
// 2.12.11 : generates java 8 bytecodes && JVM8 required for compilation
// 2.13.1  : generates java 8 bytecodes && JVM8 required for compilation

Test / fork := true  // Required to avoid "logger conflict" between sbt and code tests

libraryDependencies ++= Seq(
  "org.drools"               % "drools-core"             % "7.34.0.Final",
  "org.drools"               % "drools-compiler"         % "7.34.0.Final",
  "org.slf4j"                % "slf4j-api"               % "1.7.30",
  "ch.qos.logback"           % "logback-classic"         % "1.2.3",
  "com.owlike"               % "genson"                  % "1.6",
  "org.scala-lang.modules"  %% "scala-collection-compat" % "2.1.4",
  "org.scalatest"           %% "scalatest"               % "3.1.1" % "test",
)

testOptions in Test += {
  val rel = scalaVersion.value.split("[.]").take(2).mkString(".")
  Tests.Argument(
    "-oDF", // -oW to remove colors
    "-u", s"target/junitresults/scala-$rel/"
  )
}
