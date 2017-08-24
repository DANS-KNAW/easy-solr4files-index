package nl.knaw.dans.easy.solr4files

import java.net.URI
import java.nio.file.{ Path, Paths }

import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.Inside._
import org.scalatest._

import scala.util.Success

class ApplicationWiringSpec extends FlatSpec with Matchers {

  // can't mock the vault for init as is would require 'bags' and 'stores' as files and directories

  "update" should "succeed" in {
    val uuid = "9da0541a-d2c8-432e-8129-979a9830b427"
    val store = "pdbs"
    inside(createWiring().update(store, uuid)) {
      case Success(feedbackMessage) => feedbackMessage shouldBe s"Updated $store $uuid (6 files)"
    }
  }

  private def createWiring() = {

    new ApplicationWiring(new Configuration("", new PropertiesConfiguration())) {

      private val absolutePath: Path = Paths.get("src/test/resources/vault").toAbsolutePath
      override val vaultBaseUri = new URI(s"file:///$absolutePath/")
    }
  }
}
