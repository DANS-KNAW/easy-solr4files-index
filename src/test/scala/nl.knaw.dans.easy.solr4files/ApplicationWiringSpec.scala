package nl.knaw.dans.easy.solr4files

import java.net.URI
import java.nio.file.{ Path, Paths }

import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.Inside._
import org.scalatest._

import scala.util.Success

class ApplicationWiringSpec extends FlatSpec with Matchers {

  private val properties = new PropertiesConfiguration("src/test/resources/debug-config/application.properties")
  private val configuration = new Configuration("", properties)
  private val store = "pdbs"
  private val uuid = "9da0541a-d2c8-432e-8129-979a9830b427"

  "update" should "succeed" in {
    val wiring = new ApplicationWiring(configuration) {
      override val vaultBaseUri: URI = createURI("vault")
    }
    inside(wiring.update(store, uuid)) {
      case Success(feedbackMessage) => feedbackMessage shouldBe s"Updated $store $uuid (6 files)"
    }
  }

  "initSingleStore" should "succeed" in {
    val wiring = new ApplicationWiring(configuration) {
      override val vaultBaseUri: URI = createURI("vaultBagIds")

      override def update(store: String, uuid: String) = Success("Done")
    }

    inside(wiring.initSingleStore(store)) {
      case Success(feedbackMessage) => feedbackMessage shouldBe s"Updated bags of one store (pdbs)"
    }
  }

  "initAllStores" should "succeed" in {
    val wiring = new ApplicationWiring(configuration) {
      override val vaultBaseUri: URI = createURI("vaultStoreNames")

      override def initSingleStore(store: String) = Success("Done")
    }
    inside(wiring.initAllStores()) {
      case Success(feedbackMessage) => feedbackMessage shouldBe s"Updated all bags of all stores (${ wiring.vaultBaseUri })"
    }
  }

  private def createURI(testDir: String) = {
    val absolutPath: Path = Paths.get(s"src/test/resources/$testDir").toAbsolutePath
    new URI(s"file:///$absolutPath/")
  }
}
