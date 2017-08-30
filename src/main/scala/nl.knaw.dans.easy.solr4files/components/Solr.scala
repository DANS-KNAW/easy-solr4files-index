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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.solr.client.solrj.SolrRequest.METHOD
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest
import org.apache.solr.client.solrj.{ SolrClient, SolrQuery }
import org.apache.solr.common.util.ContentStreamBase

import scala.util.Try

trait Solr {
  this: DebugEnhancedLogging =>
  val solrUrl: URL
  lazy val solrClient: SolrClient = new HttpSolrClient.Builder(solrUrl.toString).build()


  def createDoc(bag: Bag, ddm: DDM, item: FileItem): Try[String] = Try {

    val solrDocId = s"${ bag.bagId }/${ item.path }"

    val stream = new ContentStreamBase.URLStream(item.url)
    //stream.getStream.close() // side-effect: initializes Size TODO vault doesn't return proper ContentType
    if (stream.getContentType == null) stream.setContentType(item.mimeType)
    if (stream.getSize == null) stream.setSize(new File(item.url.getPath).length)

    val req = new ContentStreamUpdateRequest("/update/extract")
    req.setWaitSearcher(false)
    req.setMethod(METHOD.POST)
    req.addContentStream(stream)
    req.setParam("literal.id", solrDocId)
    req.setParam("literal.easy_file_size", stream.getSize.toString)
    (bag.solrLiterals ++ ddm.solrLiterals ++ item.solrLiterals)
      .foreach { case (key, value) =>
        req.setParam(s"literal.easy_$key", value)
      }

    val namedList = solrClient.request(req)
    logger.debug(s"${ namedList.asShallowMap().values().toArray.mkString } $solrDocId")
    s"updated $solrDocId"
  }

  def deleteBag(bagId: String): Try[String] = Try {
    val query = new SolrQuery
    query.set("q", s"id:$bagId/*")
    solrClient.deleteByQuery(query.getQuery)
    s"deleted $bagId from the index"
  }
}
