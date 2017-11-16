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
package nl.knaw.dans.easy.solr4files

import org.apache.http.HttpStatus._
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.{ SolrClient, SolrRequest, SolrResponse }
import org.apache.solr.common.params.SolrParams
import org.apache.solr.common.util.NamedList
import org.apache.solr.common.{ SolrDocument, SolrDocumentList }
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

class SearchServletSpec extends TestSupportFixture
  with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private class StubbedWiring extends ApplicationWiring(configWithMockedVault) {

    override lazy val solrClient: SolrClient = new SolrClient() {
      // can't use mock because SolrClient has a final method

      override def query(params: SolrParams): QueryResponse = mockQueryResponse(params)

      override def close(): Unit = ()

      override def request(solrRequest: SolrRequest[_ <: SolrResponse], s: String): NamedList[AnyRef] =
        throw new Exception("mocked request")
    }

    private def mockQueryResponse(params: SolrParams) = {
      new QueryResponse {
        override def getResults: SolrDocumentList = new SolrDocumentList {
          setNumFound(3)
          setStart(0)
          add(new SolrDocument(new java.util.TreeMap[String, AnyRef] {
            put("debug", s"$params")
          }))
          add(new SolrDocument(new java.util.HashMap[String, AnyRef] {
            put("name", "file.txt")
          }))
          add(new SolrDocument(new java.util.TreeMap[String, AnyRef] {
            // a sorted order makes testing easier
            put("name", "some.png")
            put("size", "123")
          }))
        }
      }
    }
  }

  private val app = new EasyUpdateSolr4filesIndexApp(new StubbedWiring)
  addServlet(new SearchServlet(app), "/*")

  "get /" should "complain about missing argument" in {
    get("/?q=something") {
      body shouldBe "filesearch requires param 'text' (a solr dismax query), got params [q -> something]"
      status shouldBe SC_BAD_REQUEST
    }
  }

  it should "return json" in {
    get(s"/?text=nothing") {
      body should startWith(
        """{
          |  "header":{
          |    "text":"nothing",
          |    "skip":0,
          |    "limit":10,
          |    "time_allowed":5000,
          |    "found":3,
          |    "returned":3
          |  },
          |  "fileitems":[{
          |    "debug":"defType=dismax&q=nothing&fq=easy_file_accessible_to:ANONYMOUS+OR+easy_file_accessible_to:KNOWN&fq=easy_dataset_date_available:[*+TO+NOW]&fl=easy_dataset_*,easy_file_*&start=0&rows=10&timeAllowed=5000"
          |  },{
          |""".stripMargin)
      body should include(
        """{
          |    "name":"file.txt"
          |  }""".stripMargin)
      body should include(
        """{
          |    "name":"some.png",
          |    "size":"123"
          |  }""".stripMargin)
      body should endWith(
        """
          |  }]
          |}""".stripMargin)
      status shouldBe SC_OK
    }
  }

  it should "translate limit to rows" in {
    get(s"/?text=nothing&limit=1") {
      body should include("&start=0&rows=1&timeAllowed=5000")
      status shouldBe SC_OK
    }
  }

  it should "translate skip to start" in {
    get(s"/?text=nothing&skip=1") {
      body should include("&start=1&rows=10&timeAllowed=5000")
      status shouldBe SC_OK
    }
  }

  it should "translate user specified filter" in {
    get(s"/?text=nothing&file_mime_type=application/pdf") {
      body should include("&fq=easy_file_mime_type:application/pdf&")
      status shouldBe SC_OK
    }
  }

  it should "translate encoded filter" in {
    get(s"/?text=nothing&file_mime_type=application%2Fpdf") {
      println(body)
      body should include("&fq=easy_file_mime_type:application/pdf&")
      status shouldBe SC_OK
    }
  }

  // TODO test authentication, see https://github.com/DANS-KNAW/easy-update-solr4files-index/pull/9#discussion_r150809734
}
