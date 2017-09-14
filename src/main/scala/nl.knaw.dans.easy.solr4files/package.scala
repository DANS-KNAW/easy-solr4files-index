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
import java.net.{ URL, URLDecoder }

import nl.knaw.dans.lib.error.{ CompositeException, _ }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.FileUtils.readFileToString
import org.apache.solr.common.util.NamedList

import scala.collection.mutable
import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }
import scalaj.http.{ Http, HttpResponse }

package object solr4files extends DebugEnhancedLogging {

  type FeedBackMessage = String
  type SolrLiterals = Seq[(String, String)]
  type FileToShaMap = Map[String, String]

  case class WrappedCompositeException(msg: String, cause: CompositeException)
    extends Exception(s"$msg ${ cause.getMessage() }", cause)

  case class AnotherFailedException(msg: String, cause: Throwable)
    extends Exception(s"$msg; ${ cause.getMessage }", cause)

  case class HttpStatusException(msg: String, response: HttpResponse[String])
    extends Exception(s"$msg - ${ response.statusLine }, details: ${ response.body }")

  case class SolrStatusException(namedList: NamedList[AnyRef])
    extends Exception(s"solr update returned: ${ namedList.asShallowMap().values().toArray().mkString }")

  case class SolrCommitException(cause: Throwable)
    extends Exception(cause.getMessage, cause)

  abstract sealed class Submission(val solrId: String)
  case class SubmittedWithContent(override val solrId: String) extends Submission(solrId)
  case class SubmittedJustMetadata(override val solrId: String) extends Submission(solrId) {
    logger.warn(s"Resubmitted $solrId with just metadata")
  }

  implicit class RichTryStream[T](val left: Seq[Try[T]]) extends AnyVal {

    /** Typical usage: val (thrown, results) = ...toStream.map(TrySomething).takeUntilFailure */
    def takeUntilFailure: (Option[Throwable], Seq[T]) = {
      val b = mutable.ListBuffer[T]()
      val it = left.iterator
      while (it.hasNext) {
        val x = it.next()
        if (x.isFailure) return (Some(x.failed.get), b.toList)
        b += x.get
      }
      (None,b.toList)
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
        case response => Failure(HttpStatusException(s"getContent($left)", response))
      }
    }

    def getContentLength: Long = {
      if (left.getProtocol.toLowerCase == "file")
        Try(new File(left.getPath).length)
      else {
        Try(Http(left.toString).method("HEAD").asString).flatMap {
          case response if response.isSuccess => Try(response.headers("content-length").toLong)
          case response => Failure(HttpStatusException(s"getSize($left)", response))
        }
      }
    }.doIfFailure { case e => logger.warn(e.getMessage, e) }
      .getOrElse(-1L)
  }
}

