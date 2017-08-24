/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr4files

import java.net.URI

import nl.knaw.dans.easy.solr4files.components.{ Bag, Files, Vault }
import nl.knaw.dans.lib.error.TraversableTryExtensions
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Try }
import scala.xml.Elem

/**
 * Initializes and wires together the components of this application.
 *
 * @param configuration the application configuration
 */
class ApplicationWiring(configuration: Configuration) extends DebugEnhancedLogging
  with Vault {
  override val vaultBaseUri: URI = new URI(configuration.properties.getString("vault.url", ""))

  def initAllStores(): Try[String] = {
    getStoreNames
      .flatMap(_.map(initSingleStore).collectResults)
      .map(_ => s"Updated all bags of all stores ($vaultBaseUri)")
  }

  // internally called methods are final, as overriding would implicitly alter behaviour of the caller

  final def initSingleStore(storeName: String): Try[String] = {
    getBagIds(storeName)
      .flatMap(_.map(uuid => update(storeName, uuid)).collectResults)
      .map(_ => s"Updated bags of one $storeName")
  }

  final def update(storeName1: String, bagId1: String): Try[String] = {
    val me = this
    val bag = new Bag with Vault {
      override val storeName: String = storeName1
      override val bagId: String = bagId1
      override val vaultBaseUri: URI = me.vaultBaseUri
    }
    for {
      filesXML: Elem <- bag.loadFilesXML
      ddmXML: Elem <- bag.loadDDM
      shas <- bag.getFileShas
      files = new Files(filesXML, shas).openTextFiles()
      _ = println(s"Found text files: ${ files.mkString(", ") }")
    } yield s"Updated $storeName1 $bagId1 (${files.size} files)"
  }

  def delete(storeName: String, bagId: String): Try[String] =
    Failure(new NotImplementedError(s"delete not implemented ($storeName, $bagId)"))
}
