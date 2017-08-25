package nl.knaw.dans.easy.solr4files.components

import java.net.URI
import java.nio.file.Paths

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

trait Vault {
  this: VaultIO with DebugEnhancedLogging =>

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
}
