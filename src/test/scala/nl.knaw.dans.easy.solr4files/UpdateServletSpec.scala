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

  "post /update/:store/:uuid" should "return the number of updated files" in {
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

  it should "return NOT FOUND if something is not found for the n-th file, bag or store" in {
    // TODO check if exceptions from RichUrl.getContent indeed bubble up
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

  private def createHttpException(code: Int) = {
    val headers = Map[String, String]("Status" -> "")
    val r = new HttpResponse[String]("", code, headers)
    // URL could be a vocabulary for the DDM class or addressing the bag store service
    HttpStatusException("getContent(url)", r)
  }
}
