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

import nl.knaw.dans.easy.solr4files.components.DDM._
import nl.knaw.dans.easy.solr4files.{ SolrLiterals, _ }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.collection.mutable
import scala.util.Try
import scala.xml.{ Node, NodeSeq }

class DDM(xml: Node) extends DebugEnhancedLogging {

  private val profile: NodeSeq = xml \ "profile"
  private val dcmiMetadata: NodeSeq = xml \ "dcmiMetadata"

  val accessRights: String = (profile \ "accessRights").text

  // lazy postpones loading vocabularies until a file without accessibleTo=none is found
  lazy val solrLiterals: SolrLiterals =
    (dcmiMetadata \ "identifier").withFilter(isDOI).map(n => ("dataset_doi", n.text)) ++
      (profile \ "creatorDetails").map(n => ("dataset_creator", spacedText(n))) ++
      (profile \ "creator").map(n => ("dataset_creator", n.text)) ++
      (profile \ "title").map(n => ("dataset_title", n.text)) ++
      (profile \ "audience").flatMap(n => Seq(
        ("dataset_audience", n.text),
        ("dataset_subject", audienceMap.getOrElse(n.text, ""))
      )) ++
      (dcmiMetadata \ "subject").flatMap { n =>
        getAbrMap(n) match {
          case None => Seq(("dataset_subject", n.text))
          case Some(map) if map.isEmpty => Seq(("dataset_subject", n.text))
          case Some(map) => Seq(
            ("dataset_subject_abr", n.text),
            ("dataset_subject", map.getOrElse(n.text, ""))
          )
        }
      } ++
      (dcmiMetadata \ "temporal").flatMap { n =>
        getAbrMap(n) match {
          case None => Seq(("dataset_coverage_temporal", n.text))
          case Some(map) if map.isEmpty => Seq(("dataset_coverage_temporal", n.text))
          case Some(map) => Seq(
            ("dataset_coverage_temporal_abr", n.text),
            ("dataset_coverage_temporal", map.getOrElse(n.text, ""))
          )
        }
      } ++
      (dcmiMetadata \ "relation").withFilter(r => !isUrl(r) && !isStreamingSurrogate(r))
        .map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "conformsTo").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isVersionOf").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "hasVersion").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isReplacedBy").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "replaces").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isRequiredBy").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "requires").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isPartOf").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "hasPart").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isReferencedBy").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "references").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "isFormatOf").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text)) ++
      (dcmiMetadata \ "hasFormat").withFilter(!isUrl(_)).map(n => ("dataset_relation", n.text))
  // TODO spatial https://github.com/DANS-KNAW/easy-schema/blob/acb6506/src/main/assembly/dist/docs/examples/ddm/example2.xml#L280-L320
  // TODO   "dataset_coverage_spatial" -> (dcmiMetadata \ "spatial").text,
}

object DDM {

  private val xsiURI = "http://www.w3.org/2001/XMLSchema-instance"

  private val abrPrefix = "abr:ABR"

  private lazy val abrMaps = loadVocabularies("https://easy.dans.knaw.nl/schemas/vocab/2012/10/abr-type.xsd")
    .map { case (k, v) => // attributes in xsd are complex/periode
      (s"$abrPrefix$k", v) // we want to search with DDM attributes which are abr:ABRcomplex/abr:ABRperiode
    }

  private lazy val audienceMap = loadVocabularies(
    "https://easy.dans.knaw.nl/schemas/vocab/2015/narcis-type.xsd"
  )("Discipline")

  def spacedText(n: Node): String = {
    val s = mutable.ListBuffer[String]()

    def strings(n: Seq[Node]): Unit =
      n.foreach { x =>
        if (x.child.nonEmpty) strings(x.child)
        else {
          s += x.text
          strings(x.child)
        }
      }

    strings(n)
    s.mkString(" ")
  }

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

  private def isStreamingSurrogate(relation: Node) = {
    relation
      .attribute("scheme")
      .map(_.text)
      .contains("STREAMING_SURROGATE_RELATION")
  }

  private def isUrl(relation: Node) = {
    Try(new URL(relation.text)).isSuccess
  }

  private def loadVocabularies(xsdURL: String): Map[String, Map[String, String]] = {
    for {
      url <- Try(new URL(xsdURL))
      xml <- url.loadXml
    } yield (xml \ "simpleType")
      .map(n => (n.attribute("name").map(_.text).getOrElse(""), findKeyValuePairs(n)))
      .toMap
  }.getOrElse(Map.empty)

  private def findKeyValuePairs(table: Node): Map[String, String] = {
    (table \\ "enumeration")
      .map { node =>
        val key = node.attribute("value").map(_.text).getOrElse("")
        val value = (node \ "annotation" \ "documentation").text
        key -> value.replaceAll("\\s+", " ").trim
      }.toMap
  }
}
