package fr.janalyse.droolscripting

import org.slf4j._

import scala.collection.JavaConverters._
import scala.util.Try
import org.kie.api._
import org.kie.api.runtime.KieSession
import org.kie.api.builder._
import org.kie.api.io._
import org.kie.api.time.{SessionClock, SessionPseudoClock}
import org.kie.internal.io.ResourceFactory
import org.kie.api.definition.`type`.FactType
import java.util.Date

import org.drools.compiler.kie.builder.impl.InternalKieModule
import org.kie.api.runtime.rule.FactHandle

object DroolsEngine {
  def apply(drl:String): DroolsEngine = {
    new DroolsEngine("kbase1", drl, new DroolsEngineConfig())
  }
  def apply(drl:String, config: DroolsEngineConfig): DroolsEngine = {
    new DroolsEngine("kbase1", drl, config)
  }
  def apply(kbaseName:String, drl:String): DroolsEngine = {
    new DroolsEngine(kbaseName, drl, new DroolsEngineConfig())
  }
  def apply(kbaseName:String, drl:String, config: DroolsEngineConfig): DroolsEngine = {
    new DroolsEngine(kbaseName, drl, config)
  }
}

class DroolsEngine(kbaseName:String, drl: String, config: DroolsEngineConfig) extends RuntimeDrools {

  val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
  if (config.withDroolsLogging) {
    rootLogger.setLevel(ch.qos.logback.classic.Level.INFO)
  } else {
    rootLogger.setLevel(ch.qos.logback.classic.Level.ERROR)
  }

  def getCurrentTime():Long = session.getSessionClock().asInstanceOf[SessionClock].getCurrentTime
  def delete(handle: FactHandle): Unit = session.delete(handle)
  def update(handle: FactHandle, that: AnyRef): Unit = session.update(handle, that)
  def getFactHandle(arrived: Any):FactHandle = session.getFactHandle(arrived)
  def insert(that: AnyRef): FactHandle = session.insert(that)

  def insertJson(json: String): FactHandle = ???

  val ksessionName = "ksession1"
  def makeKModuleContent(config:DroolsEngineConfig): String = {
    val equalsBehavior = if (config.equalsWithIdentity) "identity" else "equality"
    val clockKind = if (config.pseudoClock) "pseudo" else ""
    s"""<kmodule xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       |         xmlns="http://www.drools.org/xsd/kmodule">
       |  <kbase name="$kbaseName" default="true" eventProcessingMode="stream" equalsBehavior="$equalsBehavior">
       |     <ksession name="$ksessionName" type="stateful" default="true" clockType="$clockKind"/>
       |  </kbase>
       |</kmodule>
       |""".stripMargin
  }

  private val logger = org.slf4j.LoggerFactory.getLogger("drools")
  logger.info(s"STARTING $kbaseName")

  private val kmoduleContent = makeKModuleContent(config)
  private val services = KieServices.Factory.get
  private val uuid = java.util.UUID.randomUUID()
  private val releaseId = services.newReleaseId( "org.sandbox", "play-with-drools-"+uuid, "1.0.0" )

  private val r1 = stringToDrlResource(drl, kbaseName+"/drl1.drl")

  private val module  = createAndDeployJar( services, kmoduleContent, releaseId, Seq(r1))
  private val container = services.newKieContainer( module.getReleaseId())
  private val session = container.newKieSession()
  services.getLoggers.newConsoleLogger(session)
  if ("""\s*global\s+org.slf4j.Logger\s*logger\s*""".r.findFirstIn(drl).isDefined) {
    session.setGlobal("logger", logger)
  }


  def timeShiftInSeconds(seconds:Int):Unit = {
    val pseudoClock = session.getSessionClock().asInstanceOf[SessionPseudoClock]
    pseudoClock.advanceTime(seconds, java.util.concurrent.TimeUnit.SECONDS)
  }

  def fireAllRules() = session.fireAllRules()
  def getObjects():Iterable[Any] = session.getObjects().asScala
  def dispose() = {
    session.dispose()
    container.dispose()
  }
  def getModelInstances(declaredType:String):Iterable[Any]={
    getObjects().filter(_.getClass().getCanonicalName == declaredType)
  }
  def getFactType(declaredType:String):Option[FactType] = {
    val Array(drlPackage, drlClassName) = declaredType.split("[.](?=[^.]*$)", 2)
    Option(session.getKieBase.getFactType(drlPackage, drlClassName))
  }
  def getModelInstanceAttribute(instance:Any, attributeName:String):Option[Object] = {
    Try {
      val declaredType = instance.getClass.getCanonicalName
      val Array(drlPackage, drlClassName) = declaredType.split("[.](?=[^.]*$)", 2)
      val factType = session.getKieBase.getFactType(drlPackage, drlClassName)
      factType.get(instance, attributeName)
    }.toOption
  }
  def getModelFirstInstance(declaredType:String):Option[Any] = getModelInstances(declaredType).headOption
  def getModelFirstInstanceAttribute(declaredType:String, attributeName:String):Option[Object] = {
    getModelFirstInstance(declaredType).flatMap(instance => getModelInstanceAttribute(instance, attributeName))
  }
  def getFields(declaredType:String):List[String] = {
    getFactType(declaredType).map { factType =>
      factType.getFields().asScala.map(_.getName()).toList
    }.getOrElse(List.empty)
  }
}