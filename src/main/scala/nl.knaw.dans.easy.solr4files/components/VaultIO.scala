package nl.knaw.dans.easy.solr4files.components

import java.io.InputStream
import java.net.URI

import org.apache.commons.io.IOUtils.readLines
import resource.ManagedResource

import scala.collection.JavaConverters._
import scala.util.Try
import scala.xml.{ Elem, XML }

trait VaultIO {
  val vaultBaseUri: URI

  def linesFrom(uri: URI): Seq[String] = {
    openManagedStream(uri).acquireAndGet(readLines).asScala
  }

  def linesFrom(storeName: String, bagId: String, file: String): Seq[String] = {
    openManagedStream(storeName, bagId, file)
      .acquireAndGet(readLines).asScala
  }

  def loadXml(storeName: String, bagId: String, file: String): Try[Elem] = Try {
    openManagedStream(storeName, bagId, file)
      .acquireAndGet(XML.load)
  }

  private def openManagedStream(storeName: String, bagId: String, file: String): ManagedResource[InputStream] = {
    openManagedStream(vaultBaseUri.resolve(s"stores/$storeName/bags/$bagId/$file"))
  }

  def openManagedStream(uri: URI): ManagedResource[InputStream] = {
    // TODO friendly error messages (using https://github.com/scalaj/scalaj-http ?), some situations:
    // "IllegalArgumentException: URI is not absolute" if vault is not configured
    // "FileNotFoundException .../MISSING_BAG_STORE/..." if neither default nor explicit store name was specified
    resource.managed(uri.toURL.openStream())
  }
}
