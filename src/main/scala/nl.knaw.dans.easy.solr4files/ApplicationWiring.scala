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
package nl.knaw.dans.easy.solr4files

import java.net.{ URI, URL }

import nl.knaw.dans.easy.solr4files.components._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient

/**
 * Initializes and wires together the components of this application.
 */
trait ApplicationWiring
  extends DebugEnhancedLogging
    with Vault
    with Solr
    with AuthorisationComponent
    with HttpWorkerComponent
    with AuthenticationComponent {

  val configuration: Configuration

  override val authentication: Authentication = new Authentication {
    override val ldapUsersEntry: String = configuration.properties.getString("ldap.users-entry")
    override val ldapProviderUrl: String = configuration.properties.getString("ldap.provider.url")
  }
  override val authorisation: Authorisation = new Authorisation {
    override val baseUri: URI = new URI(configuration.properties.getString("auth-info.url"))
  }
  override val http: HttpWorker = new HttpWorker {}

  // don't need resolve for solr, URL gives more early errors TODO perhaps not yet at service startup once implemented
  private val solrUrl: URL = new URL(configuration.properties.getString("solr.url", "http://localhost"))
  override val solrClient: SolrClient = new HttpSolrClient.Builder(solrUrl.toString).build()
  override val vaultBaseUri: URI = new URI(configuration.properties.getString("vault.url", "http://localhost"))
  //TODO BagStoreComponent using HttpWorker as in easy-download
  override val maxFileSizeToExtractContentFrom: Double = configuration.properties.getString("max-fileSize-toExtract-content-from", (64 * 1024 * 1024).toString).toDouble
}
