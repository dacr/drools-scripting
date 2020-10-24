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

import com.owlike.genson.GensonBuilder
import org.kie.api.runtime.rule.FactHandle

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

/**
 * Drools engine abstraction
 * @param kbaseName knowledge base name (what ever you want)
 * @param drl knowledge base content (rules, declarations, ...)
 * @param config drools engine configuration
 */

class DroolsEngine(kbaseName: String, drl: String, config: DroolsEngineConfig) extends RuntimeDrools {
  private val logger = org.slf4j.LoggerFactory.getLogger("DroolsEngine")

  val dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
  val dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormatPattern)
  val converters = Array(
    OffsetDateTimeConverter(dateTimeFormatter),
    ZonedDateTimeConverter(dateTimeFormatter),
    LocalDateTimeConverter(dateTimeFormatter),
    DateConverter(dateTimeFormatter)
  )
  private val genson =
    new GensonBuilder()
      .setSkipNull(true)
      .withConverters(converters:_*)
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

  def advanceTime(seconds: Int, timeUnit: TimeUnit = TimeUnit.SECONDS): Unit = {
    if (config.eventProcessingMode == StreamMode) {
      if (config.pseudoClock) {
        val pseudoClock = session.getSessionClock.asInstanceOf[SessionPseudoClock]
        pseudoClock.advanceTime(seconds, timeUnit)
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

  def delete(handle: FactHandle): Unit = session.delete(handle)

  def update(handle: FactHandle, that: AnyRef): Unit = session.update(handle, that)

  def insert(that: AnyRef): FactHandle = session.insert(that)

  def insertJson(json: String, typeInfo: String): FactHandle = {
    val cl = container.getClassLoader
    val clazz = cl.loadClass(typeInfo)
    val result = genson.deserialize(json, clazz).asInstanceOf[Object]
    insert(result)
  }

  def fireAllRules(): Int = session.fireAllRules()

  def fireUntilHalt(): Unit = session.fireUntilHalt()

  def getObjects: Iterable[Any] = session.getObjects().asScala

  def getModelInstances(declaredType: String): Iterable[Any] = {
    val declaredTypeClass = container.getClassLoader.loadClass(declaredType)
    getObjects.filter(ob => declaredTypeClass.isAssignableFrom(ob.getClass))
  }

  def getModelInstanceAttribute(instance: Any, attributeName: String): Option[Object] = {
    Try {
      val declaredType = instance.getClass.getCanonicalName
      val Array(drlPackage, drlClassName) = declaredType.split("[.](?=[^.]*$)", 2)
      val factType = session.getKieBase.getFactType(drlPackage, drlClassName)
      factType.get(instance, attributeName)
    }.toOption
  }

  def getModelFirstInstance(declaredType: String): Option[Any] = {
    getModelInstances(declaredType).headOption
  }

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