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

import org.apache.http.HttpStatus
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrInputDocument

import scala.util.Try

trait Solr {
  val solrUrl: URL
  lazy val solrClient: SolrClient = new HttpSolrClient.Builder(solrUrl.toString).build()

  def createDoc(bag: Bag, ddm: DDM, item: FileItem): Try[String] = Try {
    val doc = new SolrInputDocument()
    doc.addField("id", s"${ bag.bagId }/${ item.path }")
    doc.addField("content", bag.vaultIO.linesFrom(bag.storeName, bag.bagId, item.path).mkString("\n"))
    doc.addField("content_type", item.mimeType)

    doc.addField("easy_file_path", item.path)
    doc.addField("easy_file_mime_type", item.mimeType)
    doc.addField("easy_dataset_id", bag.bagId)
    doc.addField("easy_dataset_depositor_id", bag.getDepositor.get) // TODO friendly error message
    doc.addField("easy_dataset_title", ddm.title) // TODO multiple?
    doc.addField("easy_dataset_doi", ddm.doi)
    doc.addField("easy_dataset_creator", ddm.creator) // TODO multiple?
    doc.addField("easy_dataset_audience", ddm.audience) // TODO multiple?
    doc.addField("easy_dataset_relation", ddm.relation) // TODO multiple?
    solrClient.add(doc).getStatus match {
      case 0 => // no response header
      case HttpStatus.SC_OK =>
      case status => throw new Exception(s"Update of bag ${ bag.bagId } returned status $status")
    }
    commit("update")
    s"updated ${ item.path } (${ bag.bagId })"
  }

  def deleteBag(bagId: String): Try[String] = Try {
    import org.apache.solr.client.solrj.SolrQuery
    val query = new SolrQuery
    query.set("dataset_id", bagId)
    solrClient.deleteByQuery(query.getQuery).getStatus match {
      case 0 => // no response header
      case HttpStatus.SC_OK =>
      case status => throw new Exception(s"Delete of bag $bagId returned status $status")
    }
    commit("delete")
    s"deleted $bagId from the index"
  }

  private def commit(description: String): Unit = {
    solrClient.commit().getStatus match {
      case 0 => // no response header
      case HttpStatus.SC_OK =>
      case status => throw new Exception(s"Commit of $description returned status $status")
    }
  }
}
