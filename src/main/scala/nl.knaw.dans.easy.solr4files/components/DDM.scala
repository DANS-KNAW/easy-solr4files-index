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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.xml.{ Elem, Node }

class DDM(xml: Elem) extends DebugEnhancedLogging {
  val accessRights: String = (xml \ "profile" \ "accessRights").text
  val title: String = (xml \ "profile" \ "title").text
  val creator: String = (xml \ "profile" \ "creator").text // TODO formatting?
  val audience: String = (xml \ "profile" \ "audience").text // TODO translate
  val doi: String = (xml \ "dcmiMetadata" \ "identifier").filter(isDOI).text
  val relation: String = (xml \ "dcmiMetadata" \ "relation").text

  private def isDOI(n: Node) = (n \@ "type") == "id-type:DOI"
}
