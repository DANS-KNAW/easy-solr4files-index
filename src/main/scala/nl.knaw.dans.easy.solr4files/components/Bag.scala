/*
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
import java.util.UUID

import nl.knaw.dans.easy.solr4files.{ FileToShaMap, _ }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import scalaj.http.BaseHttp

import scala.util.Try
import scala.util.matching.Regex
import scala.xml.Elem

case class Bag(storeName: String,
               bagId: UUID,
               private val vault: Vault
              )(implicit http: BaseHttp) extends DebugEnhancedLogging {

  def fileUrl(path: String): Try[URL] = {
    vault.fileURL(storeName, bagId, path)
  }

  def fileSize(path: String): Long = {
    vault.getSize(storeName, bagId, path)
  }

  // splits a string on the first sequence of white space after the sha
  // the rest is a path that might contain white space
  private lazy val regex: Regex =
  """(\w+)\s+(.*)""".r()

  private lazy val fileShas: FileToShaMap = {
    // gov.loc.repository.bagit.reader.ManifestReader reads files, we need URL or stream
    for {
      url <- vault.fileURL(storeName, bagId, "manifest-sha1.txt")
      lines <- url.readLines()
    } yield lines.map { line: String =>
      val regex(sha, path) = line.trim
      (path, sha)
    }.toMap
  }.getOrElse(Map.empty)

  def sha(path: String): String = {
    fileShas.getOrElse(path, "")
  }

  def loadDDM(connTimeoutMs: Int = defaultConnTimeout, readTimeoutMs: Int = defaultReadTimeout): Try[Elem] = vault
    .fileURL(storeName, bagId, "metadata/dataset.xml")
    .flatMap(_.loadXml(connTimeoutMs, readTimeoutMs))

  def loadFilesXML(connTimeoutMs: Int = defaultConnTimeout, readTimeoutMs: Int = defaultReadTimeout): Try[Elem] = vault
    .fileURL(storeName, bagId, "metadata/files.xml")
    .flatMap(_.loadXml(connTimeoutMs, readTimeoutMs))
}
