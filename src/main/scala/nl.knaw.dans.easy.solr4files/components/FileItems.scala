package nl.knaw.dans.easy.solr4files.components

import nl.knaw.dans.easy.solr4files.FileToShaMap
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.xml.{ Elem, Node }

class FileItems(xml: Elem, shas: FileToShaMap) extends DebugEnhancedLogging {
  private val fileNodes = xml \ "file"

  def openAccessTextFiles(): Seq[FileItem] = {
    fileNodes
      .filter(isOpenText)
      .map(toFileItem)
      .filter(_.isDefined) // file had attribute FilePath
      .map(_.get) // get is safe because of the filter
  }

  private def toFileItem(fileNode: Node): Option[FileItem] = {
    getPath(fileNode)
      .map(path =>
        new FileItem(shas(path), fileNode)
      )
  }

  private def getPath(fileNode: Node): Option[String] = {
    fileNode
      .attribute("filepath")
      .map(_.head.text.trim)
  }

  private def isOpenText(fileItem: Node): Boolean =
    hasTextFormat(fileItem) && !hasOtherAccessThanOpen(fileItem)

  private def hasTextFormat(fileItem: Node): Boolean =
    (fileItem \\ "format").text == "text/plain"

  private def hasOtherAccessThanOpen(fileItem: Node): Boolean = {
    val seq = fileItem \\ "accessRights"
    seq.nonEmpty && seq.text != "OPEN_ACCESS"
  }
}
