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

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.http.HttpStatus._
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }
import scalaj.http.HttpResponse

class UpdateServletSpec extends TestSupportFixture
  with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private class App extends {
    // mock needs a constructor without arguments
    private val properties: PropertiesConfiguration = new PropertiesConfiguration() {
      addProperty("solr.url", "http://deasy.dans.knaw.nl:8983/solr/easyfiles")
      addProperty("vault.url", "http://deasy.dans.knaw.nl:20110/")
    }
  } with EasyUpdateSolr4filesIndexApp(new ApplicationWiring(new Configuration("", properties)))
  private val app = mock[App]
  addServlet(new EasyUpdateSolr4filesIndexServlet(app), "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      body shouldBe "EASY File Index is running."
      status shouldBe SC_OK
    }
  }

  "post /init[/:store]" should "return a feedback message for all stores" in {
    app.initAllStores _ expects() once() returning
      Success("xxx")
    post("/init") {
      body shouldBe "xxx"
      status shouldBe SC_OK
    }
  }

  it should "return a feedback message for a single store" in {
    (app.initSingleStore(_: String)) expects "pdbs" once() returning
      Success("xxx")
    post("/init/pdbs") {
      body shouldBe "xxx"
      status shouldBe SC_OK
    }
  }

  it should "return NOT FOUND for an empty path" in {
    post("/init/") {
      body should startWith("""Requesting "POST /init/" on servlet "" but only have:""")
      status shouldBe SC_NOT_FOUND
    }
  }

  "post /update/:store/:uuid" should "return a feedback message" in {
    (app.update(_: String, _: String)) expects("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") once() returning
      Success("12 files submitted")
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      body shouldBe "12 files submitted"
      status shouldBe SC_OK
    }
  }

  it should "return NOT FOUND if uuid is missing" in {
    post("/update/pdbs/") {
      body should startWith("""Requesting "POST /update/pdbs/" on servlet "" but only have:""")
      status shouldBe SC_NOT_FOUND
    }
  }

  it should "return NOT FOUND if something is not found for the first file, bag or store" in {
    (app.update(_: String, _: String)) expects("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") once() returning
      Failure(createHttpException(SC_NOT_FOUND))
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      body shouldBe "getContent(url)"
      status shouldBe SC_NOT_FOUND
    }
  }

  it should "return NOT FOUND with too many path elements" in {
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427/") {
      body should startWith("""Requesting "POST /update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427/" on servlet "" but only have:""")
      status shouldBe SC_NOT_FOUND
    }
  }

  it should "return NOT FOUND if something is not found for the n-th file, bag or store" in {
    // TODO check if exceptions from getContent indeed bubble up: refactor RichUrl into heavy cake trait
    (app.update(_: String, _: String)) expects("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") once() returning
      Failure(MixedResultsException(Seq.empty, createHttpException(SC_NOT_FOUND)))
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      body shouldBe "Log files should show which actions succeeded. Finally failed with: getContent(url)"
      status shouldBe SC_NOT_FOUND
    }
  }

  it should "return INTERNAL SERVER ERROR in case of unexpected errors" in {
    (app.update(_: String, _: String)) expects("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") once() returning
      Failure(new Exception())
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      body shouldBe ""
      status shouldBe SC_INTERNAL_SERVER_ERROR
    }
  }

  "delete /?q=XXX" should "return a feedback message" in {
    (app.delete(_: String)) expects "*:*" once() returning
      Success("xxx")
    delete("/?q=*:*") {
      body shouldBe "xxx"
      status shouldBe SC_OK
    }
  }

  it should "return BAD REQUEST with an invalid query" in {
    (app.delete(_: String)) expects ":" once() returning
      Failure(SolrBadRequestException("Cannot parse ':'", new Exception))
    delete("/?q=:") {
      body should startWith("Cannot parse ':'")
      status shouldBe SC_BAD_REQUEST
    }
  }

  it should "complain about the required query" in {
    delete("/") {
      body shouldBe "delete requires param 'q': a solr query"
      status shouldBe SC_BAD_REQUEST
    }
  }

  "delete /:store[/:uuid]" should "return a feedback message with just a store" in {
    (app.delete(_: String)) expects "easy_dataset_store_id:pdbs" once() returning
      Success("xxx")
    delete("/pdbs") {
      body shouldBe "xxx"
      status shouldBe SC_OK
    }
  }

  it should "return a feedback message with store and UUID" in {
    (app.delete(_: String)) expects "easy_dataset_id:9da0541a-d2c8-432e-8129-979a9830b427" once() returning
      Success("xxx")
    delete("/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      body shouldBe "xxx"
      status shouldBe SC_OK
    }
  }

  it should "return NOT FOUND with too many path elements" in {
    delete("/pdbs/9da0541a-d2c8-432e-8129-979a9830b427/") {
      body should startWith("""Requesting "DELETE /pdbs/9da0541a-d2c8-432e-8129-979a9830b427/" on servlet "" but only have:""")
      status shouldBe SC_NOT_FOUND
    }
  }

  private def createHttpException(code: Int) = {
    val headers = Map[String, String]("Status" -> "")
    val r = new HttpResponse[String]("", code, headers)
    // URL could be a vocabulary for the DDM class or addressing the bag store service
    HttpStatusException("getContent(url)", r)
  }
}
