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

import nl.knaw.dans.easy.solr4files.TestSupportFixture
import nl.knaw.dans.easy.solr4files.components.RightsFor.{ ANONYMOUS, KNOWN, NONE, RESTRICTED_GROUP }
import org.joda.time.DateTime

class FileItemSpec extends TestSupportFixture {

  private val centaurBag = Bag("pdbs", uuidCentaur, mockedVault)

  "solrLiteral" should "return proper values" in {
    initVault()
    val path = "data/reisverslag/centaur.mpg"
    val xml = <file filepath={path}>
      <dcterms:type>http://schema.org/VideoObject</dcterms:type>
      <dcterms:format>video/mpeg</dcterms:format>
      <dcterms:title>video about the centaur meteorite</dcterms:title>
      <accessibleToRights>RESTRICTED_GROUP</accessibleToRights>
      <dcterms:relation xml:lang="en">data/reisverslag/centaur.srt</dcterms:relation>
      <dcterms:relation xml:lang="nl">data/reisverslag/centaur-nederlands.srt</dcterms:relation>
    </file>

    val authInfoItem = AuthInfoItem(s"someUUID/$path", "someone", DateTime.now, RESTRICTED_GROUP, RESTRICTED_GROUP)
    val fi = FileItem(centaurBag, ddm("OPEN_ACCESS"), xml, authInfoItem)
    fi.mimeType shouldBe "video/mpeg"
    fi.path shouldBe path

    val solrLiterals = fi.solrLiterals.toMap
    solrLiterals("file_path") shouldBe fi.path
    solrLiterals("file_size") shouldBe "0"
    solrLiterals("file_title") shouldBe "video about the centaur meteorite"
    solrLiterals("file_accessible_to") shouldBe "RESTRICTED_GROUP"
    solrLiterals("file_mime_type") shouldBe "video/mpeg"
    solrLiterals("file_checksum") shouldBe "1dd40013ce63dfa98dfe19f5b4bbf811ee2240f7"
  }

  it should "use the dataset rights as default" in {
    val authInfoItem = AuthInfoItem("", "", DateTime.now, ANONYMOUS, ANONYMOUS)
    val solrLiterals = FileItem(centaurBag, ddm("OPEN_ACCESS"), <file filepath="p"/>, authInfoItem)
      .solrLiterals.toMap
    solrLiterals("file_accessible_to") shouldBe "ANONYMOUS"
  }

  it should "also use the dataset rights as default" in {
    val authInfoItem = AuthInfoItem("", "", DateTime.now, KNOWN, KNOWN)
    val item = FileItem(centaurBag, ddm("OPEN_ACCESS_FOR_REGISTERED_USERS"), <file filepath="p"/>, authInfoItem)
    val solrLiterals = item.solrLiterals.toMap
    solrLiterals("file_accessible_to") shouldBe "KNOWN"
  }

  it should "not have read the lazy files in case of accessible to none" ignore { // TODO
    val authInfoItem = AuthInfoItem("", "", DateTime.now, NONE, NONE)
    val item = FileItem(centaurBag, ddm("NO_ACCESS"), <file filepath="p"/>, authInfoItem)

    // The bag.sha's and ddm.vocabularies are private
    // so we need side effects, not something like https://stackoverflow.com/questions/1651927/how-to-unit-test-for-laziness
    // we could mock the vault.fileURL for this test
    // throwing an error when called for the sha's
    // remains checking for the vocabularies in DDM
  }

  private def ddm(datasetAccessRights: String) = new DDM(
    <ddm:DDM xsi:schemaLocation="http://easy.dans.knaw.nl/schemas/md/ddm/ https://easy.dans.knaw.nl/schemas/md/ddm/ddm.xsd"
             xmlns:ddm="http://easy.dans.knaw.nl/schemas/md/ddm/">
      <ddm:profile>
        <ddm:accessRights>{datasetAccessRights}</ddm:accessRights>
        <ddm:creatorDetails></ddm:creatorDetails>
      </ddm:profile>
    </ddm:DDM>
  )
}
