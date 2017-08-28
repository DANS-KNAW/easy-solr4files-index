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

import java.net.{ URI, URL }
import java.nio.file.Paths

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

trait Vault {
  this: VaultIO with DebugEnhancedLogging =>

  def getStoreNames: Try[Seq[String]] = Try {
    val uri = vaultBaseUri.resolve("stores")
    logger.info(s"getting storeNames with $uri")
    linesFrom(uri).map { line =>
      val trimmed = line.trim.replace("<", "").replace(">", "")
      Paths.get(new URI(trimmed).getPath).getFileName.toString
    }
  }

  def getBagIds(storeName: String): Try[Seq[String]] = Try {
    val storeURI = vaultBaseUri.resolve(s"stores/$storeName/bags")
    logger.info(s"getting bag ids with $storeURI")
    linesFrom(storeURI).map { _.trim }
  }
}
