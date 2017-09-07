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

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.solr4files.FeedBackMessage
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.solr.client.solrj.SolrRequest.METHOD
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest
import org.apache.solr.client.solrj.{ SolrClient, SolrQuery }
import org.apache.solr.common.util.ContentStreamBase

import scala.util.{ Failure, Success, Try }

trait Solr {
  this: DebugEnhancedLogging =>
  val solrUrl: URL
  lazy val solrClient: SolrClient = new HttpSolrClient.Builder(solrUrl.toString).build()


  def createDoc(bag: Bag, ddm: DDM, item: FileItem): Try[FeedBackMessage] = Try {
    val solrDocId = s"${ bag.bagId }/${ item.path }"

    val stream = new ContentStreamBase.URLStream(item.url)
    stream.getStream.close() // side-effect: initializes Size TODO vault doesn't return proper ContentType
    if (stream.getContentType == null) stream.setContentType(item.mimeType)
    if (stream.getSize == null) stream.setSize(new File(item.url.getPath).length)

    val req = new ContentStreamUpdateRequest("/update/extract")
    req.setWaitSearcher(false)
    req.setMethod(METHOD.POST)
    req.addContentStream(stream)
    req.setParam("literal.id", solrDocId)
    req.setParam("literal.easy_file_size", stream.getSize.toString)
    (bag.solrLiterals ++ ddm.solrLiterals ++ item.solrLiterals)
      .foreach { case (key, value) if value.trim.nonEmpty =>
        req.setParam(s"literal.easy_$key", value.replaceAll("\\s+", " ").trim)
      }

    executeUpdate(req) match {
      case Success(()) => s"updated ${ s"$solrDocId" }"
      case Failure(_) =>
        req.getContentStreams.clear() // retry with just metadata
        executeUpdate(req) match {
          case Failure(e) => throw e
          case Success(()) =>
            logger.error(s"Failed to submit $solrDocId with content, successfully retried with just metadata")
            s"update retried ${ s"$solrDocId" }"
        }
    }
  }

  private def executeUpdate(req: ContentStreamUpdateRequest): Try[Unit] = {
    Try {
      val namedList = solrClient.request(req)
      val status = namedList.get("status")
      if (status != null && status != "0")
        throw new Exception(s"solr update returned: ${ namedList.asShallowMap().values().toArray().mkString }")
    }
  }

  def deleteBag(bagId: String): Try[FeedBackMessage] = Try {
    val query = new SolrQuery
    query.set("q", s"id:$bagId/*")
    solrClient.deleteByQuery(query.getQuery)
    s"deleted $bagId from the index"
  }

  def commit(): Try[Unit] = {
    Try(solrClient.commit())
  }
}
