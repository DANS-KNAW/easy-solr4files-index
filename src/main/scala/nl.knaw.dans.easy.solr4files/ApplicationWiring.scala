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
import nl.knaw.dans.lib.error.{ CompositeException, TraversableTryExtensions }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }

/**
 * Initializes and wires together the components of this application.
 *
 * @param configuration the application configuration
 */
class ApplicationWiring(configuration: Configuration)
  extends DebugEnhancedLogging with VaultIO with Vault with Solr {

  // don't need resolve for solr, URL gives more early errors TODO perhaps not enough early errors
  override val solrUrl: URL = new URL(configuration.properties.getString("solr.url", ""))
  override val vaultBaseUri: URI = new URI(configuration.properties.getString("vault.url", ""))

  def initAllStores(): Try[FeedBackMessage] = {
    getStoreNames
      .flatMap(updateStores)
      .map(results => s"Updated all bags of ${ results.size } stores ($vaultBaseUri)")
  }

  def initSingleStore(storeName: String): Try[FeedBackMessage] = {
    getBagIds(storeName)
      .flatMap(updateBags(storeName, _))
      .map(results => s"Updated ${ results.size } bags of one store ($storeName)")
  }

  def update(storeName: String, bagId: String): Try[FeedBackMessage] = {
    val bag = Bag(storeName, bagId, this)
    for {
      ddmXML <- bag.loadDDM
      ddm = new DDM(ddmXML)
      filesXML <- bag.loadFilesXML
      files = (filesXML \ "file").map(FileItem(bag, ddm, _)).filter(_.shouldIndex)
      _ <- deleteBag(bag.bagId)
      feedbackMessage <- files.map(createDoc(bag, ddm, _)).collectResults(bag.bagId)
      _ <- commit()
    } yield feedbackMessage
  }

  def delete(bagId: String): Try[FeedBackMessage] = for {
    _ <- deleteBag(bagId)
    _ <- commit()
  } yield s"Deleted file documents for bag $bagId"

  private def updateStores(storeNames: Seq[String]): Try[Seq[FeedBackMessage]] = {
    storeNames
      .map(initSingleStore)
      .collectResults
      .recoverWith { case t: CompositeException =>
        throw new Exception(s"Tried to update ${ storeNames.size } stores, ${ t.getMessage() }", t)
      }
  }

  private def updateBags(storeName: String, bagIds: Seq[String]): Try[Seq[FeedBackMessage]] = {
    bagIds
      .map(uuid => update(storeName, uuid))
      .collectResults
      .recoverWith { case t: CompositeException =>
        throw new Exception(s"Tried to update ${ bagIds.size } bags, ${ t.getMessage() }", t)
      }
  }

  private implicit class mixedResults(left: Seq[Try[Submission]]) {
    def collectResults(bagId: String): Try[FeedBackMessage] = {
      val (withContentCount, justMetadataCount, failures) = left.foldRight((0, 0, List.empty[Throwable])) {
        case (Success(SubmittedWithContent(_)), (withContent, justMetadata, es)) => (withContent + 1, justMetadata, es)
        case (Success(SubmittedJustMetadata(_)), (withContent, justMetadata, es)) => (withContent, justMetadata + 1, es)
        case (Failure(e), (withContent, justMetadata, es)) => (withContent, justMetadata, e :: es)
      }
      val total = withContentCount + justMetadataCount
      val stats = s"Bag $bagId: updated $total files, $justMetadataCount of them without content"

      if (total == left.size)
        Success(stats)
      else {
        val t = CompositeException(failures)
        Failure(new Exception(s"$stats, another ${ t.getMessage }", t))
      }
    }
  }
}
