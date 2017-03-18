name := "slack-bot-gateway"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies += "com.github.gilbertw1" %% "slack-scala-client" % "0.2.1"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies ++= {
  val akkaV       = "2.4.17"
  val akkaHttpV   = "10.0.1"
  val scalaTestV  = "3.0.1"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV,
    "org.scalatest"     %% "scalatest" % scalaTestV % "test"
  )
}
