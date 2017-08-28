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

import java.net.{ URI, URL }

import nl.knaw.dans.easy.solr4files.components._
import nl.knaw.dans.lib.error.TraversableTryExtensions
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

/**
 * Initializes and wires together the components of this application.
 *
 * @param configuration the application configuration
 */
class ApplicationWiring(configuration: Configuration)
  extends DebugEnhancedLogging with VaultIO with Vault with Solr {

  // don't need resolve for solr, URL gives more early errors TODO perhaps not enough early errors
  override val solrUrl: URL = new URL(configuration.properties.getString("solr.url", ""))
  override val vaultBaseUri: URI = new URI(configuration.properties.getString("vault.url", ""))

  def initAllStores(): Try[String] = {
    getStoreNames
      .flatMap(_.map(initSingleStore).collectResults)
      .map(results => s"Updated all bags of ${ results.size } stores ($vaultBaseUri)")
  }

  def initSingleStore(storeName: String): Try[String] = {
    getBagIds(storeName)
      .flatMap(_.map(uuid => update(storeName, uuid)).collectResults)
      .map(results => s"Updated ${ results.size } bags of one store ($storeName)")
  }

  def update(storeName: String, bagId: String): Try[String] = {
    val bag = Bag(storeName, bagId, this)
    val fileBaseURI = vaultBaseUri.resolve(s"stores/$storeName/bags/$bagId/")
    for {
      ddmXML <- bag.loadDDM
      ddm = new DDM(ddmXML)
      shas <- bag.getFileShas
      filesXML <- bag.loadFilesXML
      files = new FileItems(filesXML, shas, fileBaseURI).openAccessTextFiles()
      _ <- files.map(fileItem => createDoc(bag, ddm, fileItem)).collectResults
      _ = println(s"Found text files: ${ files.map(_.path).mkString(", ") }")
    } yield s"Updated $storeName $bagId (${ files.size } files)"
  }

  def delete(bagId: String): Try[String] = {
    deleteBag(bagId)
  }
}
