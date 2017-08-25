package nl.knaw.dans.easy.solr4files.components

import nl.knaw.dans.easy.solr4files._
import org.scalatest._

class FilesSpec extends FlatSpec with Matchers {

  "openTextFiles" should "return the second file" in {
    val fileToShaMap = FileToShaMap(
      "data/reisverslag/centaur-nederlands.srt" -> "sha1",
      "data/reisverslag/centaur.srt" -> "sha2",
      "data/reisverslag/deel01.docx" -> "sha3"
    )
    val xml = <files xmlns:dcterms="http://purl.org/dc/terms/">
      <file filepath="data/reisverslag/centaur-nederlands.srt">
        <dcterms:format>text/plain</dcterms:format>
        <dcterms:accessRights>RESTRICTED_GROUP</dcterms:accessRights>
      </file>
      <file filepath="data/reisverslag/centaur.srt">
        <dcterms:format>text/plain</dcterms:format>
      </file>
      <file filepath="data/reisverslag/deel01.docx">
        <dcterms:format>application/vnd.openxmlformats-officedocument.wordprocessingml.document</dcterms:format>
      </file>
    </files>

    val xs = new FileItems(xml, fileToShaMap).openAccessTextFiles()
    xs.size shouldBe 1
    xs.head.checkSum shouldBe "sha2"
    xs.head.path shouldBe "data/reisverslag/centaur.srt"
  }
}
