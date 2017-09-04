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

import scala.xml.{Elem, Node, XML}

class DDM(xml: Elem) extends DebugEnhancedLogging {

  private def isDOI(n: Node) = (n \@ "type") == "id-type:DOI"

  val accessRights: String = (xml \ "profile" \ "accessRights").text
  val solrLiterals: SolrLiterals = Seq(
    "dataset_doi" -> (xml \ "dcmiMetadata" \ "identifier").filter(isDOI).text,

    // TODO add white space in case of one-line input, is the order fixed?
    "dataset_creator" -> (xml \ "profile" \ "creatorDetails").head.text.replaceAll("\\s+"," ").trim
  ) ++
    (xml \ "profile" \ "title").map(n => ("dataset_title",n.text))++
    (xml \ "profile" \ "audience").flatMap(n => codeAndText("dataset_audience", n.text, audienceMap))++
    (xml \ "dcmiMetadata" \ "subject").flatMap(n =>
      if (isABR(n)) Seq(("dataset_subject", n.text))
      else codeAndText("dataset_subject", n.text, abrMap)
    ) ++
    (xml \ "profile" \ "relation").map(n => ("dataset_relation",n.text))
  // TODO   "dataset_coverage" -> (xml \ "dcmiMetadata" \ "temporal").text,
  // TODO   "dataset_coverage" -> (xml \ "dcmiMetadata" \ "spatial").text,
}

object DDM {

  private def codeAndText(field: String, key: String, keyValues: Map[String, String]): Seq[(String, String)] = {
    Seq(
      (field, key),
      (field, keyValues.getOrElse(key, ""))
    )
  }

  private def isABR(n: Node) = {
    n.attribute("type").map(_.text).mkString.startsWith("abr")
  }

  val audienceMap: Map[String,String] = loadVocabulary("https://easy.dans.knaw.nl/schemas/vocab/2015/narcis-type.xsd")

  // TODO currently assuming ABR-complex and ABR-period are disjunct
  val abrMap: Map[String,String] = loadVocabulary("https://easy.dans.knaw.nl/schemas/vocab/2012/10/abr-type.xsd")

  private def loadVocabulary(xsd: String): Map[String, String] = {
    (resource.managed(
      new URL(xsd).openStream() // TODO error handling
    ).acquireAndGet(XML.load) \\ "enumeration")
      .map { node =>
        val key: String = node.attribute("value").map(_.text).getOrElse("")
        val value = (node \ "annotation" \ "documentation").text
        key -> value.replaceAll("\\s+", " ").trim
      }.toMap
  }
}
