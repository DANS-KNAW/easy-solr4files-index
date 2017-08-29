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

import nl.knaw.dans.easy.solr4files.SolrLiterals

import scala.xml.Node

class FileItem(sha: String, fileURL: URL, xml: Node) {

  val mimeType: String = (xml \ "format").text
  val url: URL = fileURL
  val path: String = xml.attribute("filepath").map(_.text).getOrElse("")
  val solrLiterals: SolrLiterals = Seq(
    ("file_path", path),
    ("file_checksum", sha),
    ("file_mime_type", mimeType)
  )
}
