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
package nl.knaw.dans.easy.solr4files.components

import java.io.File
import java.net.URI
import java.nio.file.Paths

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Success, Try }
import scalaj.http.Http

trait Vault extends DebugEnhancedLogging {
  this: VaultIO =>

  def getStoreNames: Try[Seq[String]] = Try {
    val uri = vaultBaseUri.resolve("stores")
    logger.info(s"getting storeNames with $uri")
    linesFrom(uri).map { line =>
      // the Vault returns localhost, we need the configured host
      val trimmed = line.trim.replace("<", "").replace(">", "")
      Paths.get(new URI(trimmed).getPath).getFileName.toString
    }
  }

  def getBagIds(storeName: String): Try[Seq[String]] = Try {
    val storeURI = vaultBaseUri.resolve(s"stores/$storeName/bags")
    logger.info(s"getting bag ids with $storeURI")
    linesFrom(storeURI).map { _.trim }
  }

  def getSize(storeName: String, bagId: String, path: String): Long = {
    val url = fileURL(storeName, bagId, path)

    if (url.getProtocol.toLowerCase == "file")
      new File(url.getPath).length
    else Http(url.toString).method("HEAD").asString match {
      case response if !response.isSuccess =>
        logger.warn(s"getSize($url) ${ response.statusLine }, details: ${ response.body }")
        -1L
      case response =>
        Try(response.headers("content-length").toLong).recoverWith {
          case e => logger.warn(s"getSize($url) content-length: ${ e.getMessage }", e)
            Success(-1L)
        }.getOrElse(-1L)
    }
  }
}
