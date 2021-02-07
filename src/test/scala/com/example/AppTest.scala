package com.example

import io.gatling.core.Predef.{scenario, _}
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef.http

import scala.concurrent.duration._
import scala.language.postfixOps

class AppTest extends Simulation {

  Server.startHttpServer()

  val httpProtocol = http
    .baseUrl("http://localhost:8080")

  val scn = scenario("My Scenario")
    .exec(http("hello").get("/hello"))

  setUp(scn.inject(rampUsersPerSec(1).to(600).during(60.seconds)))
    .protocols(httpProtocol)
}
