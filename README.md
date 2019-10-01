# Drools scripting
Drools made easy to use for scripting or testing purposes.

An [hello world drools example](https://gist.github.com/dacr/6921d569fd33182da358d6a8e383aa0a) runnable with [ammonite](http://ammonite.io/) :

```scala
import $ivy.`org.scalatest::scalatest:3.0.8`
import $ivy.`fr.janalyse::drools-scripting:1.0.1`

import org.scalatest._, org.scalatest.OptionValues._
import fr.janalyse.droolscripting._

object HelloTest extends FlatSpec with Matchers {
  "Drools" should "say hello" in {
    val drl =
      """package testdrools
        |rule "hello" when
        |then
        |  insert("HELLO WORLD");
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    engine.getObjects.headOption.value shouldBe "HELLO WORLD"
  }
}
HelloTest.execute()
```
