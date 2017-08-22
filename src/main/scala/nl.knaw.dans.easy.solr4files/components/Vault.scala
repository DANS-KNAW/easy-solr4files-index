package nl.knaw.dans.easy.solr4files.components

import java.net.URI
import java.nio.file.{ Path, Paths }

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils.readLines

import scala.collection.JavaConverters._
import scala.util.{ Success, Try }
import scala.xml.{ Elem, XML }

trait Vault extends DebugEnhancedLogging {

  val vaultBaseUri: URI

  def getStoreNames: Try[Seq[String]] = Try {
    val uri = vaultBaseUri.resolve("stores")
    logger.info(s"getting storeNames with $uri")
    linesFrom(uri).map { line =>
      val trimmed = line.trim.replace("<", "").replace(">", "")
      Paths.get(new URI(trimmed).getPath).getFileName.toString
    }
  }

  def getBagIds(storeName: String): Try[Seq[String]] = Try {
    val storeURI = vaultBaseUri.resolve(s"stores/$storeName/bags")
    logger.info(s"getting bag ids with $storeURI")
    linesFrom(storeURI).map { _.trim }
  }

  def getFilesXml(storeName: String, bagId: String): Try[Elem] = Try {
    val uri = vaultBaseUri.resolve(s"stores/$storeName/bags/$bagId/")
    logger.info(s"Getting $uri")
    openManagedStream(uri).acquireAndGet(XML.load)
  }

  def textFiles(filesXML: Elem): Try[Seq[Path]] = {
    // TODO filter on mime type, move to FilesXml trait
    logger.info(s"${ (filesXML \ "file").size } files found")
    Success(Seq[Path]())
  }

  private def vaultFrom(uri: URI): URI = {
    new URI(uri.getScheme + "://" + uri.getAuthority + "/")
  }

  private def linesFrom(uri: URI): Seq[String] = {
    openManagedStream(uri).acquireAndGet(readLines).asScala
  }

  private def openManagedStream(uri: URI) = {
    // TODO friendly error messages, some situations:
    // "IllegalArgumentException: URI is not absolute" if vault is not configured
    // "FileNotFoundException .../MISSING_BAG_STORE/..." if neither default nor explicit store name was specified
    resource.managed(uri.toURL.openStream())
  }
}
