package fr.janalyse.droolscripting

object DroolsEngineConfig {
  val configWithIdentity = DroolsEngineConfig(equalsWithIdentity = true)
  val configWithEquality = DroolsEngineConfig()
}
case class DroolsEngineConfig(
  equalsWithIdentity:Boolean=false,
  pseudoClock:Boolean=true
)