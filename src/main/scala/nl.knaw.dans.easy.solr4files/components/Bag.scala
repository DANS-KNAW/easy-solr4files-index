package nl.knaw.dans.easy.solr4files.components

import nl.knaw.dans.easy.solr4files.FileToShaMap

import scala.util.Try
import scala.xml.{ Elem, XML }

trait Bag {
  this: Vault =>
  val storeName: String
  val bagId: String

  def getDepositor: Try[String] = Try {
    val uri = vaultBaseUri.resolve(s"stores/$storeName/bags/$bagId/manifest-sha1.txt")
    val key = "EASY-User-Account"
    linesFrom(uri)
      .filter(_.trim.startsWith(key))
      .map(_.trim.replace(key, "").trim.replace(":", "").trim)
      .head // TODO friendly message in case field is absent or repeated
  }

  def getFileShas: Try[FileToShaMap] = Try {
    val uri = vaultBaseUri.resolve(s"stores/$storeName/bags/$bagId/manifest-sha1.txt")
    linesFrom(uri).map { line: String =>
      val xs = line.trim.split("""\s+""")
      (xs(1), xs(0))
    }.toMap
  }

  def loadDDM: Try[Elem] = loadXml("metadata/dataset.xml")

  def loadFilesXML: Try[Elem] = loadXml("metadata/files.xml")

  private def loadXml(file: String): Try[Elem] = Try {
    val uri = vaultBaseUri.resolve(s"stores/$storeName/bags/$bagId/$file")
    logger.info(s"Getting $uri")
    openManagedStream(uri).acquireAndGet(XML.load)
  }
}
