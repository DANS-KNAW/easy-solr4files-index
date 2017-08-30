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

import java.net.{URI, URL, URLEncoder}
import java.nio.file.Paths

import nl.knaw.dans.easy.solr4files.components._
import nl.knaw.dans.lib.error.CompositeException
import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.Inside._
import org.scalatest._

import scala.collection.mutable
import scala.util.Try
import scala.util.{Failure, Success}

class ApplicationWiringSpec extends FlatSpec with Matchers {

  private val store = "pdbs"
  private val uuid = "9da0541a-d2c8-432e-8129-979a9830b427"

  "update" should "call the stubbed Solr.submit method" in {
    val wiring = new ApplicationWiring(createConfig("vault")) {
      // comment the next line for a quicker online test cycle than running from the command line
      // TODO try the EmbeddedSolrServer
      override def createDoc(bag: Bag, ddm: DDM, item: FileItem): Try[String] = Success(s"stubbed Solr.createDoc ${ item.path }")
      override def deleteBag(bagId: String): Try[String] = Success(s"stubbed Solr.deleteBag $bagId")
    }
    inside(wiring.update(store, uuid)) {
      case Success(msg) => msg shouldBe s"Updated pdbs $uuid (7 files)"
      case Failure(t) if t.getCause.isInstanceOf[CompositeException] => t.getCause.printStackTrace()
      case Failure(t) => t.printStackTrace()
    }
  }

  "delete" should "call the stubbed Solr.delete method" in {
    val wiring = new ApplicationWiring(createConfig("vault")) {
      override def deleteBag(bagId: String) = Success(s"stubbed Solr.deleteBag $bagId")
    }
    inside(wiring.delete(uuid)) {
      case Success(msg) => msg shouldBe s"Deleted file documents for bag $uuid"
    }
  }

  "initSingleStore" should "call the stubbed ApplicationWiring.update method" in {
    val wiring = new ApplicationWiring(createConfig("vaultBagIds")) {
      override def update(store: String, uuid: String) = Success("stubbed ApplicationWiring.update")
    }
    inside(wiring.initSingleStore(store)) {
      case Success(msg) => msg shouldBe "Updated 5 bags of one store (pdbs)"
    }
  }

  "initAllStores" should "call the stubbed ApplicationWiring.initSingleStore method" in {
    val path = Paths.get(s"src/test/resources/vaultStoreNames").toAbsolutePath
    val wiring = new ApplicationWiring(createConfig("vaultStoreNames")) {
      override def initSingleStore(store: String) = Success("stubbed initSingleStore")
    }
    inside(wiring.initAllStores()) {
      case Success(msg) => msg shouldBe s"Updated all bags of 4 stores (file:///$path/)"
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
