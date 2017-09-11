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
package nl.knaw.dans.easy.solr4files.components

import java.net.URL

import nl.knaw.dans.easy.solr4files._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.solr.client.solrj.SolrRequest.METHOD
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest
import org.apache.solr.client.solrj.{ SolrClient, SolrQuery }
import org.apache.solr.common.util.{ ContentStreamBase, NamedList }

import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

trait Solr extends DebugEnhancedLogging {
  val solrUrl: URL
  lazy val solrClient: SolrClient = new HttpSolrClient.Builder(solrUrl.toString).build()

  def createDoc(item: FileItem, size: Long): Try[Submission] = {
    val solrDocId = s"${ item.bag.bagId }/${ item.path }"
    val request = new ContentStreamUpdateRequest("/update/extract") {
      setWaitSearcher(false)
      setMethod(METHOD.POST)
      addContentStream(new ContentStreamBase.URLStream(item.bag.fileUrl(item.path)))
      setParam("literal.id", solrDocId)
      setParam("literal.easy_file_size", size.toString)
      for (
        (key, value) <- item.bag.solrLiterals ++ item.ddm.solrLiterals ++ item.solrLiterals
      ) {
        if (value.trim.nonEmpty)
          setParam(s"literal.easy_$key", value.replaceAll("\\s+", " ").trim)
      }
    }
    submitRequest(solrDocId, request)
  }


  private def submitRequest(solrDocId: String, req: ContentStreamUpdateRequest): Try[Submission] = {
    executeUpdate(req)
      .map(_ => SubmittedWithContent(solrDocId))
      .recoverWith { case t =>
        logger.warn(s"First submit attempt of $solrDocId failed with ${ t.getMessage }", t)
        req.getContentStreams.clear() // retry with just metadata
        executeUpdate(req).map { _ =>
          logger.error(s"Failed to submit $solrDocId with content, successfully retried with just metadata")
          SubmittedJustMetadata(solrDocId)
        }
      }
  }

  private def executeUpdate(req: ContentStreamUpdateRequest): Try[Unit] = {
    for {
      namedList <- Try(solrClient.request(req))
      status = Option(namedList.get("status")).withFilter("0" !=)
      _ <- status.map(_ => statusError(namedList)).getOrElse(Success(()))
    } yield ()
  }

  private def statusError(namedList: NamedList[AnyRef]) = {
    Failure(new Exception(s"solr update returned: ${ namedList.asShallowMap().values().toArray().mkString }"))
  }

  def deleteBag(bagId: String): Try[FeedBackMessage] = Try {
    solrClient.deleteByQuery(new SolrQuery {
      set("q", s"id:$bagId/*")
    }.getQuery)
    s"deleted $bagId from the index"
  }

  def commit(): Try[Unit] = {
    Try(solrClient.commit())
  }
}
