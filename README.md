# Drools scripting [![Build Status][travisImg]][travisLink] [![License][licenseImg]][licenseLink] [![Maven][mavenImg]][mavenLink]
Drools made easy to use for scripting or testing purposes.

An [hello world drools example](https://gist.github.com/dacr/6921d569fd33182da358d6a8e383aa0a) runnable with [ammonite](http://ammonite.io/) :

```scala
import $ivy.`org.scalatest::scalatest:3.0.8`
import $ivy.`fr.janalyse::drools-scripting:1.0.6`

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
    engine.strings shouldBe List("HELLO WORLD")
  }
}
HelloTest.execute()
```

or an other one :

```scala
import $ivy.`fr.janalyse::drools-scripting:1.0.6`, $ivy.`org.scalatest::scalatest:3.0.8`
import fr.janalyse.droolscripting._, org.scalatest._, org.scalatest.OptionValues._

object HelloTest extends FlatSpec with Matchers {
  "Drools" should "say hello" in {
    val drl =
      """package test
        |
        |declare Someone
        |  name:String
        |end
        |
        |declare Message
        |  message:String
        |end
        |
        |rule "hello" when
        |  Someone($name:name)
        |then
        |  insert(new Message("HELLO "+$name));
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.insertJson("""{"name":"John"}""","test.Someone")
    engine.fireAllRules()
    val msgOption = engine.getModelFirstInstanceAttribute("test.Message", "message")
    msgOption.value shouldBe "HELLO John"
  }
}
HelloTest.execute()
```

[travisImg]: https://img.shields.io/travis/dacr/drools-scripting.svg
[travisImg2]: https://travis-ci.org/dacr/drools-scripting.png?branch=master
[travisLink]:https://travis-ci.org/dacr/drools-scripting

[mavenImg]: https://img.shields.io/maven-central/v/fr.janalyse/naturalsort_2.12.svg
[mavenImg2]: https://maven-badges.herokuapp.com/maven-central/fr.janalyse/naturalsort_2.12/badge.svg
[mavenLink]: https://search.maven.org/#search%7Cga%7C1%7Cfr.janalyse.drools-scripting

[licenseImg]: https://img.shields.io/github/license/dacr/drools-scripting.svg
[licenseImg2]: https://img.shields.io/:license-apache2-blue.svg
[licenseLink]: LICENSE
