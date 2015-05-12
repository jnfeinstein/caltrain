name := "caltrain"

version := "0.0.1"

scalaVersion := "2.11.6"

val phantomVersion = "1.8.0"

libraryDependencies ++= Seq(
  "com.websudos" % "phantom_2.10" % phantomVersion,
  "com.websudos"  %% "phantom-dsl" % phantomVersion,
  "joda-time" % "joda-time" % "2.7",
  "org.five11" %% "scala-511" % "0.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "slf4j-simple" % "1.7.12"
)

resolvers ++= Seq(
  "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype repo"                    at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype releases"                at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots"               at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype staging"                 at "http://oss.sonatype.org/content/repositories/staging",
  "Java.net Maven2 Repository"       at "http://download.java.net/maven/2/",
  "Twitter Repository"               at "http://maven.twttr.com",
  "Websudos releases"                at "http://maven.websudos.co.uk/ext-release-local"
)
