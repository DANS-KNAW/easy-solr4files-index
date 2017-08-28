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

    val stream = new ContentStreamBase.URLStream(item.url)

    // no values when mocking with the file system
    if (stream.getContentType == null) stream.setContentType(item.mimeType)
    if (stream.getSize == null) stream.setSize(new File(item.url.getPath).length)

    val req = new ContentStreamUpdateRequest("/update/extract")
    req.setWaitSearcher(false)
    req.setMethod(METHOD.POST)
    req.addContentStream(stream)
    req.setParam("literal.id", s"${ bag.bagId }/${ item.path }")
    req.setParam("literal.easy_file_path", item.path)
    req.setParam("literal.easy_file_mime_type", item.mimeType)
    req.setParam("literal.easy_dataset_id", bag.bagId)
    req.setParam("literal.easy_dataset_depositor_id", bag.getDepositor.get) // TODO friendly error message
    req.setParam("literal.easy_dataset_title", ddm.title) // TODO multiple?
    req.setParam("literal.easy_dataset_doi", ddm.doi)
    req.setParam("literal.easy_dataset_creator", ddm.creator) // TODO multiple?
    req.setParam("literal.easy_dataset_audience", ddm.audience) // TODO multiple?
    req.setParam("literal.easy_dataset_relation", ddm.relation) // TODO multiple?

    val map = solrClient.request(req).asShallowMap()
    logger.debug(s"${ map.values().toArray.mkString(", ") } ${ bag.bagId } ${ item.path }")
    s"updated ${ item.path } (${ bag.bagId })"
  }

  def deleteBag(bagId: String): Try[String] = Try {
    val query = new SolrQuery
    query.set("q", s"id:$bagId/*")
    solrClient.deleteByQuery(query.getQuery)
    s"deleted $bagId from the index"
  }
}
