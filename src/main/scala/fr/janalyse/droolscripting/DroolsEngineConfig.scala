package fr.janalyse.droolscripting

import org.slf4j.Logger

sealed trait EventProcessingMode
object StreamMode extends EventProcessingMode
object CloudMode extends EventProcessingMode

object DroolsEngineConfig {
  val configWithIdentity = DroolsEngineConfig(equalsWithIdentity = true)
  val configWithEquality = DroolsEngineConfig()
}
case class DroolsEngineConfig(
  equalsWithIdentity:Boolean=false,
  pseudoClock:Boolean=true,
  withDroolsLogging:Boolean = false,
  eventProcessingMode:EventProcessingMode = StreamMode
) {
  val ksessionName = "ksession1"
}
