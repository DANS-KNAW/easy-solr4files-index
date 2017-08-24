package nl.knaw.dans.easy.solr4files.components

import java.net.URI
import java.nio.file.{ Path, Paths }

import org.scalatest.Inside.inside
import org.scalatest.{ FlatSpec, Matchers }

import scala.util.Success

class BagSpecs extends FlatSpec with Matchers {

  private val bag =
    new Bag with Vault {
      private val absolutePath: Path = Paths.get("src/test/resources/vault").toAbsolutePath
      override val storeName: String = "pdbs"
      override val bagId: String = "9da0541a-d2c8-432e-8129-979a9830b427"
      override val vaultBaseUri: URI = new URI(s"file:///$absolutePath/")
    }

  "loadXml" should "load files.xml" in {
    bag.loadFilesXML shouldBe a[Success[_]]
  }

  "getFileShas" should "read the shas of the files" in {
    inside(bag.getFileShas) {
      case Success(shas) => shas.keys.size shouldBe 9
    }
  }
}
