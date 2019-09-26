name := "drools-scripting"
organization := "fr.janalyse"
homepage := Some(new URL("https://github.com/dacr/drools-scripting"))


scalaVersion := "2.13.1"
crossScalaVersions := Seq(scalaVersion.value, "2.12.10")

libraryDependencies ++= Seq(
  "org.drools" % "drools-core" % "7.26.0.Final",
  "org.drools" % "drools-compiler" % "7.26.0.Final",
  "org.slf4j" % "slf4j-api" % "1.7.28",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
)

pomIncludeRepository := { _ => false }

useGpg := true

licenses += "Apache 2" -> url(s"http://www.apache.org/licenses/LICENSE-2.0.txt")
releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishMavenStyle := true
publishArtifact in Test := false
publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)

scmInfo := Some(ScmInfo(url(s"https://github.com/dacr/drools-scripting"), s"git@github.com:dacr/drools-scripting.git"))

PgpKeys.useGpg in Global := true // workaround with pgp and sbt 1.2.x
pgpSecretRing := pgpPublicRing.value // workaround with pgp and sbt 1.2.x

pomExtra in Global := {
  <developers>
    <developer>
      <id>dacr</id>
      <name>David Crosson</name>
      <url>https://github.com/dacr</url>
    </developer>
  </developers>
}


import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
