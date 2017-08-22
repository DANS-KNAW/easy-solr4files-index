package nl.knaw.dans.easy.solr4files.components

import java.net.URI
import java.nio.file.{ Path, Paths }

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils.readLines

import scala.collection.JavaConverters._
import scala.util.{ Success, Try }
import scala.xml.{ Elem, XML }

trait Vault extends DebugEnhancedLogging {

  /** @return URI's with trailing slash: ready to extend the path for a specific file */
  def getBags(storeURI: URI): Try[Seq[URI]] = Try {
    val storeName = Paths.get(storeURI.getPath).getParent.getFileName
    val vaultURI = vaultFrom(storeURI)
    linesFrom(storeURI).map { line =>
      val uuid = line.trim
      vaultURI.resolve(s"stores/$storeName/bags/$uuid/")
    }
  }

  /** @return URI's without trailing slash: ready for a HTTP request that lists the bags */
  def getStores(baseURI: URI): Try[Seq[URI]] = Try {
    val vaultURI = vaultFrom(baseURI)
    linesFrom(baseURI).map { line =>
      val trimmed = line.trim.replace("<", "").replace(">", "")
      val storeName = Paths.get(new URI(trimmed).getPath).getFileName
      vaultURI.resolve(s"stores/$storeName/bags")
    }
  }

  def getFilesXml(baseUri: URI): Try[Elem] = Try {
    val uri = baseUri.resolve("metadata/files.xml")
    logger.info(s"Getting $uri")
    XML.load(openStream(uri))
  }

  def textFiles(filesXML: Elem): Try[Seq[Path]] = {
    // TODO filter on mime type
    logger.info(s"${ (filesXML \ "file").size } files found")
    Success(Seq[Path]())
  }

  private def vaultFrom(uri: URI): URI = {
    new URI(uri.getScheme + "://" + uri.getAuthority + "/")
  }

  private def linesFrom(uri: URI): Seq[String] = {
    readLines(openStream(uri)).asScala
  }

  private def openStream(uri: URI) = {
    // TODO fix "IllegalArgumentException: URI is not absolute" if vault is not configured
    // TODO fox "FileNotFoundException .../MISSING_BAG_STORE/..." if neither default nor explicit store name was specified
    uri.toURL.openStream() // TODO close
  }
}
