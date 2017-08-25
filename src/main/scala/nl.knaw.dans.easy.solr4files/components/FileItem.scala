package nl.knaw.dans.easy.solr4files.components

import scala.xml.Node

class FileItem(sha: String, xml: Node) {
  def mimeType: String = (xml \ "format").text

  def checkSum: String = sha

  def path: String =
    xml.attribute("filepath").map(_.text).getOrElse("")
}
