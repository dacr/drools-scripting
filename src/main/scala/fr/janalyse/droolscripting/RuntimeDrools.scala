package fr.janalyse.droolscripting

import org.kie.api._
import org.kie.api.runtime.KieSession
import org.kie.api.builder._
import org.kie.api.io._
import org.kie.api.time.SessionPseudoClock
import org.kie.internal.io.ResourceFactory
import org.kie.api.definition.`type`.FactType

import org.drools.compiler.kie.builder.impl.InternalKieModule

trait RuntimeDrools {
  /* Inspired from the following test code :
    https://github.com/kiegroup/drools/blob/master/drools-compiler/src/test/java/org/drools/compiler/integrationtests/KieBuilderTest.java
   */
  def createJar(ks: KieServices, kModuleContent:String, releaseId: ReleaseId, drlResources: Seq[Resource]):Array[Byte] = {
    val kfs = ks.newKieFileSystem.generateAndWritePomXML(releaseId).writeKModuleXML(kModuleContent)
    for { resource <- drlResources } { kfs.write(resource) }
    val kb = ks.newKieBuilder(kfs).buildAll
    assert(!kb.getResults.hasMessages(Message.Level.ERROR), kb.getResults.getMessages(Message.Level.ERROR).toString)
    ks.getRepository.getKieModule(releaseId).asInstanceOf[InternalKieModule].getBytes
  }
  def deployJarIntoRepository(ks: KieServices, jar: Array[Byte]):KieModule = {
    val jarRes = ks.getResources.newByteArrayResource(jar)
    ks.getRepository.addKieModule(jarRes)
  }
  def createAndDeployJar(ks: KieServices, kModuleContent:String, releaseId: ReleaseId, drls: Seq[Resource]):KieModule = {
    val jar = createJar(ks, kModuleContent, releaseId, drls)
    deployJarIntoRepository(ks, jar)
  }
  def stringToDrlResource(drlContent:String, sourcePath:String):Resource = {
    ResourceFactory.newByteArrayResource(drlContent.getBytes).setResourceType(ResourceType.DRL).setSourcePath(sourcePath)
  }
}