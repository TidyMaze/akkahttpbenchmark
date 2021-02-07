
lazy val akkaHttpVersion = "10.2.3"
lazy val akkaVersion    = "2.6.12"

lazy val root = (project in file("."))
  .enablePlugins(GatlingPlugin)
  .settings(
    inThisBuild(List(
      organization    := "com.example",
      scalaVersion := "2.12.13"
    )),
    name := "akkahttpbenchmark",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "org.scalatest" %% "scalatest" % "3.1.4" % Test,
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.2.1" % Test,
      "io.gatling" % "gatling-test-framework" % "3.2.1" % Test,
    ),
  )
