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

import java.nio.file.Paths

import nl.knaw.dans.lib.error.CompositeException
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.solr.common.SolrInputDocument
import org.scalatest.Inside._
import org.scalatest._

import scala.util.Failure

class ApplicationWiringSpec extends FlatSpec with Matchers {

  private val store = "pdbs"
  private val uuid = "9da0541a-d2c8-432e-8129-979a9830b427"

  "update" should "call the overridden submit method" in {
    val wiring = new ApplicationWiring(createConfig("vault")) {
      // comment the next line for a quicker online test cycle than running from the command line
      // TODO try the EmbeddedSolrServer
      override def submit(doc: SolrInputDocument) = Failure(new Exception("mocked submit"))
    }
    inside(wiring.update(store, uuid)) {
      case Failure(e) => e.getMessage shouldBe s"mocked submit"
    }
  }

  "initSingleStore" should "call the overridden upadte method" in {
    val wiring = new ApplicationWiring(createConfig("vaultBagIds")) {
      override def update(store: String, uuid: String) = Failure(new Exception("mocked update"))
    }
    inside(wiring.initSingleStore(store)) {
      case Failure(e: CompositeException) =>
        e.getMessage shouldBe "5 exceptions occurred."
        e.getCause.getCause.getMessage shouldBe "mocked update"
    }
  }

  "initAllStores" should "call the overridden initSingleStore method" in {
    val wiring = new ApplicationWiring(createConfig("vaultStoreNames")) {
      override def initSingleStore(store: String) = Failure(new Exception("mocked initSingleStore"))
    }
    inside(wiring.initAllStores()) {
      case Failure(e: CompositeException) =>
        e.getMessage shouldBe "4 exceptions occurred."
        e.getCause.getCause.getMessage shouldBe "mocked initSingleStore"
    }
  }

  private def createConfig(testDir: String) = {
    val absolutePath = Paths.get(s"src/test/resources/$testDir").toAbsolutePath.toString
    val properties = new PropertiesConfiguration()
    properties.addProperty("solr.url", "http://deasy.dans.knaw.nl:8983/solr/#/easyfiles")
    properties.addProperty("vault.url", s"file:///$absolutePath/")
    new Configuration("", properties)
  }
}
