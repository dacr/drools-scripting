# Drools scripting [![License][licenseImg]][licenseLink] [![Maven][mavenImg]][mavenLink]
[Drools](https://www.drools.org/) made easy to use for scripting or testing purposes.

This library allows you to easily design proof of concepts based on the [drools expert system](https://www.drools.org/).
It greatly simplifies how you can quickly write drools based code examples or small experiments.

Just insert JSON facts into your drools working memory, and use the available engine methods to 
interact with the expert system and extract data from it. Data extraction can be done through
simple accessors or through the JSON format. Check the documented methods in the DroolsEngine class
or take a look to the [large amount of example I've made available](https://gist.github.com/dacr/c071a7b7d3de633281cbe84a34be47f1#drools).
(Most of them can be run directly by using the great [scala-cli][scl] REPL solution)

A selection of my drools shared code examples based on drools-scripting project :
- [drools-hello-world-revisited](https://gist.github.com/89405b045a9ef691706235b474a9a11d) : Drools Hello world revisited
- [drools-hello-world](https://gist.github.com/6921d569fd33182da358d6a8e383aa0a) : Drools Hello world
- [drools-basics](https://gist.github.com/fd48009178f3874b7dd89d6d71c7c066) : Basic drools usage examples through unit test cases.
- [drools-basics-json](https://gist.github.com/bae5897d75efe32426516d77ca6dbf9c) : Basic drools usage examples through unit test cases.
- [drools-kb-understanding](https://gist.github.com/ffe6ec1f1a3ce54ea3ac82db2ce69537) : Drools understanding rules knowledge base
- [drools-kb-events-understanding](https://gist.github.com/12dbb61d923b38a4694379e6be8d0086) : Drools understanding events base knowledge base
- [drools-kb-understanding-constraints-limitations](https://gist.github.com/277e39f97b97c579bd3f59c7f0f045ee) : Drools constraints analysis
- [drools-backward-simple](https://gist.github.com/55b8f8d90570ac6546413734d552a418) : Simple drools backward chaining example.
- [drools-equality](https://gist.github.com/db261d01a309a5aa7ba8bc28abb7dd2b) : Drools equality behavior and customization thanks to the `key` annotation.
- [drools-insertLogical-aggregate-expires](https://gist.github.com/819826154cab02918563816002381245) : Understanding insertLogical, aggregates in the context of drools events reasoning, keeping up to date a computed value
- [drools-kb-banking-advanced](https://gist.github.com/b5f0335b7c218b57e413a63633cad9f4) : Enhanced drools example banking application wired to a kafka topic
- [drools-kb-advanced-understanding](https://gist.github.com/39a769215d16359be1bbe303c51b166f) : Drools advanced understanding rules knowledge base
- [drools-kb-doctor](https://gist.github.com/6230af7afcb084dbb36b8f459a4d39c8) : Drools family knowledge base
- [drools-kb-family](https://gist.github.com/3b7a586ca28eddba389d291fe814a7e8) : Drools family knowledge base
- [drools-kb-forever-loop-on-modify](https://gist.github.com/563ff368d7f8693ff7139ad1efede13d) : Drools rules loop knowledge base
- [drools-kb-forever-loop](https://gist.github.com/919291796fb8f970657fcd1dd1c5bb76) : Drools forever loop knowledge base
- [drools-kb-official-examples-games](https://gist.github.com/1f54649e8558b905ef2227c476333498) : Drools official examples launcher examples
- [drools-kb-official-examples-simple-with-pauses](https://gist.github.com/75a56fdb3c5088835abaa5823dc415ca) : Drools step by step FireKB example
- [drools-kb-official-examples-simple](https://gist.github.com/dc2a7ea4ba3db399e378166887e13765) : Drools official minimalist FireKB example
- [drools-kb-official-examples](https://gist.github.com/c18e2d4ca7a99c4723bec37f1a418afc) : Drools official examples unit tests, it list all example knowledge base names
- [drools-kb-pets](https://gist.github.com/f029b4c19a1631c9120a78eff4e3c4f6) : Drools family knowledge base
- [drools-kb-reasoning-forward-chaining-enhanced](https://gist.github.com/1adea71ebd862de273fddba31a6bde57) : Drools forward chaining example knowledge base with roots from wikipedia definition example
- [drools-kb-reasoning-forward-chaining](https://gist.github.com/e1f605addfedef45604f2c587dfe083c) : Drools forward chaining example knowledge base with roots from wikipedia definition example
- [drools-parsing-issues](https://gist.github.com/3733f31509bd265eb103b62be5b3b8b7) : Drools parsing issues
- [drools-persistence](https://gist.github.com/1e43978a6685e67431665a914e246eed) : Learn to use drools working memory persistence to disk.


A [hello world drools example](https://gist.github.com/dacr/6921d569fd33182da358d6a8e383aa0a) runnable with [scala-cli][scl] :

```scala
// ---------------------
//> using scala "3.5.1"
//> using dep "fr.janalyse::drools-scripting:1.2.0"
//> using dep "org.scalatest::scalatest:3.2.19"
// ---------------------

import fr.janalyse.droolscripting.*, org.scalatest.flatspec.*, org.scalatest.matchers.*

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

or [an other one](https://gist.github.com/dacr/89405b045a9ef691706235b474a9a11d) runnable with [scala-cli][scl] :

```scala
// ---------------------
//> using scala "3.5.1"
//> using dep "fr.janalyse::drools-scripting:1.2.0"
//> using dep "org.scalatest::scalatest:3.2.19"
// ---------------------

import fr.janalyse.droolscripting.*, org.scalatest.*, flatspec.*, matchers.*, OptionValues.*

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


[amm]: https://ammonite.io/
[scl]: https://scala-cli.virtuslab.org/

[travisImg]: https://img.shields.io/travis/dacr/drools-scripting.svg
[travisImg2]: https://travis-ci.org/dacr/drools-scripting.png?branch=master
[travisLink]:https://travis-ci.org/dacr/drools-scripting

[mavenImg]: https://img.shields.io/maven-central/v/fr.janalyse/drools-scripting_2.13.svg
[mavenImg2]: https://maven-badges.herokuapp.com/maven-central/fr.janalyse/drools-scripting_2.13/badge.svg
[mavenLink]: https://search.maven.org/#search%7Cga%7C1%7Cfr.janalyse.drools-scripting

[licenseImg]: https://img.shields.io/github/license/dacr/drools-scripting.svg
[licenseImg2]: https://img.shields.io/:license-apache2-blue.svg
[licenseLink]: LICENSE
