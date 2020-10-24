# Drools scripting [![Build Status][travisImg]][travisLink] [![License][licenseImg]][licenseLink] [![Maven][mavenImg]][mavenLink]
Drools made easy to use for scripting or testing purposes.

This library allows you to easily design proof of concepts based of the drools expert system.
It greatly simplifies how you can quickly write drools based code examples or small experiments.

Just insert JSON facts into your drools working memory, and use the available engine methods to 
interact with the expert system and extract data from it. Data extraction can be done through
simple accessors or through the JSON format. Check the documented methods in the DroolsEngine class
or take a look to the [large amount of example I've made available](https://gist.github.com/dacr/c071a7b7d3de633281cbe84a34be47f1#drools).

An [hello world drools example](https://gist.github.com/dacr/6921d569fd33182da358d6a8e383aa0a) runnable with [ammonite](http://ammonite.io/) :

```scala
import $ivy.`fr.janalyse::drools-scripting:1.0.11`, $ivy.`org.scalatest::scalatest:3.2.2`
import fr.janalyse.droolscripting._, org.scalatest.flatspec._, org.scalatest.matchers._

object HelloTest extends AnyFlatSpec with should.Matchers {
  "Drools" should "say hello" in {
    val drl =
      """package test
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

or [an other one](https://gist.github.com/dacr/89405b045a9ef691706235b474a9a11d) :

```scala
import $ivy.`fr.janalyse::drools-scripting:1.0.10`, $ivy.`org.scalatest::scalatest:3.2.2`
import fr.janalyse.droolscripting._, org.scalatest._, flatspec._, matchers._, OptionValues._

object HelloTest extends AnyFlatSpec with should.Matchers {
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

[mavenImg]: https://img.shields.io/maven-central/v/fr.janalyse/drools-scripting_2.13.svg
[mavenImg2]: https://maven-badges.herokuapp.com/maven-central/fr.janalyse/drools-scripting_2.13/badge.svg
[mavenLink]: https://search.maven.org/#search%7Cga%7C1%7Cfr.janalyse.drools-scripting

[licenseImg]: https://img.shields.io/github/license/dacr/drools-scripting.svg
[licenseImg2]: https://img.shields.io/:license-apache2-blue.svg
[licenseLink]: LICENSE
