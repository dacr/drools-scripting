/*
 * Copyright 2020-2023 David Crosson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.janalyse.droolscripting

import fr.janalyse.droolscripting.DroolsEngineConfig._
import org.scalatest.OptionValues._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class DroolsEngineStreamModesTest extends AnyFlatSpec with should.Matchers {
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
