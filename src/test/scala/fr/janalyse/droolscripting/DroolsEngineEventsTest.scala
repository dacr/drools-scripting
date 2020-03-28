/*
 * Copyright 2020 David Crosson
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

import java.util.Date

import fr.janalyse.droolscripting.DroolsEngineConfig._
import org.scalatest.OptionValues._
import org.scalatest.flatspec._
import org.scalatest.matchers._


import scala.jdk.CollectionConverters._

class DroolsEngineEventsTest extends AnyFlatSpec with should.Matchers {

  "Drools" should "manage events, say hello and then goodbye" in {
    val drl =
      """package testdrools
        |global org.slf4j.Logger logger
        |
        |declare Arrived
        |  @role(event)
        |  @expires(2s)
        |end
        |
        |rule "init" when
        |then
        |  insert(new Arrived());
        |end
        |
        |rule "hello" when
        |  Arrived()
        |then
        |  logger.info("HELLO John DOE");
        |end
        |
        |rule "bye" when
        |  not Arrived()
        |then
        |  logger.info("GOOD BYE John DOE !");
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    engine.getObjects.size shouldBe 1
    engine.timeShiftInSeconds(5)
    engine.fireAllRules()
    engine.getObjects.size shouldBe 0
    engine.dispose()
  }

  it should "not possible to update event datetime" in {
    val drl =
      """package testdrools
        |
        |import java.util.Date
        |
        |global org.slf4j.Logger logger
        |
        |declare Arrived
        |  @role(event)
        |  @expires(10s)
        |  @timestamp(datetime)
        |  datetime: Date
        |end
        |
        |rule "init" when
        |then
        |  insert(new Arrived(new Date(0)));
        |end
        |
        |rule "init2"
        |enabled false
        |duration (2s)
        |when
        |  $a:Arrived($datetime:datetime)
        |then
        |  modify($a) {
        |    setDatetime(new Date(3))
        |  }
        |end
        |
        |rule "hello" when
        |  Arrived()
        |then
        |  logger.info("HELLO John DOE");
        |end
        |
        |rule "bye" when
        |  not Arrived()
        |then
        |  logger.info("GOOD BYE John DOE !");
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()

    val initialDate = engine.getModelFirstInstanceAttribute("testdrools.Arrived", "datetime").value.asInstanceOf[java.util.Date]
    val currentDate = new java.util.Date()
    info("current pseudo clock state :"+engine.getCurrentTime)
    info("Arrived fields: "+engine.getFields("testdrools.Arrived").mkString(","))
    info(s"initialDate = $initialDate")
    info(s"currentDate = $currentDate")
    note("Pseudo clock starts to 0")
    engine.getObjects.size shouldBe 1

    for {
      arrived <- engine.getModelFirstInstance("testdrools.Arrived")
      handle = engine.getFactHandle(arrived)
      factType <- engine.getFactType("testdrools.Arrived")
    } {
      info("** updating datetime field **")
      val newArrived = factType.newInstance()
      factType.set(newArrived, "datetime", new Date(4000))
      engine.update(handle, newArrived)//, "datetime")
      factType.getMetaData.asScala.foreach{ case (a,b) => info(s"=> $a $b")}
      engine.getModelFirstInstanceAttribute("testdrools.Arrived","datetime").value shouldBe new Date(4000)
    }

    val updatedDate = engine.getModelFirstInstanceAttribute("testdrools.Arrived", "datetime").value.asInstanceOf[java.util.Date]
    info(s"updatedDate = $updatedDate")

    engine.timeShiftInSeconds(5) // t+5s
    engine.fireAllRules()
    engine.getObjects.size shouldBe 1

    engine.timeShiftInSeconds(4) // t+9s
    engine.fireAllRules()
    engine.getObjects.size shouldBe 1

    engine.timeShiftInSeconds(3) // t+12s
    engine.fireAllRules()
    engine.getObjects.size shouldBe 0 // original datetime and expiration occurs !!!
    info("original datetime and expiration occurs, any change is not taken into account")
    info("event are considerated as immutable !!")

    engine.timeShiftInSeconds(13) // t+25s
    engine.fireAllRules()
    engine.getObjects.size shouldBe 0

    engine.dispose()
  }

  it should "be possible refresh to an event" in {
    val drl =
      """package testdrools
        |
        |dialect "mvel"
        |
        |import java.util.Date
        |
        |global org.slf4j.Logger logger
        |
        |declare ArrivedInput
        |  @role( event )
        |  @timestamp( datetime )
        |  @expires( 10s )
        |  name: String
        |  datetime: Date
        |end
        |
        |declare Present
        |  name: String @key
        |end
        |
        |rule "present" when
        |  ArrivedInput($name:name)
        |then
        |  insertLogical(new Present($name))
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl, configWithIdentity)
    engine.fireAllRules()

    note("Pseudo clock starts to 0")
    engine.getObjects.size shouldBe 0

    def makeArrived(name:String, whenSeconds:Int):Object = {
      val factType = engine.getFactType("testdrools.ArrivedInput").get
      val newArrived = factType.newInstance()
      factType.set(newArrived, "datetime", new Date(whenSeconds*1000L))
      factType.set(newArrived, "name", "John")
      newArrived
    }

    val arrived0 = makeArrived("John Doe", 1)
    val handle0 = engine.insert(arrived0)

    engine.timeShiftInSeconds(5) // t+5s
    engine.fireAllRules()
    engine.getObjects.size shouldBe 2


    engine.timeShiftInSeconds(4) // t+9s
    engine.fireAllRules()
    engine.getObjects.size shouldBe 2

    info("DELETE AND THEN INSERT WITH UPDATED TIMESTAMP")
    engine.delete(handle0)
    val arrived1 = makeArrived("John Doe", 5)
    engine.insert(arrived1)
    info(arrived1.toString)

    engine.timeShiftInSeconds(3) // t+12s
    engine.fireAllRules()
    engine.getObjects.size shouldBe 2 // Arrived retracted and then reinserted with a new timestamp so no expiration

    engine.timeShiftInSeconds(20) // t+32s
    engine.fireAllRules()
    engine.getObjects.size shouldBe 0 // expiration occurs on the newest arrived event !!!

    engine.dispose()
  }
}
