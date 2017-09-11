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

import java.net.URL

import nl.knaw.dans.lib.error.CompositeException
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }
import scalaj.http.Http
import nl.knaw.dans.lib.error._

package object solr4files extends DebugEnhancedLogging {

  type FeedBackMessage = String
  type FileToShaMap = Map[String, String]
  type SolrLiterals = Seq[(String, String)]

  def FileToShaMap(xs: (String, String)*): FileToShaMap = Seq(xs: _*).toMap

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

      if (total == left.size)
        Success(stats)
      else {
        val t = CompositeException(failures)
        Failure(new Exception(s"$stats, another ${ t.getMessage }", t))
      }
    }
  }

  implicit class RichURL(val left: URL) {

    def loadXml: Try[Elem] = Try {
      resource.managed(left.openStream()).acquireAndGet(XML.load) // TODO replace
    }

    def readLines: Try[Seq[String]] = Try {
      resource.managed(left.openStream()) // TODO replace
        .acquireAndGet(IOUtils.readLines)
        .asScala
    }

    def getContentLength: Long = {
      Http(left.toString).method("HEAD").asString match {
        case response if !response.isSuccess =>
          logger.warn(s"getSize($left) ${ response.statusLine }, details: ${ response.body }")
          -1L
        case response =>
          Try(response.headers("content-length").toLong)
            .doIfFailure { case e => logger.warn(s"getSize($left) content-length: ${ e.getMessage }", e) }
            .getOrElse(-1L)
      }
    }
  }
}

