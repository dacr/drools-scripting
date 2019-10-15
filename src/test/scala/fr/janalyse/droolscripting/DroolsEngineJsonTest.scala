package fr.janalyse.droolscripting

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.OptionValues._

class DroolsEngineJsonTest extends FlatSpec with Matchers {
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
    engine.getModelFirstInstance("testdrools.Someone") shouldBe 'defined
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
