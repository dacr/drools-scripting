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
    engine.getObjects().filter(_.isInstanceOf[String]).headOption.value shouldBe "Hello John"
    engine.getModelFirstInstance("testdrools.Someone")
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
    engine.getObjects().filter(_.isInstanceOf[String]).headOption.value shouldBe "Hello John 2 France"
    engine.dispose()
  }
}
