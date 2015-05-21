name := "caltrain"

version := "0.0.1"

scalaVersion := "2.11.6"

val phantomVersion = "1.8.0"

assemblyJarName in assembly := "caltrain.jar"

mainClass in assembly := Some("org.caltrain.Boot")

fork in run := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.11",
  "com.websudos" % "phantom_2.11" % phantomVersion,
  "com.websudos"  %% "phantom-dsl" % phantomVersion,
  "joda-time" % "joda-time" % "2.7",
  "io.spray" % "spray-can_2.11" % "1.3.3",
  "io.spray" %%  "spray-json" % "1.3.2",
  "io.spray" % "spray-routing-shapeless2_2.11" % "1.3.3",
  "net.jpountz.lz4" % "lz4" % "1.3.0",
  "org.five11" %% "scala-511" % "0.0.5",
  "org.scala-lang" % "scala-library" % "2.11.6",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "slf4j-simple" % "1.7.12",
  "org.xerial.snappy" % "snappy-java" % "1.1.2-RC1"
)

val githubUrl = url("https://raw.githubusercontent.com/jnfeinstein/scala-511/master/releases")
resolvers += Resolver.url("scala-511", githubUrl)(Resolver.ivyStylePatterns)
