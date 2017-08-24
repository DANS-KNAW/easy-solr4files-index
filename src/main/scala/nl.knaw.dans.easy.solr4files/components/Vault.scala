package nl.knaw.dans.easy.solr4files.components

import java.io.InputStream
import java.net.URI
import java.nio.file.Paths

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils.readLines
import resource.ManagedResource

import scala.collection.JavaConverters._
import scala.util.Try

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

  def linesFrom(uri: URI): Seq[String] = {
    openManagedStream(uri).acquireAndGet(readLines).asScala
  }

  def openManagedStream(uri: URI): ManagedResource[InputStream] = {
    // TODO friendly error messages, some situations:
    // "IllegalArgumentException: URI is not absolute" if vault is not configured
    // "FileNotFoundException .../MISSING_BAG_STORE/..." if neither default nor explicit store name was specified
    resource.managed(uri.toURL.openStream())
  }
}
