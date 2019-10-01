package fr.janalyse.droolscripting

import java.util.Date
import scala.collection.JavaConverters._
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
    engine.getObjects().headOption.value shouldBe "HELLO WORLD"
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
    engine.getObjects().headOption.value shouldBe "HELLO WORLD"
    engine.dispose()
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
    engine.getObjects().size shouldBe 2
    engine.dispose()
  }

  it should "say hello and then goodbye" in {
    val drl =
      """package testdrools
        |
        |global org.slf4j.Logger logger
        |
        |declare Arrived
        |  @role(event)
        |  @expires(2s)
        |end
        |
        |rule "init" when
        |then
        |  insert(new Arrived());
        |end
        |
        |rule "hello" when
        |  Arrived()
        |then
        |  logger.info("HELLO John DOE");
        |end
        |
        |rule "bye" when
        |  not Arrived()
        |then
        |  logger.info("GOOD BYE John DOE !");
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()
    engine.getObjects().size shouldBe 1
    engine.timeShiftInSeconds(5)
    engine.fireAllRules()
    engine.getObjects().size shouldBe 0
    engine.dispose()
  }

  it should "not possible to update event datetime" in {
    val drl =
      """package testdrools
        |
        |import java.util.Date
        |
        |global org.slf4j.Logger logger
        |
        |declare Arrived
        |  @role(event)
        |  @expires(10s)
        |  @timestamp(datetime)
        |  datetime: Date
        |end
        |
        |rule "init" when
        |then
        |  insert(new Arrived(new Date(0)));
        |end
        |
        |rule "init2"
        |enabled false
        |duration (2s)
        |when
        |  $a:Arrived($datetime:datetime)
        |then
        |  modify($a) {
        |    setDatetime(new Date(3))
        |  }
        |end
        |
        |rule "hello" when
        |  Arrived()
        |then
        |  logger.info("HELLO John DOE");
        |end
        |
        |rule "bye" when
        |  not Arrived()
        |then
        |  logger.info("GOOD BYE John DOE !");
        |end
        |""".stripMargin
    val engine = DroolsEngine(drl)
    engine.fireAllRules()

    val initialDate = engine.getModelFirstInstanceAttribute("testdrools.Arrived", "datetime").value.asInstanceOf[java.util.Date]
    val currentDate = new java.util.Date()
    info("current pseudo clock state :"+engine.getCurrentTime())
    info("Arrived fields: "+engine.getFields("testdrools.Arrived").mkString(","))
    info(s"initialDate = $initialDate")
    info(s"currentDate = $currentDate")
    note("Pseudo clock starts to 0")
    engine.getObjects().size shouldBe 1


    for {
      arrived <- engine.getModelFirstInstance("testdrools.Arrived")
      handle = engine.getFactHandle(arrived)
      factType <- engine.getFactType("testdrools.Arrived")
    } {
      info("** updating datetime field **")
      val newArrived = factType.newInstance()
      factType.set(newArrived, "datetime", new Date(4000))
      engine.update(handle, newArrived)//, "datetime")
      factType.getMetaData.asScala.foreach{ case (a,b) => info(s"=> $a $b")}
      engine.getModelFirstInstanceAttribute("testdrools.Arrived","datetime").value shouldBe new Date(4000)
    }

    val updatedDate = engine.getModelFirstInstanceAttribute("testdrools.Arrived", "datetime").value.asInstanceOf[java.util.Date]
    info(s"updatedDate = $updatedDate")

    engine.timeShiftInSeconds(5) // t+5s
    engine.fireAllRules()
    engine.getObjects().size shouldBe 1

    engine.timeShiftInSeconds(4) // t+9s
    engine.fireAllRules()
    engine.getObjects().size shouldBe 1

    engine.timeShiftInSeconds(3) // t+12s
    engine.fireAllRules()
    engine.getObjects().size shouldBe 0 // original datetime and expiration occurs !!!
    info("original datetime and expiration occurs, any change is not taken into account")
    info("event are considerated as immutable !!")

    engine.timeShiftInSeconds(13) // t+25s
    engine.fireAllRules()
    engine.getObjects().size shouldBe 0

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
        |
        |rule "init"
        |when
        |then
        |  insert(new That(42));
        |end
        |
        |
        |rule "found something"
        |when
        |  That($value:value)
        |then
        |  insertLogical(new Something("found-"+$value));
        |  insert(new Flag());
        |end
        |
        |
        |rule "update found"
        |no-loop
        |when
        |  String(this == "update")
        |  $that:That()
        |then
        |  update($that);
        |end
        |
        |
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


  it should "be possible refresh an event" in {
    val drl =
      """package testdrools
        |
        |dialect "mvel"
        |
        |import java.util.Date
        |
        |global org.slf4j.Logger logger
        |
        |declare ArrivedInput
        |  @role( event )
        |  @timestamp( datetime )
        |  @expires( 10s )
        |  name: String
        |  datetime: Date
        |end
        |
        |declare Present
        |  name: String @key
        |end
        |
        |rule "present" when
        |  ArrivedInput($name:name)
        |then
        |  insertLogical(new Present($name))
        |end
        |
        |""".stripMargin
    val engine = DroolsEngine(drl, configWithIdentity)
    engine.fireAllRules()

    note("Pseudo clock starts to 0")
    engine.getObjects().size shouldBe 0

    def makeArrived(name:String, whenSeconds:Int):Object = {
      val factType = engine.getFactType("testdrools.ArrivedInput").get
      val newArrived = factType.newInstance()
      factType.set(newArrived, "datetime", new Date(whenSeconds*1000L))
      factType.set(newArrived, "name", "John")
      newArrived
    }

    val arrived0 = makeArrived("John Doe", 1)
    val handle0 = engine.insert(arrived0)

    engine.timeShiftInSeconds(5) // t+5s
    engine.fireAllRules()
    engine.getObjects().size shouldBe 2


    engine.timeShiftInSeconds(4) // t+9s
    engine.fireAllRules()
    engine.getObjects().size shouldBe 2

    info("DELETE AND THEN INSERT WITH UPDATED TIMESTAMP")
    engine.delete(handle0)
    val arrived1 = makeArrived("John Doe", 5)
    info(arrived1.toString)

    engine.timeShiftInSeconds(3) // t+12s
    engine.fireAllRules()
    engine.getObjects().size shouldBe 2 // Arrived retracted and then reinserted with a new timestamp so no expiration

    engine.timeShiftInSeconds(20) // t+32s
    engine.fireAllRules()
    engine.getObjects().size shouldBe 0 // expiration occurs on the newest arrived event !!!


    engine.dispose()
  }



}
