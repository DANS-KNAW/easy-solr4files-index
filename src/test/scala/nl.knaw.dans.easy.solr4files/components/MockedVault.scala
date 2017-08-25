package nl.knaw.dans.easy.solr4files.components

import java.net.URI
import java.nio.file.{ Path, Paths }

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

case class MockedVault(testDir: String) extends Vault with DebugEnhancedLogging {
  private val absolutePath: Path = Paths.get(s"src/test/resources/$testDir").toAbsolutePath
  override val vaultBaseUri = new URI(s"file:///$absolutePath/")
}