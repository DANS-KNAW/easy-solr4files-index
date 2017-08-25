package nl.knaw.dans.easy.solr4files

import java.net.URI
import java.nio.file.{ Path, Paths }

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.solr.common.SolrInputDocument
import org.scalatest.Inside._
import org.scalatest._

import scala.util.Failure

class ApplicationWiringSpec extends FlatSpec with Matchers {

  private val properties = new PropertiesConfiguration("src/test/resources/debug-config/application.properties")
  private val configuration = new Configuration("", properties)
  private val store = "pdbs"
  private val uuid = "9da0541a-d2c8-432e-8129-979a9830b427"

  "update" should "call the mocked submit method" in {
    val wiring = new ApplicationWiring(configuration) {
      override val vaultBaseUri: URI = createURI("vault")

      override def submit(doc: SolrInputDocument) = Failure(new Exception("mocked submit"))
    }
    inside(wiring.update(store, uuid)) {
      case Failure(e) => e.getMessage shouldBe s"mocked submit"
    }
  }

  "initSingleStore" should "call the mocked submit upadte" in {
    val wiring = new ApplicationWiring(configuration) {
      override val vaultBaseUri: URI = createURI("vaultBagIds")

      override def update(store: String, uuid: String) = Failure(new Exception("mocked update"))
    }

    inside(wiring.initSingleStore(store)) {
      case Failure(e) => e.getMessage shouldBe "5 exceptions occurred."
    }
  }

  "initAllStores" should "call the mocked submit initSingleStore" in {
    val wiring = new ApplicationWiring(configuration) {
      override val vaultBaseUri: URI = createURI("vaultStoreNames")

      override def initSingleStore(store: String) = Failure(new Exception("mocked initSingleStore"))
    }
    inside(wiring.initAllStores()) {
      case Failure(e) => e.getMessage shouldBe "4 exceptions occurred."
    }
  }

  private def createURI(testDir: String) = {
    val absolutPath: Path = Paths.get(s"src/test/resources/$testDir").toAbsolutePath
    new URI(s"file:///$absolutPath/")
  }
}
