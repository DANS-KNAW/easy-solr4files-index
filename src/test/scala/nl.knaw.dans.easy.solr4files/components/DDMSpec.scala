package nl.knaw.dans.easy.solr4files.components

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Falkink on 1-9-2017.
  */
class DDMSpec  extends FlatSpec with Matchers {

  "solrLiteral" should "return ..." in {
    val ddm = new DDM(<ddm:DDM
        xsi:schemaLocation="http://easy.dans.knaw.nl/schemas/md/ddm/ https://easy.dans.knaw.nl/schemas/md/ddm/ddm.xsd"
        xmlns:ddm="http://easy.dans.knaw.nl/schemas/md/ddm/"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dct="http://purl.org/dc/terms/"
        xmlns:dcterms="http://purl.org/dc/terms/"
        xmlns:dcmitype="http://purl.org/dc/dcmitype/"
        xmlns:dcx="http://easy.dans.knaw.nl/schemas/dcx/"
        xmlns:dcx-dai="http://easy.dans.knaw.nl/schemas/dcx/dai/"
        xmlns:dcx-gml="http://easy.dans.knaw.nl/schemas/dcx/gml/"
        xmlns:narcis="http://easy.dans.knaw.nl/schemas/vocab/narcis-type/"
        xmlns:abr="http://www.den.nl/standaard/166/Archeologisch-Basisregister/"
        xmlns:id-type="http://easy.dans.knaw.nl/schemas/vocab/identifier-type/">
      <ddm:profile>
        <dc:title>Reis naar Centaur-planetoïde</dc:title>
        <dc:title>Trip to Centaur asteroid</dc:title>
        <dcx-dai:creatorDetails>
          <dcx-dai:author>
            <dcx-dai:titles>Captain</dcx-dai:titles>
            <dcx-dai:initials>J.T.</dcx-dai:initials>
            <dcx-dai:surname>Kirk</dcx-dai:surname>
            <dcx-dai:organization>
              <dcx-dai:name xml:lang="en">United Federation of Planets</dcx-dai:name>
            </dcx-dai:organization>
          </dcx-dai:author>
        </dcx-dai:creatorDetails>
        <ddm:audience>D30000</ddm:audience>
        <ddm:accessRights>OPEN_ACCESS</ddm:accessRights>
      </ddm:profile>
      <ddm:dcmiMetadata>
        <dcterms:spatial>here</dcterms:spatial>
        <dcterms:spatial>there</dcterms:spatial>
        <dc:relation>http://x</dc:relation>
        <ddm:relation scheme="STREAMING_SURROGATE_RELATION">/domain/dans/user/janvanmansum/collection/Jans-test-files/presentation/easy-dataset:14</ddm:relation>
        <dc:subject>astronomie</dc:subject>
        <dc:subject>ruimtevaart</dc:subject>
        <dc:subject xsi:type="abr:ABRcomplex">IX</dc:subject>
        <dcx-gml:spatial srsName="http://www.opengis.net/def/crs/EPSG/0/4326">
          <Point xmlns="http://www.opengis.net/gml">
            <pos>455271.2 83575.4</pos>
          </Point>
        </dcx-gml:spatial>
        <dcx-gml:spatial>
          <boundedBy xmlns="http://www.opengis.net/gml">
            <Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/28992">
              <lowerCorner>4 2</lowerCorner>
              <upperCorner>3 1</upperCorner>
            </Envelope>
          </boundedBy>
        </dcx-gml:spatial>
        <dcterms:temporal xsi:type="abr:ABRperiode">PALEOLB</dcterms:temporal>
        <dcterms:temporal>random text</dcterms:temporal>
      </ddm:dcmiMetadata>
    </ddm:DDM>)
    ddm.accessRights shouldBe "OPEN_ACCESS"
    ddm.solrLiterals should (
      contain(("dataset_audience", "D30000")) and
        contain(("dataset_doi", ""))
      )
    ddm.solrLiterals should (
      contain(("dataset_relation", "/domain/dans/user/janvanmansum/collection/Jans-test-files/presentation/easy-dataset:14")) and
        contain(("dataset_creator", "Captain J.T. Kirk, United Federation of Planets"))
      )
    ddm.solrLiterals should (
      contain(("dataset_title", "Reis naar Centaur-planetoïde"))
        and contain(("dataset_title", "Trip to Centaur asteroid"))
      )
  }
}
