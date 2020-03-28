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

import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatest.OptionValues._

class DroolsEngineJsonTest extends AnyFlatSpec with should.Matchers {
  "Drools" should "manage json inputs associated to simple mvel declarations" in {
    val drl =
      """package testdrools
        |
        |declare Someone
        |  name: String
        |end
        |
        |rule "hello" when
        |  Someone($name:name)
        |then
        |  insert("Hello "+$name);
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.insertJson("""{"name":"John"}""", "testdrools.Someone")
    engine.fireAllRules()
    engine.getModelFirstInstance("testdrools.Someone").isDefined shouldBe true
    engine.getModelFirstInstance("java.lang.String").value shouldBe "Hello John"
    engine.dispose()
  }

  it should "manage json inputs associated to more complex mvel declarations" in {
    val drl =
      """package testdrools
        |
        |declare Address
        |  street:String
        |  town:String
        |  country:String
        |end
        |
        |declare Someone
        |  name: String
        |  age: int
        |  surnames: String[]
        |  address: Address
        |end
        |
        |rule "hello" when
        |  Someone($name:name, $address:address, $surnames:surnames, $country:address.country)
        |then
        |  insert("Hello "+$name+" "+$surnames.length+" "+$country);
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    val json =
      """
        |{
        |  "name": "John",
        |  "age": 42,
        |  "surnames": ["joe", "junior"],
        |  "address": {
        |    "street":"Somewhere",
        |    "town":"NoTown",
        |    "country":"France"
        |  }
        |}
        |""".stripMargin
    engine.insertJson(json, "testdrools.Someone")
    engine.fireAllRules()
    engine.getModelFirstInstance("java.lang.String").value shouldBe "Hello John 2 France"
    engine.dispose()
  }

  it should "be able to deal with ISO8601 json dates" in {
    val drl=
      """package test
        |import java.util.Date
        |declare Someone
        |  name:String
        |  birth:Date
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.insertJson("""{"name":"joe", "birth":"2019-01-01T14:00:00Z"}""", "test.Someone")
    engine.fireAllRules()
    val people = engine.getModelInstances("test.Someone")
    info(people.mkString(","))
    people should have size 1
  }

  it should "be able to use enumerations" in {
    val drl=
      """package test
        |
        |declare enum Priority LOW(0), MEDIUM(1), HIGH(2);
        |  value: int
        |end
        |
        |declare enum Color RED("red"), GREEN("green"), BLUE("blue");
        |  name: String
        |end
        |
        |declare Combo
        |  priority:Priority
        |  color:Color
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.insertJson("""LOW""", "test.Priority")
    engine.insertJson("""RED""", "test.Color")
    engine.insertJson("""{"priority":"LOW", "color":"GREEN"}""", "test.Combo")
    engine.fireAllRules()
    engine.getObjects should have size 3
    val combo = engine.getModelInstances("test.Combo").headOption.value
    combo.toString should include regex "LOW.*GREEN"
  }

  it should "be able to deserialize java Maps" in {
    val drl =
      """package test
        |import java.util.Map
        |declare Config
        |  props:Map
        |end
        |//-----------------
        |rule "check" when
        |  Config(props["scope"] == "prod")
        |then
        |  insert("OK");
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.insertJson("""{"props":{"scope":"prod"}}""", "test.Config")
    engine.fireAllRules()
    engine.strings shouldBe List("OK")
  }

}
