package fr.janalyse.droolscripting

import java.io.File

import org.scalatest.OptionValues._
import org.scalatest.{FlatSpec, Matchers}

class DroolsEngineDrlFileTest extends FlatSpec with Matchers {
  "DroolsScripting" should "be able to load knowledge base from a file" in {
    val drlFile = new File("tests-data/kb-doctor.drl")
    val engine = DroolsEngine(drlFile, DroolsEngineConfig.configWithEquality)
    val facts = List(
      """{"temperature":39.5}"""                 -> "diagnosis.PatientTemperature",
      """{"strength":"MEDIUM", "kind":"OILY"}""" -> "diagnosis.Coughing",
      """{"strength":"HIGH"}"""                  -> "diagnosis.MuscleAche",
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
