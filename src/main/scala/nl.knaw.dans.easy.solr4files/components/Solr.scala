package nl.knaw.dans.easy.solr4files.components

import java.net.URL

import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.common.SolrInputDocument

import scala.util.Try

trait Solr {
  val solrUrl: URL
  lazy val solr: SolrClient = new HttpSolrClient.Builder("").build()

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
    doc
  }

  def submit(solrDoc: SolrInputDocument): Try[Unit]= Try{
    val response = solr.add(solrDoc)
    response.getStatus // TODO error handling
    solr.commit()
  }
}
