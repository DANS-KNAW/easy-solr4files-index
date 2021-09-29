/*
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

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.logging.servlet._
import nl.knaw.dans.lib.string._
import org.apache.http.HttpStatus._
import org.scalatra._
import scalaj.http.HttpResponse

import scala.util.Try

class UpdateServlet(app: EasySolr4filesIndexApp) extends ScalatraServlet
  with ServletLogger
  with PlainLogFormatter
  with LogResponseBodyOnError
  with DebugEnhancedLogging {
  logger.info("File index update servlet running...")

  private def respond(result: Try[String]): ActionResult = {
    val msgPrefix = "Log files should show which actions succeeded. Finally failed with: "
    result.map(Ok(_))
      .doIfFailure { case e => logger.error(e.getMessage, e) }
      .getOrRecover {
        case SolrBadRequestException(message, _) => BadRequest(message) // delete or search only
        case HttpStatusException(message, HttpResponse(_, SC_NOT_FOUND, _)) => NotFound(message)
        case HttpStatusException(message, HttpResponse(_, SC_SERVICE_UNAVAILABLE, _)) => ServiceUnavailable(message)
        case HttpStatusException(message, HttpResponse(_, SC_REQUEST_TIMEOUT, _)) => RequestTimeout(message)
        case MixedResultsException(_, HttpStatusException(message, HttpResponse(_, SC_NOT_FOUND, _))) => NotFound(msgPrefix + message)
        case MixedResultsException(_, HttpStatusException(message, HttpResponse(_, SC_SERVICE_UNAVAILABLE, _))) => ServiceUnavailable(msgPrefix + message)
        case MixedResultsException(_, HttpStatusException(message, HttpResponse(_, SC_REQUEST_TIMEOUT, _))) => RequestTimeout(msgPrefix + message)
        case t => InternalServerError(t.getMessage) // for an internal servlet we can and should expose the cause
      }
  }

  private def getUUID = {
    params("uuid").toUUID.toTry
  }

  private def badUuid(e: Throwable) = {
    BadRequest(e.getMessage)
  }

  post("/update/:store/:uuid") {
    getUUID
      .map(uuid => respond(app.update(params("store"), uuid).map(_.msg)))
      .getOrRecover(badUuid)
  }

  post("/init") {
    respond(app.initAllStores())
  }

  post("/init/:store") {
    respond(app.initSingleStore(params("store")).map(_.msg))
  }

  delete("/:store/:uuid") {
    getUUID
      .map(uuid => respond(app.delete(s"easy_dataset_id:$uuid")))
      .getOrRecover(badUuid)
  }

  delete("/:store") {
    respond(app.delete(s"easy_dataset_store_id:${ params("store") }"))
  }

  delete("/") {
    params.get("q")
      .map(q => respond(app.delete(q)))
      .getOrElse(BadRequest("delete requires param 'q', got " + params.asString))
  }
}
