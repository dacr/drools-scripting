package fr.janalyse.droolscripting

import fr.janalyse.droolscripting.DroolsEngineConfig._
import org.scalatest.OptionValues._
import org.scalatest._

class DroolsEngineStreamModesTest extends FlatSpec with Matchers {
  val drl =
    """package testdrools
      |declare Nothing end
      |""".stripMargin

  "DroolsEngine" should "not allow advance time with a real time clock" in {
    val config = DroolsEngineConfig(
      equalsWithIdentity = false,
      pseudoClock = false,
      eventProcessingMode = StreamMode
    )
    val engine = DroolsEngine(drl, config)
    intercept[DroolsEngineException] {
      engine.advanceTimeHours(1)
      engine.fireAllRules()
    }
    engine.dispose()
  }
  it should "not allow advance time in cloud mode event with a pseudo clock " in {
    val config = DroolsEngineConfig(
      equalsWithIdentity = false,
      pseudoClock = true,
      eventProcessingMode = CloudMode
    )
    val engine = DroolsEngine(drl, config)
    intercept[DroolsEngineException] {
      engine.advanceTimeHours(1)
      engine.fireAllRules()
    }
    engine.dispose()
  }
}
