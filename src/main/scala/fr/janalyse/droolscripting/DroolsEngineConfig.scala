/*
 * Copyright 2020-2023 David Crosson
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

sealed trait EventProcessingMode
object StreamMode extends EventProcessingMode
object CloudMode extends EventProcessingMode

object DroolsEngineConfig {
  val configWithIdentity = DroolsEngineConfig(equalsWithIdentity = true)
  val configWithEquality = DroolsEngineConfig()
}
case class DroolsEngineConfig(
  equalsWithIdentity:Boolean=false,
  pseudoClock:Boolean=true,
  withDroolsLogging:Boolean = false,
  eventProcessingMode:EventProcessingMode = StreamMode
) {
  val ksessionName = "ksession1"
}
