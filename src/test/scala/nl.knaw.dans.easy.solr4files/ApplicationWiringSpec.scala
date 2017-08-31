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
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.client.solrj.{SolrClient, SolrRequest, SolrResponse}
import org.apache.solr.common.util.NamedList
import org.scalatest.Inside._
import org.scalatest._

import scala.util.Success

class ApplicationWiringSpec extends FlatSpec with Matchers {

  private val store = "pdbs"
  private val uuid = "9da0541a-d2c8-432e-8129-979a9830b427"
  private class MockedAndStubbedWiring extends ApplicationWiring(createConfig("vault")){
    override lazy val solrClient = new SolrClient(){ // TODO replace with mock[SolrClient]
      override def request(solrRequest: SolrRequest[_ <: SolrResponse], s: String): NamedList[AnyRef] = new NamedList[AnyRef]()
      override def deleteByQuery(q: String): UpdateResponse = new UpdateResponse
      override def commit(): UpdateResponse = new UpdateResponse
      override def close(): Unit = ()
    }
  }

  "update" should "call the stubbed solrClient.request" in {
    val result = new MockedAndStubbedWiring().update(store, uuid)
    inside(result) {
      case Success(msg) => msg shouldBe s"Updated pdbs $uuid (7 files)"
    }
  }

  "delete" should "call the stubbed solrClient.deleteByQuery" in {
    val result = new MockedAndStubbedWiring().delete(uuid)
    inside(result) {
      case Success(msg) => msg shouldBe s"Deleted file documents for bag $uuid"
    }
  }

  "initSingleStore" should "call the stubbed ApplicationWiring.update method" in {
    val result = new ApplicationWiring(createConfig("vaultBagIds")) {
      // vaultBagIds/bags can't be a file and directory so we have to stub
      override def update(store: String, uuid: String) = Success("stubbed ApplicationWiring.update")
    }.initSingleStore(store)
    inside(result) {
      case Success(msg) => msg shouldBe "Updated 5 bags of one store (pdbs)"
    }
  }

  "initAllStores" should "call the stubbed ApplicationWiring.initSingleStore method" in {
    val result = new ApplicationWiring(createConfig("vaultStoreNames")) {
      // vaultStoreNames/stores can't be a file and directory so we have to stub
      override def initSingleStore(store: String) = Success("stubbed initSingleStore")
    }.initAllStores()
    inside(result) {
      case Success(msg) => msg should startWith ("Updated all bags of 4 stores (file:///")
    }
  }

  private def createConfig(testDir: String) = {
    val vaultPath = URLEncoder.encode(Paths.get(s"src/test/resources/$testDir").toAbsolutePath.toString, "UTF8")
    val properties = new PropertiesConfiguration()
    properties.addProperty("solr.url", "http://deasy.dans.knaw.nl:8983/solr/easyfiles")
    properties.addProperty("vault.url", s"file:///$vaultPath/")
    new Configuration("", properties)
  }
}
