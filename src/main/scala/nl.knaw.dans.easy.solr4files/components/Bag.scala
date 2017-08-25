package nl.knaw.dans.easy.solr4files.components

import nl.knaw.dans.easy.solr4files.FileToShaMap
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try
import scala.xml.Elem

case class Bag(storeName: String,
               bagId: String,
               vaultIO: VaultIO
              ) extends DebugEnhancedLogging {

  def getDepositor: Try[String] = Try {
    val key = "EASY-User-Account"
    vaultIO.linesFrom(storeName, bagId, "manifest-sha1.txt")
      .filter(_.trim.startsWith(key))
      .map(_.trim.replace(key, "").trim.replace(":", "").trim)
      .head // TODO friendly message in case field is absent or repeated
  }

  def getFileShas: Try[FileToShaMap] = Try {
    vaultIO.linesFrom(storeName, bagId, "manifest-sha1.txt").map { line: String =>
      val xs = line.trim.split("""\s+""")
      (xs(1), xs(0))
    }.toMap
  }

  def loadDDM: Try[Elem] = vaultIO.loadXml(storeName, bagId, "metadata/dataset.xml")

  def loadFilesXML: Try[Elem] = vaultIO.loadXml(storeName, bagId, "metadata/files.xml")
}
