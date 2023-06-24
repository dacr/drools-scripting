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

import java.io.File

import org.scalatest.OptionValues._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class DroolsEngineDrlFileTest extends AnyFlatSpec with should.Matchers {
  "DroolsScripting" should "be able to load knowledge base from a file" in {
    val drlFile = new File("tests-data/kb-doctor.drl")
    val engine = DroolsEngine(drlFile, DroolsEngineConfig.configWithEquality)
    val facts = List(
      """{"temperature":39.5}"""                 -> "diagnosis.PatientTemperature",
      """{"strength":"MEDIUM", "kind":"OILY"}""" -> "diagnosis.Coughing",
      """{"strength":"HIGH"}"""                  -> "diagnosis.MuscleAche"
    )
    //facts.foreach{case (json, dataType) => engine.insertJson(json, dataType)}
    for { (json, dataType) <- facts } {
      engine.insertJson(json, dataType)
    }
    engine.fireAllRules()
    engine.getObjects.foreach(ob => info(ob.toString))
    engine.getModelInstances("diagnosis.Symptom").toList.size should be > 0
    engine.getModelInstances("diagnosis.Sickness").toList.size should be > 0

  }
}
