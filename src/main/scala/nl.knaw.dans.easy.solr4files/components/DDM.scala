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

import nl.knaw.dans.easy.solr4files.SolrLiterals
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.xml.{ Elem, Node }

class DDM(xml: Elem) extends DebugEnhancedLogging {

  private def isDOI(n: Node) = (n \@ "type") == "id-type:DOI"

  // TODO translate audience to human readable value, perhaps add both values in different fields?
  // https://github.com/DANS-KNAW/easy-schema/blob/master/src/main/assembly/dist/vocab/2015/narcis-type.xsd

  val accessRights: String = (xml \ "profile" \ "accessRights").text
  val solrLiterals: SolrLiterals = Seq(
    "dataset_doi" -> (xml \ "dcmiMetadata" \ "identifier").filter(isDOI).text,
    // TODO multiple occurrences for the next items, see also easy-update-solr-index
    "dataset_title" -> (xml \ "profile" \ "title").text,
    "dataset_creator" -> (xml \ "profile" \ "creator").text,
    "dataset_audience" -> (xml \ "profile" \ "audience").text,
//    "dataset_subject" -> (xml \ "profile" \ "audience").text,
//    "dataset_coverage" -> (xml \ "profile" \ "audience").text,
    "dataset_relation" -> (xml \ "dcmiMetadata" \ "relation").text
  )
}
