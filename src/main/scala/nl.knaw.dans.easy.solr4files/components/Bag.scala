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

import java.net.URL

import nl.knaw.dans.easy.solr4files.{ FileToShaMap, SolrLiterals }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try
import scala.xml.Elem

case class Bag(storeName: String,
               bagId: String,
               private val vaultIO: VaultIO
              ) extends DebugEnhancedLogging {

  private def getDepositor: String = Try {
    val key = "EASY-User-Account"
    vaultIO.linesFrom(storeName, bagId, "bag-info.txt")
      .filter(_.trim.startsWith(key))
      .map(_.trim.replace(key, "").trim.replace(":", "").trim)
      .head
  }.getOrElse("unknown")

  def fileUrl(path: String): URL = {
    vaultIO.fileURL(storeName, bagId, path)
  }

  private val fileShas: FileToShaMap = Try {
    vaultIO.linesFrom(storeName, bagId, "manifest-sha1.txt").map { line: String =>
      val Array(sha, path) = line.trim.split("""\s+""")
      (path, sha)
    }.toMap
  }.getOrElse(FileToShaMap())

  def sha(path: String): String = {
    fileShas.getOrElse(path, "")
  }

  val solrLiterals: SolrLiterals = Seq(
    ("dataset_depositor_id", getDepositor),
    ("dataset_id", bagId)
  )

  def loadDDM: Try[Elem] = vaultIO.loadXml(storeName, bagId, "metadata/dataset.xml")

  def loadFilesXML: Try[Elem] = vaultIO.loadXml(storeName, bagId, "metadata/files.xml")
}
