package nl.knaw.dans.easy.solr4files.components

import java.net.URI
import java.nio.file.{ Path, Paths }

import org.scalatest.Inside._
import org.scalatest._

import scala.util.Success

class VaultSpec extends FlatSpec with Matchers {

  "getStoreNames" should "return names" in {
    inside(createVault("vaultStoreNames").getStoreNames) {
      case Success(names) => names should contain only("easy", "pdbs", "mendeley", "dryad")
    }
  }

  "getBagIds" should "return UUID's" in {
    inside(createVault("vaultBagIds").getBagIds("pdbs")) {
      case Success(names) => names should contain only(
        "9da0541a-d2c8-432e-8129-979a9830b427",
        "24d305fc-060c-4b3b-a5f5-9f212d463cbc",
        "3528bd4c-a87a-4bfa-9741-a25db7ef758a",
        "f70c19a5-0725-4950-aa42-6489a9d73806",
        "6ccadbad-650c-47ec-936d-2ef42e5f3cda")
    }
  }

  "getFilesXml" should "load files.xml" in {
    createVault("vault").readFilesXml("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") shouldBe a[Success[_]]
  }

  private def createVault (testDir: String) = {
    new Vault {
      private val absolutePath: Path = Paths.get("src/test/resources/" + testDir).toAbsolutePath
      override val vaultBaseUri = new URI(s"file:///$absolutePath/")
    }
  }
}
