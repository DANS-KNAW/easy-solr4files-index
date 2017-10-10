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
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{ Failure, Success }
import scalaj.http.HttpResponse

class UpdateServletSpec extends TestSupportFixture
  with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private class App extends EasyUpdateSolr4filesIndexApp(null)
  private val app = mock[App]
  addServlet(new EasyUpdateSolr4filesIndexServlet(app), "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      status shouldBe SC_OK
      body shouldBe "EASY File Index is running."
    }
  }

  "post /init[/:store]" should "return a feedback message for all stores" in {
    app.initAllStores _ expects() once() returning
      Success("xxx")
    post("/init") {
      status shouldBe SC_OK
      body shouldBe "xxx"
    }
  }

  it should "return a feedback message for a single store" in {
    (app.initSingleStore(_: String)) expects "pdbs" once() returning
      Success("xxx")
    post("/init/pdbs") {
      status shouldBe SC_OK
      body shouldBe "xxx"
    }
  }

  it should "return NOT FOUND for an empty path" in {
    post("/init/") {
      status shouldBe SC_NOT_FOUND
      body should startWith("""Requesting "POST /init/" on servlet "" but only have:""")
    }
  }

  "post /update/:store/:uuid" should "return a feedback message" in {
    (app.update(_: String, _: String)) expects("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") once() returning
      Success("12 files submitted")
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      status shouldBe SC_OK
      body shouldBe "12 files submitted"
    }
  }

  it should "return NOT FOUND if uuid is missing" in {
    post("/update/pdbs/") {
      status shouldBe SC_NOT_FOUND
      body should startWith("""Requesting "POST /update/pdbs/" on servlet "" but only have:""")
    }
  }

  it should "return NOT FOUND if something is not found for the first file, bag or store" in {
    (app.update(_: String, _: String)) expects("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") once() returning
      Failure(createHttpException(SC_NOT_FOUND))
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      status shouldBe SC_NOT_FOUND
      body shouldBe "getContent(url)"
    }
  }

  it should "return NOT FOUND with too many path elements" in {
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427/") {
      status shouldBe SC_NOT_FOUND
      body should startWith("""Requesting "POST /update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427/" on servlet "" but only have:""")
    }
  }

  it should "return NOT FOUND if something is not found for the n-th file, bag or store" in {
    // TODO check if exceptions from getContent indeed bubble up: refactor RichUrl into heavy cake trait
    (app.update(_: String, _: String)) expects("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") once() returning
      Failure(MixedResultsException(Seq.empty, createHttpException(SC_NOT_FOUND)))
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      status shouldBe SC_NOT_FOUND
      body shouldBe "Log files should show which actions succeeded. Finally failed with: getContent(url)"
    }
  }

  it should "return INTERNAL SERVER ERROR in case of unexpected errors" in {
    (app.update(_: String, _: String)) expects("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427") once() returning
      Failure(new Exception())
    post("/update/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      status shouldBe SC_INTERNAL_SERVER_ERROR
      body shouldBe ""
    }
  }

  "delete /?q=XXX" should "return a feedback message" in {
    (app.delete(_: String)) expects "*:*" once() returning
      Success("xxx")
    delete("/?q=*:*") {
      status shouldBe SC_OK
      body shouldBe "xxx"
    }
  }

  it should "return BAD REQUEST with an invalid query" in {
    // TODO check the error bubbles up from solrClient.deleteByQuery
    (app.delete(_: String)) expects ":" once() returning
      Failure(SolrBadRequestException("Cannot parse ':'", new Exception))
    delete("/?q=:") {
      status shouldBe SC_BAD_REQUEST
      body should startWith("Cannot parse ':'")
    }
  }

  it should "complain about the required query" in {
    delete("/") {
      status shouldBe SC_BAD_REQUEST
      body shouldBe "delete requires param 'q': a solr query"
    }
  }

  "delete /:store[/:uuid]" should "return a feedback message with just a store" in {
    (app.delete(_: String)) expects "easy_dataset_store_id:pdbs" once() returning
      Success("xxx")
    delete("/pdbs") {
      status shouldBe SC_OK
      body shouldBe "xxx"
    }
  }

  it should "return a feedback message with store and UUID" in {
    (app.delete(_: String)) expects "easy_dataset_id:9da0541a-d2c8-432e-8129-979a9830b427" once() returning
      Success("xxx")
    delete("/pdbs/9da0541a-d2c8-432e-8129-979a9830b427") {
      status shouldBe SC_OK
      body shouldBe "xxx"
    }
  }

  it should "return NOT FOUND with too many path elements" in {
    delete("/pdbs/9da0541a-d2c8-432e-8129-979a9830b427/") {
      status shouldBe SC_NOT_FOUND
      body should startWith("""Requesting "DELETE /pdbs/9da0541a-d2c8-432e-8129-979a9830b427/" on servlet "" but only have:""")
    }
  }

  private def createHttpException(code: Int) = {
    val headers = Map[String, String]("Status" -> "")
    val r = new HttpResponse[String]("", code, headers)
    // URL could be a vocabulary for the DDM class or addressing the bag store service
    HttpStatusException("getContent(url)", r)
  }
}
