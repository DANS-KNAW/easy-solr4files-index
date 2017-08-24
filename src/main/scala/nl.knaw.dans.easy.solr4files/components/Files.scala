package nl.knaw.dans.easy.solr4files.components

import nl.knaw.dans.easy.solr4files.FileToShaMap
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.xml.{ Elem, Node }

class Files(xml: Elem, shas: FileToShaMap) extends DebugEnhancedLogging {
  private val fileNodes = xml \\ "file"

  def openTextFiles(): Seq[String] =
    fileNodes.filter(isOpenText).map(_ \@ "filepath").filter(_.trim.nonEmpty)

  private def isOpenText(fileItem: Node): Boolean =
    hasTextFormat(fileItem) && !hasOtherAccessThanOpen(fileItem)

  private def hasTextFormat(fileItem: Node): Boolean =
    (fileItem \\ "format").text == "text/plain"

  private def hasOtherAccessThanOpen(fileItem: Node): Boolean = {
    val seq = fileItem \\ "accessRights"
    seq.nonEmpty && seq.text != "OPEN_ACCESS"
  }
}
