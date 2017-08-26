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

import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrInputDocument

import scala.util.Try

trait Solr {
  val solrUrl: URL
  lazy val solr: SolrClient = new HttpSolrClient.Builder(solrUrl.toString).build()

  def buildDoc(bag: Bag, ddm: DDM, item: FileItem): Try[SolrInputDocument] = Try{
    val doc = new SolrInputDocument()
    doc.addField("id",s"${bag.bagId}/${item.path}")
    doc.addField("file_path",item.path)
    doc.addField("file_mime_type",item.mimeType)
    doc.addField("dataset_id",bag.bagId)
    doc.addField("dataset_depositor_id",bag.getDepositor)
    doc.addField("dataset_title",ddm.title)
    doc.addField("dataset_doi",ddm.doi)
    doc.addField("dataset_creator",ddm.creator)
    doc.addField("dataset_audience",ddm.audience)
    doc.addField("dataset_relation",ddm.relation) // TODO multiple relations?
    // TODO add file content
    doc
  }

  def submit(solrDoc: SolrInputDocument): Try[Unit]= Try{
    val response = solr.add(solrDoc)
    response.getStatus // TODO error handling
    solr.commit()
  }
}
