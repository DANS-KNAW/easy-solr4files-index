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

import java.net.URLEncoder
import java.nio.file.Paths

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.http.HttpStatus._
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.client.solrj.{ SolrClient, SolrRequest, SolrResponse }
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.util.NamedList
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

class SolrErrorHandlingSpec extends TestSupportFixture
  with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private val configuration = new Configuration("", new PropertiesConfiguration() {
    private val vaultPath = URLEncoder.encode(Paths.get(s"src/test/resources/vault/stores/pdbs").toAbsolutePath.toString, "UTF8")
    addProperty("solr.url", "http://deasy.dans.knaw.nl:8983/solr/easyfiles")
    addProperty("vault.url", s"file:///$vaultPath/")
  })
  private class StubbedWiring extends {} with ApplicationWiring(configuration) {
    override lazy val solrClient: SolrClient = new SolrClient() {
      // can't use mock because SolrClient has a final method, now we can't count the actual calls

      override def deleteByQuery(q: String): UpdateResponse = {
        case class MockedParseException() extends Exception
        throw new HttpSolrClient.RemoteSolrException("mockedHost", 0, "mocked message", MockedParseException())
      }

      override def commit(): UpdateResponse =
        throw new Exception("not expected call")

      override def add(doc: SolrInputDocument): UpdateResponse =
        throw new Exception("not expected call")

      override def close(): Unit = ()

      override def request(solrRequest: SolrRequest[_ <: SolrResponse], s: String): NamedList[AnyRef] =
        throw new Exception("not expected call")
    }
  }

  private val app = new EasyUpdateSolr4filesIndexApp(new StubbedWiring)
  addServlet(new EasyUpdateSolr4filesIndexServlet(app), "/*")


  it should "return the exception bubbling up from solrClient.deleteByQuery" in {
    delete("/xxx/yyy") {
      status shouldBe SC_INTERNAL_SERVER_ERROR // TODO should be bad request
      body shouldBe ""
    }
  }
}
