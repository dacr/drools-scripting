package fr.janalyse.droolscripting

import fr.janalyse.droolscripting.DroolsEngineConfig._
import org.scalatest.OptionValues._
import org.scalatest._


object namespace1 {
  object namespace2 {
    case class DummyX(name: String, value: Int)
  }
}
case class Dummy(name: String, value: Int)

class DroolsEngineCaseClassesTest extends FlatSpec with Matchers {

  "drools" should "manage right regular case classes, but unfortunately not those created from ammonite" in {
    info(new namespace1.namespace2.DummyX("truc", 42).getClass.getName)
    info(new Dummy("truc", 42).getClass.getName)
    val drl =
      """package testdrools
        |
        |dialect "mvel"
        |
        |//import ammonite.$file.namespace1.namespace2.Dummy
        |// Looks like import renaming is not supported in mvel as for java :(
        |//import ammonite.$file.drools$minuswith$minuscase$minusclasses$NameSpace$Dummy$ as Dummy
        |//import fr.janalyse.droolscripting.namespace1$.namespace2$.DummyX
        |import fr.janalyse.droolscripting.Dummy
        |
        |global org.slf4j.Logger logger
        |
        |rule "hello dummy"
        |when
        |  Dummy($name:name, value == 42)
        |then
        |  System.out.println("HELLO "+$name)
        |  insert(new Dummy("blah", 24))
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.insert(Dummy("truc", 42))
    engine.fireAllRules()
    engine.dispose()
  }

}
