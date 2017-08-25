package nl.knaw.dans.easy.solr4files.components

import java.net.{ URI, URL }

import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient

trait Solr {
  val solrUrl: URL
  lazy val solr: SolrClient = new HttpSolrClient.Builder("").build()
}
