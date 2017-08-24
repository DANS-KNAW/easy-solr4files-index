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
