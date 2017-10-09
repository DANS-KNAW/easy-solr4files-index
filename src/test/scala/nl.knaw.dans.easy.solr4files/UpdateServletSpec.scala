package nl.knaw.dans.easy.solr4files

import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

class UpdateServletSpec extends TestSupportFixture
  with ServletFixture
  with ScalatraSuite
  with MockFactory {

  private class App extends EasyUpdateSolr4filesIndexApp(null)
  private val app = mock[App]
  addServlet(new EasyUpdateSolr4filesIndexServlet(app), "/*")

  "get /" should "return the message that the service is running" in {
    get("/") {
      status shouldBe 200
      body shouldBe "EASY File Index is running."
    }
  }
}
