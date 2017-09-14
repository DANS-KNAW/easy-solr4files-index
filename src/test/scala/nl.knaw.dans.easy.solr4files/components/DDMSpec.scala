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

import nl.knaw.dans.easy.solr4files.{ TestSupportFixture, _ }

class DDMSpec extends TestSupportFixture {

  private val vault = mockVault("vault")

  "solrLiteral" should "return proper values" in {
    assume(canConnectToEasySchemas)
    val uuid = "9da0541a-d2c8-432e-8129-979a9830b427"
    val xml = vault.fileURL("pdbs", uuid, "metadata/dataset.xml").flatMap(_.loadXml).get

    val ddm = new DDM(xml)
    ddm.accessRights shouldBe "OPEN_ACCESS"
    val literals: Seq[(String, String)] = ddm.solrLiterals
      .map { case (k, v) => (k, v.replaceAll("\\s+", " ").trim) }
      .filter { case (_, v) => !v.isEmpty }
    val expected = Seq(
      ("dataset_doi", "10.5072/dans-x6g-x2hb"),
      ("dataset_audience", "D30000"),
      ("dataset_subject", "Humanities"),
      ("dataset_relation", "dummy"),
      ("dataset_relation", "blabla"),
      ("dataset_coverage_temporal", "random text"),
      ("dataset_creator", "Captain J.T. Kirk United Federation of Planets"),
      ("dataset_subject", "astronomie"),
      ("dataset_subject", "ruimtevaart"),
      ("dataset_subject_abr", "IX"),
      ("dataset_subject", "Infrastructuur, onbepaald"),
      ("dataset_coverage_temporal_abr", "PALEOLB"),
      ("dataset_coverage_temporal", "Paleolithicum laat B: 18000 C14 -8800 vC"),
      ("dataset_title", "Reis naar Centaur-planetoïde"),
      ("dataset_title", "Trip to Centaur asteroid")
    )
    // in case of problems "should contain theSameElementsAs" gives two very long lists that do not equal
    // the following checks signal a problem in a short way at the end of the exception message
    expected.foreach(literals should contain(_))
    literals.foreach(expected should contain(_))
  }

  ignore should "have white space in a one liner creator" in { // TODO support proper white space for one liners?
    assume(canConnectToEasySchemas)
    val ddmLiterals = new DDM(<ddm:DDM
        xsi:schemaLocation="http://easy.dans.knaw.nl/schemas/md/ddm/ https://easy.dans.knaw.nl/schemas/md/ddm/ddm.xsd"
        xmlns:ddm="http://easy.dans.knaw.nl/schemas/md/ddm/"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:dcx-dai="http://easy.dans.knaw.nl/schemas/dcx/dai/"
        xmlns:id-type="http://easy.dans.knaw.nl/schemas/vocab/identifier-type/">
      <ddm:profile>
        <dcx-dai:creatorDetails>
          <dcx-dai:author>
            <dcx-dai:titles>Captain</dcx-dai:titles><dcx-dai:initials>J.T.</dcx-dai:initials><dcx-dai:surname>Kirk</dcx-dai:surname>
            <dcx-dai:organization>
              <dcx-dai:name xml:lang="en">United Federation of Planets</dcx-dai:name>
            </dcx-dai:organization>
          </dcx-dai:author>
        </dcx-dai:creatorDetails>
      </ddm:profile>
    </ddm:DDM>
    ).solrLiterals.toMap
    ddmLiterals("dataset_creator") shouldBe "Captain J.T. Kirk United Federation of Planets"
  }

}
