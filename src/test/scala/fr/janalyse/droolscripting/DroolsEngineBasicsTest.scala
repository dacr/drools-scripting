package fr.janalyse.droolscripting

import java.util.Date
import scala.jdk.CollectionConverters._
import org.scalatest._
import org.scalatest.OptionValues._

import DroolsEngineConfig._

class DroolsEngineBasicsTest extends FlatSpec with Matchers {

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
    engine.dispose()
  }


  it should "support logging within defined kb" in {
    val drl =
      """package testdrools
         |global org.slf4j.Logger logger
         |rule "hello" when
         |then
         |  logger.info("Hello");
         |  insert("HELLO WORLD");
         |end
         |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    engine.getObjects.headOption.value shouldBe "HELLO WORLD"
    engine.dispose()
  }

  it should "be possible to get objects" in {
    val drl=
      """package test
        |declare Top
        |  age:int
        |end
        |
        |rule "init"
        |when
        |then
        |  insert(new Top(42));
        |  insert(new Top(24));
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    engine.getObjects.size shouldBe  2
  }

  it should "be possible to get object by their types" in {
    val drl=
      """package test
        |declare Top
        |  age:int
        |end
        |
        |rule "init"
        |when
        |then
        |  insert(new Top(42));
        |  insert(new Top(24));
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    engine.getModelInstances("test.Top").size shouldBe  2
  }

  it should "be possible to get object by their super types" in {
    val drl=
      """package test
        |declare Top
        |  age:int
        |end
        |
        |declare Bottom extends Top end
        |
        |rule "init"
        |when
        |then
        |  insert(new Bottom(42));
        |  insert(new Bottom(24));
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    engine.getModelInstances("test.Top").size shouldBe  2
  }

  it should "be possible to get java raw types from working memory" in {
    val drl=
      """package test
        |rule "init" when then
        |  insert("Cool Raoul");
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    val strings = engine.getModelInstances("java.lang.String")
    strings should have size 1
    strings.headOption.value shouldBe "Cool Raoul"
  }

  it should "be possible to get easily java Strings from working memory" in {
    val drl=
      """package test
        |rule "init" when then
        |  insert("Cool Raoul");
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    engine.getStrings() shouldBe List("Cool Raoul")
  }


  it should "react on an inserted message" in {
    val drl =
      """package testdrools
        |rule "hello message"
        |when
        |  $msg:String()
        |then
        |  insert(1);
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.insert("some message")
    engine.fireAllRules()
    engine.getObjects.size shouldBe 2
    engine.getModelInstances("java.lang.Integer").headOption.value shouldBe 1
    engine.dispose()
  }

  it should "allow us to access internal model definitions" in {
    val drl =
      """package testdrools
        |
        |global org.slf4j.Logger logger
        |
        |declare Message
        |  content:String
        |end
        |
        |rule "init"
        |when
        |then
        |  insert(new Message("Hello World"));
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    val instances = engine.getModelInstances("testdrools.Message")
    instances should have size 1
    engine.getModelInstanceAttribute(instances.head, "content").value shouldBe "Hello World"
    engine.getModelFirstInstanceAttribute("testdrools.Message", "content").value shouldBe "Hello World"
  }

  it should "be possible collect things and loop over found occurences" in {
    val drl =
      """package testdrools
        |
        |global org.slf4j.Logger logger
        |import java.util.LinkedList
        |
        |declare Message
        |  content:String
        |end
        |
        |
        |declare AllMessages
        |  all:String
        |end
        |
        |
        |rule "init"
        |when
        |then
        |  insert(new Message("msg1"));
        |  insert(new Message("msg2"));
        |  insert(new Message("msg3"));
        |end
        |
        |
        |rule "collect"
        |when
        |  $messages: LinkedList(size>0) from collect( Message() )
        |then
        |  StringBuffer sb=new StringBuffer();
        |  for(Object msg: $messages) sb.append(((Message)msg).getContent()+"-");
        |  insert(new AllMessages(sb.toString()));
        |end
        |
        |""".stripMargin
    info("Drools collect objects into List without supporting generics.")
    info("In this example $messages is a LinkedList of Objects.")
    info("In order to iterate in the then clause you'll to iterate through objects and use explicit cast.")
    info("WARNING : it is forbidden to access the field ")
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    val instances = engine.getModelInstances("testdrools.AllMessages")
    instances should have size 1
    engine
      .getModelFirstInstanceAttribute("testdrools.AllMessages", "all")
      .value
      .toString
      .split("[-]").toList.sorted.mkString("-") shouldBe "msg1-msg2-msg3"
  }


  it should "be possible to update logically inserted fact" in {
    val drl =
      """package testdrools
        |
        |global org.slf4j.Logger logger
        |
        |declare That
        |  value:int
        |end
        |
        |declare Something
        |  content:String
        |end
        |
        |declare Flag
        |end
        |
        |rule "init"
        |when
        |then
        |  insert(new That(42));
        |end
        |
        |rule "found something"
        |when
        |  That($value:value)
        |then
        |  insertLogical(new Something("found-"+$value));
        |  insert(new Flag());
        |end
        |
        |rule "update found"
        |no-loop
        |when
        |  String(this == "update")
        |  $that:That()
        |then
        |  update($that);
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl,configWithIdentity)

    engine.fireAllRules()
    engine.getModelInstances("testdrools.Something") should have size 1
    engine.getModelInstances("testdrools.Flag") should have size 1
    engine.getModelFirstInstanceAttribute("testdrools.Something", "content") shouldBe Some("found-42")

    engine.insert("update")
    engine.fireAllRules()
    engine.getModelInstances("testdrools.Something") should have size 1
    engine.getModelInstances("testdrools.Flag") should have size 2
    engine.getModelFirstInstanceAttribute("testdrools.Something", "content") shouldBe Some("found-42")

    info("As Something is logically inserted, it is removed on updateThat and then reinserted.")
  }

}
