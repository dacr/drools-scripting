/*
 * Copyright 2020 David Crosson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.janalyse.droolscripting

import java.io.File
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

import org.slf4j._

import scala.jdk.CollectionConverters._
import scala.util.Try
import org.kie.api._
import org.kie.api.runtime.KieSession
import org.kie.api.time.{SessionClock, SessionPseudoClock}
import org.kie.api.definition.`type`.FactType
import java.util.concurrent.TimeUnit

import com.owlike.genson.{Converter, GensonBuilder}
import org.kie.api.runtime.rule.FactHandle


/**
 * Drools engine abstraction
 * @param kbaseName knowledge base name (what ever you want)
 * @param drl knowledge base content (rules, declarations, ...)
 * @param config drools engine configuration
 */

class DroolsEngine(kbaseName: String, drl: String, config: DroolsEngineConfig) extends RuntimeDrools {
  private val logger = org.slf4j.LoggerFactory.getLogger("DroolsEngine")

  private val genson =
    new GensonBuilder()
      .setSkipNull(true)
      .withConverters(
        OffsetDateTimeConverter(),
        ZonedDateTimeConverter(),
        LocalDateTimeConverter(),
        DateConverter()
      )
      .useClassMetadata(true)
      .useConstructorWithArguments(false) // Take care, with true you may encounter IllegalArgumentException within asm.ClassReader
      .create()

  LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) match {
    case rootLogger: ch.qos.logback.classic.Logger if config.withDroolsLogging =>
      rootLogger.setLevel(ch.qos.logback.classic.Level.INFO)
    case rootLogger: ch.qos.logback.classic.Logger =>
      rootLogger.setLevel(ch.qos.logback.classic.Level.ERROR)
    case rootLogger =>
      logger.warn(s"Couldn't automically configure log levels for ${rootLogger.getClass.getCanonicalName} logger")
  }

  private def makeKModuleContent(config: DroolsEngineConfig): String = {
    val equalsBehavior = if (config.equalsWithIdentity) "identity" else "equality"
    val eventProcessingMode = config.eventProcessingMode match {
      case StreamMode => "stream"
      case CloudMode => "cloud"
    }
    val sessionName = config.ksessionName
    val clockKind = if (config.pseudoClock) "pseudo" else "realtime"
    s"""<kmodule xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       |         xmlns="http://www.drools.org/xsd/kmodule">
       |  <kbase name="$kbaseName"
       |         default="true"
       |         eventProcessingMode="$eventProcessingMode"
       |         equalsBehavior="$equalsBehavior">
       |     <ksession name="$sessionName"
       |               type="stateful"
       |               default="true"
       |               clockType="$clockKind"/>
       |  </kbase>
       |</kmodule>
       |""".stripMargin
  }


  val services: KieServices = KieServices.Factory.get

  private val module = {
    val kmoduleContent = makeKModuleContent(config)
    val uuid = java.util.UUID.randomUUID()
    val releaseId = services.newReleaseId(
      "fr.janalyse",
      "playing-with-drools-" + uuid,
      "1.0.0")
    val r1 = stringToDrlResource(drl, kbaseName + "/drl1.drl")
    createAndDeployJar(services, kmoduleContent, releaseId, Seq(r1))
  }

  private val container = services.newKieContainer(module.getReleaseId)

  val session: KieSession = container.newKieSession()

  services.getLoggers.newConsoleLogger(session)
  if ("""\s*global\s+org.slf4j.Logger\s*logger\s*""".r.findFirstIn(drl).isDefined) {
    session.setGlobal("logger", logger)
  }

  // ===========================================================================

  def dispose(): Unit = {
    session.dispose()
    container.dispose()
  }

  def getFactHandle(arrived: Any): FactHandle = session.getFactHandle(arrived)

  def getFactType(declaredType: String): Option[FactType] = {
    val Array(drlPackage, drlClassName) = declaredType.split("[.](?=[^.]*$)", 2)
    Option(session.getKieBase.getFactType(drlPackage, drlClassName))
  }

  def getFields(declaredType: String): List[String] = {
    getFactType(declaredType).map { factType =>
      factType.getFields.asScala.map(_.getName()).toList
    }.getOrElse(List.empty)
  }

  def getCurrentTime: Long = {
    if (config.pseudoClock) {
      session.getSessionClock.asInstanceOf[SessionPseudoClock].getCurrentTime
    } else {
      session.getSessionClock.asInstanceOf[SessionClock].getCurrentTime
    }
  }

  def timeShiftInSeconds(seconds: Int): Unit = advanceTime(seconds, TimeUnit.SECONDS)

  def advanceTimeMillis(millis: Int): Unit = advanceTime(millis, TimeUnit.MILLISECONDS)
  def advanceTimeSeconds(seconds: Int): Unit = advanceTime(seconds, TimeUnit.SECONDS)
  def advanceTimeMinutes(minutes: Int): Unit = advanceTime(minutes, TimeUnit.MINUTES)
  def advanceTimeHours(hours: Int): Unit = advanceTime(hours, TimeUnit.HOURS)
  def advanceTimeDays(days: Int): Unit = advanceTime(days, TimeUnit.DAYS)

  /**
   * Manipulate the time, if a pseudo clock has been configured.
   * This method is very important for unit test purposes.
   *
   * @param duration how much time shall will go in future
   * @param timeUnit the time unit, default is seconds
   */
  def advanceTime(duration: Int, timeUnit: TimeUnit = TimeUnit.SECONDS): Unit = {
    if (config.eventProcessingMode == StreamMode) {
      if (config.pseudoClock) {
        val pseudoClock = session.getSessionClock.asInstanceOf[SessionPseudoClock]
        pseudoClock.advanceTime(duration, timeUnit)
      } else {
        val msg = "time clock adjustements can only work with pseudo clock, check your configuration"
        logger.warn(msg)
        throw new DroolsEngineException(msg)
      }
    } else {
      val msg = "time clock adjustements can only work in stream mode, check your eventProcessingMode configuration"
      logger.warn(msg)
      throw new DroolsEngineException(msg)
    }
  }

  /**
   * delete a fact
   *
   * @param handle
   */
  def delete(handle: FactHandle): Unit = session.delete(handle)

  /**
   * update a fact
   *
   * @param handle
   * @param that
   */
  def update(handle: FactHandle, that: AnyRef): Unit = session.update(handle, that)

  /**
   * insert a raw object into drools, it can be anything
   *
   * @param that
   * @return
   */
  def insert(that: AnyRef): FactHandle = session.insert(that)

  /**
   * Insert a fact described in json, the given typeInfo will be use to find the right declaration
   *
   * @param json the fact to insert
   * @param typeInfo the type of the object we will create
   * @return internal drools fact handle
   */
  def insertJson(json: String, typeInfo: String): FactHandle = {
    val cl = container.getClassLoader
    val clazz = cl.loadClass(typeInfo)
    val result = genson.deserialize(json, clazz).asInstanceOf[Object]
    insert(result)
  }

  /**
   * Makes the drools engine fire all activable rules, it will stop once no more rules are activable
   *
   * @return number of rule fired
   */

  def fireAllRules(): Int = session.fireAllRules()

  /**
   * Enter a forever fireAll loop
   */
  def fireUntilHalt(): Unit = session.fireUntilHalt()

  /**
   * Retreive all available fact from drools working memory
   *
   * @return objects iterable
   */
  def getObjects: Iterable[Any] = session.getObjects().asScala

  /**
   * Retreive as json all available fact from drools working memory
   *
   * @return json strings iterable
   */
  def getObjectsAsJson:Iterable[String] = session.getObjects().asScala.map(genson.serialize)

  /**
   * Get all facts which have the given type or inheritate from it
   *
   * @param declaredType the full type information of the facts we want to extract
   * @return objects iterable
   */
  def getModelInstances(declaredType: String): Iterable[Any] = {
    val declaredTypeClass = container.getClassLoader.loadClass(declaredType)
    getObjects.filter(ob => declaredTypeClass.isAssignableFrom(ob.getClass))
  }

  /**
   * Get all facts which have the given type or inheritate from it
   *
   * @param declaredType the full type information of the facts we want to extract
   * @return json strings iterable
   */
  def getModelInstancesAsJson(declaredType:String): Iterable[String] = {
    getModelInstances(declaredType).map(genson.serialize)
  }

  /**
   * Retrieve a field value from a drools facts previously extracted from the working memory
   *
   * @param instance
   * @param attributeName
   * @return
   */
  def getModelInstanceAttribute(instance: Any, attributeName: String): Option[Object] = {
    Try {
      val declaredType = instance.getClass.getCanonicalName
      val Array(drlPackage, drlClassName) = declaredType.split("[.](?=[^.]*$)", 2)
      val factType = session.getKieBase.getFactType(drlPackage, drlClassName)
      factType.get(instance, attributeName)
    }.toOption
  }

  /**
   * Convenient method to quickly get the first available instance of the given type
   *
   * @param declaredType
   * @return object option
   */
  def getModelFirstInstance(declaredType: String): Option[Any] = {
    getModelInstances(declaredType).headOption
  }

  /**
   * Convenient method to quickly get the first available json instance of the given type
   *
   * @param declaredType
   * @return json string option
   */
  def getModelFirstInstanceAsJson(declaredType: String): Option[String] = {
    getModelFirstInstance(declaredType).map(genson.serialize)
  }

  /**
   * Convenient method to extract a field value from the first found instance of the given type
   * Of course, really convenient if only have just one instance of the given type
   *
   * @param declaredType
   * @param attributeName
   * @return value
   */
  def getModelFirstInstanceAttribute(declaredType: String, attributeName: String): Option[Object] = {
    getModelFirstInstance(declaredType).flatMap { instance =>
      getModelInstanceAttribute(instance, attributeName)
    }
  }

  /**
   * A convenient method to quickly extract all string instance from drools working
   * memory. Using such strings is quite helpful for testing purposes.
   * @return working memory String instances
   */
  def strings:List[String] = {
    getModelInstances("java.lang.String").toList.collect {
      case str:String => str
    }
  }
}


/**
 * DroolsEngine factories
 */
object DroolsEngine {
  def apply(drlFile: File): DroolsEngine = {
    apply(drlFile, new DroolsEngineConfig())
  }
  def apply(drlFile: File, config:DroolsEngineConfig): DroolsEngine = {
    val drl = scala.io.Source.fromFile(drlFile).getLines().mkString("\n")
    new DroolsEngine("kbase1", drl, config)
  }

  def apply(drl: String): DroolsEngine = {
    new DroolsEngine("kbase1", drl, new DroolsEngineConfig())
  }

  def apply(drl: String, config: DroolsEngineConfig): DroolsEngine = {
    new DroolsEngine("kbase1", drl, config)
  }

  def apply(kbaseName: String, drl: String): DroolsEngine = {
    new DroolsEngine(kbaseName, drl, new DroolsEngineConfig())
  }

  def apply(kbaseName: String, drl: String, config: DroolsEngineConfig): DroolsEngine = {
    new DroolsEngine(kbaseName, drl, config)
  }
}
