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

class DroolsEngineEqualityTest extends AnyFlatSpec with should.Matchers {

  "Drools" should "equality check with mvel objects" in {
    info("Take care mvel declarations don't implement the right hashcode and equals implementation by default => solution add @key annotation !")
    val drl =
      """package testdrools
        |dialect "mvel"
        |
        |global org.slf4j.Logger logger
        |
        |declare Identity
        |  name:String @key
        |  age:int
        |end
        |
        |declare Someone
        |  cardId:Identity
        |end
        |
        |rule "init" when
        |then
        |  insert(new Someone(new Identity("John", 42)));
        |  insert(new Someone(new Identity("John", 43)));
        |end
        |
        |
        |rule "must not fired"
        |when
        |  Someone($a:cardId)
        |  Someone(cardId != $a)
        |then
        |  insertLogical("same people");
        |end
        |
        |rule "check john age"
        |when
        |  Someone($a:cardId)
        |then
        |  logger.info("age="+$a.age);
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl, configWithEquality)
    engine.fireAllRules()
    engine.getObjects.size shouldBe 2
    engine.dispose()
  }

  it should "equality check with mvel objects 2" in {
    info("Take care mvel declarations don't implement the right hashcode and equals implementation by default => solution add @key annotation !")
    val drl =
      """package testdrools
        |dialect "mvel"
        |
        |global org.slf4j.Logger logger
        |
        |declare Identity
        |  name:String @key
        |  age:int
        |end
        |
        |rule "init" when
        |then
        |  insert(new Identity("John", 42));
        |  insert(new Identity("John", 43));
        |end
        |
        |rule "check john age"
        |when
        |  $a:Identity()
        |then
        |  logger.info("age="+$a.age);
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl, configWithEquality)
    engine.fireAllRules()
    engine.getObjects.size shouldBe 1
    engine.getModelFirstInstanceAttribute("testdrools.Identity", "age").value.toString shouldBe "42" // TODO bof :( & scala3 is more restrictive :)
    engine.dispose()
  }


  it should "equality check with external objects" in {
    val drl =
      """package testdrools
        |dialect "java"
        |import java.awt.Point
        |
        |global org.slf4j.Logger logger
        |
        |declare Someone
        |  point:Point
        |end
        |
        |rule "init" when
        |then
        |  insert(new Point(1,1));
        |  insert(new Point(1,1));
        |end
        |
        |
        |rule "one or two points"
        |when
        |  $a:Point()
        |  Point(this != $a)
        |then
        |  insertLogical("same point");
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl,configWithEquality)
    engine.fireAllRules()
    engine.getObjects.size shouldBe 1
    engine.dispose()
  }



  it should "say hello and then goodbye check equality" in {
    val drl =
      """package testdrools
        |import java.util.Date
        |
        |global org.slf4j.Logger logger
        |
        |declare Arrived
        |  @role(event)
        |  @expires(2s)
        |end
        |
        |declare Hello
        |  message:String @key
        |end
        |
        |rule "init" when
        |then
        |  insert(new Arrived());
        |  insert(new Arrived());
        |end
        |
        |rule "hello" when
        |  Arrived()
        |then
        |  insert(new Hello("yearr"));
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl, configWithEquality)
    engine.fireAllRules()
    engine.getObjects.size shouldBe 3
    engine.timeShiftInSeconds(5)
    engine.fireAllRules()
    engine.getObjects.size shouldBe 1
    engine.dispose()
  }


  it should "say hello and then goodbye check equality again" in {
    val drl =
      """package testdrools
        |import java.util.Date
        |
        |global org.slf4j.Logger logger
        |
        |declare HelloInput
        |  x:int
        |  y:String
        |  z:String
        |end
        |
        |declare Hello
        |  x:int
        |  y:String
        |end
        |
        |
        |rule "init" when
        |then
        |  insert(new HelloInput(42, "the truth", "here"));
        |end
        |
        |
        |rule "hello" when
        |  HelloInput($x:x, $y:y)
        |then
        |  insertLogical(new Hello($x, $y));
        |end
        |
        |
        |rule "init2"
        |duration 2000
        |no-loop
        |when
        |  $a:HelloInput()
        |then
        |  modify($a) {
        |    //setZ("not here"); /* no impact on hello rule*/
        |    setY("the TRUTH"); /* has impact on hello rule because of the insertLogical and $y:y */
        |  }
        |end
        |
        |
        |""".stripMargin
    val engine = DroolsEngine(drl, configWithEquality)
    engine.fireAllRules()
    engine.timeShiftInSeconds(5)
    engine.fireAllRules()
    engine.getModelFirstInstanceAttribute("testdrools.Hello", "y").value shouldBe "the TRUTH"
    engine.dispose()
  }


}
