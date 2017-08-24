package nl.knaw.dans.easy.solr4files.components

import nl.knaw.dans.easy.solr4files._
import org.scalatest._

class FilesSpec extends FlatSpec with Matchers {

  "openTextFiles" should "return the second file" in {
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

    new Files(xml, FileToShaMap()).openTextFiles() should
      contain only "data/reisverslag/centaur.srt"
  }
}
