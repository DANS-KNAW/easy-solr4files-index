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

import nl.knaw.dans.easy.solr4files.components.Vault
import nl.knaw.dans.lib.error.TraversableTryExtensions
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }
import scala.xml.Elem

class EasyUpdateSolr4filesIndexApp(wiring: ApplicationWiring) extends AutoCloseable
  with Vault
  with DebugEnhancedLogging {

  def initAll(stores: URI): Try[String] = {
    getStores(stores)
      .flatMap(_.map(init).collectResults)
      .map(_ => s"Updated all bags of all stores ($stores)")
  }

  def init(bags: URI): Try[String] = {
    logger.info(s"Updating bags of one store ($bags)")
    getBags(bags)
      .flatMap(_.map(update).collectResults)
      .map(_ => s"Updated bags of one store ($bags)")
  }

  def update(baseUri: URI): Try[String] = {
    for {
      filesXML: Elem <- getFilesXml(baseUri)
      files <- textFiles(filesXML)
    } yield s"Updated $baseUri"
  }

  def delete(baseUrl: URI): Try[String] =
    Failure(new NotImplementedError(s"delete not implemented ($baseUrl)"))

  def init(): Try[Unit] = {
    // Do any initialization of the application here. Typical examples are opening
    // databases or connecting to other services.
    Success(())
  }

  override def close(): Unit = {

  }
}