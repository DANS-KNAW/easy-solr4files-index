package nl.knaw.dans.easy.solr4files.components

import java.nio.file.Path

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Success, Try }
import scala.xml.Elem

class FilesXml (filesXML: Elem) extends DebugEnhancedLogging {
  def textFiles(): Try[Seq[Path]] = {
    // TODO filter on mime type, move to FilesXml trait
    logger.info(s"${ (filesXML \ "file").size } files found")
    Success(Seq[Path]())
  }
}
