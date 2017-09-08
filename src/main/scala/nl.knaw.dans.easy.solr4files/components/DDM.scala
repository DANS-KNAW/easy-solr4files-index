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
import nl.knaw.dans.easy.solr4files.components.DDM._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.xml.{ Node, NodeSeq, XML }

class DDM(xml: Node) extends DebugEnhancedLogging {

  private val profile: NodeSeq = xml \ "profile"
  private val dcmiMetadata: NodeSeq = xml \ "dcmiMetadata"

  val accessRights: String = (profile \ "accessRights").text
  val solrLiterals: SolrLiterals =
    (dcmiMetadata \ "identifier").withFilter(isDOI).map(n => ("dataset_doi", n.text)) ++
      (profile \ "creatorDetails").map(n => ("dataset_creator", n.text)) ++
      (profile \ "creator").map(n => ("dataset_creator", n.text)) ++
      (profile \ "title").map(n => ("dataset_title", n.text)) ++
      (profile \ "audience").flatMap(n => Seq(
        ("dataset_narcis_audience", n.text), // TODO https://github.com/DANS-KNAW/easy-dtap/pull/131#pullrequestreview-60582037
        ("dataset_audience", audienceMap.getOrElse(n.text, ""))
      )) ++
      (dcmiMetadata \ "subject").flatMap { n =>
        getAbrMap(n) match {
          case map if map.isEmpty => Seq(("dataset_subject", n.text))
          case abrMap => Seq(
            ("dataset_abr_subject", n.text), // TODO https://github.com/DANS-KNAW/easy-dtap/pull/131#pullrequestreview-60582037
            ("dataset_subject", abrMap.get.getOrElse(n.text, ""))
          )
        }
      } ++
      (dcmiMetadata \ "relation").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "conformsTo").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isVersionOf").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "hasVersion").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isReplacedBy").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "replaces").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isRequiredBy").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "requires").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isPartOf").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "hasPart").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isReferencedBy").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "references").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isFormatOf").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "hasFormat").map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "temporal").map(n => ("dataset_coverage", n.text))
  // TODO spatial https://github.com/DANS-KNAW/easy-schema/blob/acb6506/src/main/assembly/dist/docs/examples/ddm/example2.xml#L280-L320
  // TODO   "dataset_coverage" -> (dcmiMetadata \ "spatial").text,
}

object DDM {

  private val xsiURI = "http://www.w3.org/2001/XMLSchema-instance"

  private val abrPrefix = "abr:ABR"

  private def getAbrMap(ddmSubjectNode: Node): Option[Map[String, String]] = {
    ddmSubjectNode
      .attribute(xsiURI, "type")
      .map(_.text)
      .filter(_.startsWith(abrPrefix))
      .flatMap(abrMaps.get)
  }

  private def isDOI(identifierNode: Node) = {
    identifierNode
      .attribute(xsiURI, "type")
      .map(_.text)
      .contains("id-type:DOI")
  }

  private val abrMaps = loadVocabularies("https://easy.dans.knaw.nl/schemas/vocab/2012/10/abr-type.xsd")
    .map { case (k, v) => // attributes in xsd are complex/periode
      (s"$abrPrefix$k", v) // we want to search with DDM attributes which are abr:ABRcomplex/abr:ABRperiode
    }

  private val audienceMap = loadVocabularies(
    "https://easy.dans.knaw.nl/schemas/vocab/2015/narcis-type.xsd"
  )("Discipline")

  private def loadVocabularies(xsdURL: String): Map[String, Map[String, String]] = {
    val xmlDoc = resource.managed(
      new URL(xsdURL).openStream()
    ).acquireAndGet(XML.load) // TODO error handling
    (xmlDoc \ "simpleType")
      .map(n => (n.attribute("name").head.text, findKeyValuePairs(n)))
      .toMap
  }

  private def findKeyValuePairs(table: Node): Map[String, String] = {
    (table \\ "enumeration")
      .map { node =>
        val key = node.attribute("value").map(_.text).getOrElse("")
        val value = (node \ "annotation" \ "documentation").text
        key -> value.replaceAll("\\s+", " ").trim
      }.toMap
  }
}
