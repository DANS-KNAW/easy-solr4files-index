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

class FileItem(bag: Bag, ddm: DDM, xml: Node) {

  // see ddm.xsd EasyAccessCategoryType
  private def datasetAccessitbleTo = ddm.accessRights match {
    // @formatter:off
    case "OPEN_ACCESS"                      => "ANONYMOUS"
    case "OPEN_ACCESS_FOR_REGISTERED_USERS" => "KNOWN"
    case "GROUP_ACCESS"                     => "RESTRICTED_GROUP"
    case "REQUEST_PERMISSION"               => "RESTRICTED_REQUEST"
    case "NO_ACCESS"                        => "NONE"
    case _                                  => "NONE"
    // @formatter:off
  }
  private val accessRights: String = Option( xml \ "accessRights")
    .map(_.text.trim)
    .getOrElse( datasetAccessitbleTo )
  val path: String = xml.attribute("filepath")
    .map(_.text.trim)
    .getOrElse("")
  val url: URL = bag.fileUrl(path)
  val mimeType: String = (xml \ "format").text
  val isImage: Boolean = mimeType.startsWith("video") || mimeType.startsWith("image")
  val skip: Boolean = isImage || path.isEmpty
  val solrLiterals: SolrLiterals = Seq(
    ("file_path", path),
    ("file_checksum", bag.sha(path)),
    ("file_mime_type", mimeType),
    ("file_accessible_to", accessRights)
  )
}
