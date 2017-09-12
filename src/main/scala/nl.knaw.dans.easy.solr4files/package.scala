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
package nl.knaw.dans.easy

import java.io.File
import java.net.{URL, URLDecoder}

import nl.knaw.dans.lib.error.{CompositeException, _}
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.FileUtils.readFileToString

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, XML}
import scalaj.http.{Http, HttpResponse}

package object solr4files extends DebugEnhancedLogging {

  type FeedBackMessage = String
  type SolrLiterals = Seq[(String, String)]

  type FileToShaMap = Map[String, String]
  def FileToShaMap(xs: (String, String)*): FileToShaMap = Seq(xs: _*).toMap

  case class WrappedCompositeException(msg: String, cause: CompositeException )
    extends Exception(s"$msg ${cause.getMessage()}", cause)

  case class HttpStatusException(msg: String, response: HttpResponse[String] )
    extends Exception(s"$msg - ${ response.statusLine }, details: ${ response.body }")

  abstract sealed class Submission(solrId: String)
  case class SubmittedWithContent(solrId: String) extends Submission(solrId)
  case class SubmittedJustMetadata(solrId: String) extends Submission(solrId)

  implicit class MixedResults(val left: Seq[Try[Submission]]) extends AnyVal {
    def collectResults(bagId: String): Try[FeedBackMessage] = {
      val (withContentCount, justMetadataCount, failures) = left.foldRight((0, 0, List.empty[Throwable])) {
        case (Success(SubmittedWithContent(_)), (withContent, justMetadata, es)) => (withContent + 1, justMetadata, es)
        case (Success(SubmittedJustMetadata(_)), (withContent, justMetadata, es)) => (withContent, justMetadata + 1, es)
        case (Failure(e), (withContent, justMetadata, es)) => (withContent, justMetadata, e :: es)
      }
      val total = withContentCount + justMetadataCount
      val stats = s"Bag $bagId: updated $total files, $justMetadataCount of them without content"
      if (total == left.size) Success(stats)
      else Failure(WrappedCompositeException(s"$stats, another", CompositeException(failures)))
    }
  }

  implicit class RichURL(val left: URL) {

    def loadXml: Try[Elem] = {
      getContent.flatMap(s => Try(XML.loadString(s)))
    }

    def readLines: Try[Seq[String]] = {
      getContent.map(_.split("\n"))
    }

    private def getContent: Try[String] = {
      if (left.getProtocol.toLowerCase == "file") {
        val path = URLDecoder.decode(left.getPath, "UTF8")
        Try(readFileToString(new File(path), "UTF8"))
      }
      else Try(Http(left.toString).method("GET").asString).flatMap {
        case response if response.isSuccess => Success(response.body)
        case response => Failure(HttpStatusException(s"getSize($left)", response))
      }
    }

    def getContentLength: Long = {
      if (left.getProtocol.toLowerCase == "file")
        Try(new File(left.getPath).length).getOrElse(-1L)
      else Try(Http(left.toString).method("HEAD").asString).map {
        case response if !response.isSuccess =>
          logger.warn(s"getSize($left) ${ response.statusLine }, details: ${ response.body }")
          -1L
        case response =>
          Try(response.headers("content-length").toLong)
            .doIfFailure { case e => logger.warn(s"getSize($left) content-length: ${ e.getMessage }", e) }
            .getOrElse(-1L)
      }.getOrElse(-1L)
    }
  }
}

