package fr.janalyse.droolscripting

import org.slf4j.Logger

object DroolsEngineConfig {
  val configWithIdentity = DroolsEngineConfig(equalsWithIdentity = true)
  val configWithEquality = DroolsEngineConfig()
}
case class DroolsEngineConfig(
  equalsWithIdentity:Boolean=false,
  pseudoClock:Boolean=true,
  withDroolsLogging:Boolean = false
)
