/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr4files.components

import java.net.URI

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

    val xs = new FileItems(xml, fileToShaMap, new URI("file:///")).openAccessTextFiles()
    xs.size shouldBe 1
    xs.head.path shouldBe "data/reisverslag/centaur.srt"
  }
}
