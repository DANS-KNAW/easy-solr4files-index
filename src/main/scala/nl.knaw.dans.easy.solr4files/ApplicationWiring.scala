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
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }
import scala.xml.Elem

/**
 * Initializes and wires together the components of this application.
 *
 * @param configuration the application configuration
 */
class ApplicationWiring(configuration: Configuration)
  extends DebugEnhancedLogging with Vault with Solr {

  // don't need resolve for solr, URL gives more early errors TODO perhaps not yet at service startup once implemented
  override val solrUrl: URL = new URL(configuration.properties.getString("solr.url", ""))
  override val vaultBaseUri: URI = new URI(configuration.properties.getString("vault.url", ""))

  def initAllStores(): Try[FeedBackMessage] = {
    getStoreNames
      .flatMap(updateStores)
      .map(results => s"Updated all bags of ${ results.size } stores ($vaultBaseUri)")
  }

  def initSingleStore(storeName: String): Try[StoreSubmitted] = {
    getBagIds(storeName)
      .flatMap(updateBags(storeName, _))
  }

  def update(storeName: String, bagId: String): Try[BagSubmitted] = {
    val bag = Bag(storeName, bagId, this)
    for {
      ddmXML <- bag.loadDDM
      ddm = new DDM(ddmXML)
      filesXML <- bag.loadFilesXML
      _ <- deleteBag(bag.bagId)
      feedbackMessage <- updateFiles(bag, ddm, filesXML)
      _ <- commit()
    } yield feedbackMessage
  }.recoverWith {
    case t: SolrStatusException => commitAnyway(t) // just the delete
    case t: SolrUpdateException => commitAnyway(t) // just the delete
    case t: MixedResultsException => commitAnyway(t) // delete and some files
    case t => Failure(t)
  }

  def delete(bagId: String): Try[FeedBackMessage] = {
    for {
      _ <- deleteBag(bagId)
      _ <- commit()
    } yield s"Deleted file documents for bag $bagId"
  }.recoverWith {
    case t: SolrCommitException => Failure(t)
    case t =>
      commit()
      Failure(t)
  }

  private def commitAnyway(t: Throwable): Try[Nothing] = {
    commit()
      .map(_ => Failure(t))
      .getOrRecover(t2 => Failure(new CompositeException(t2, t)))
  }

  /**
   * The number of bags submitted per store are logged as info
   * if and when another store in the vault failed.
   */
  private def updateStores(storeNames: Seq[String]): Try[FeedBackMessage] = {
    lazy val stats = s"Updated ${ storeNames.size } stores"
    storeNames.toStream.map(initSingleStore).takeUntilFailure match {
      case (None, _) => Success(stats)
      case (Some(t), Seq()) => Failure(t)
      case (Some(t), results) =>
        results.foreach(fb => logger.info(fb.getMessage))
        Failure(MixedResultsException(stats, results, t))
    }
  }

  /**
   * The number of files submitted with or without content per bag are logged as info
   * if and when another bag in the same store failed.
   */
  private def updateBags(storeName: String, bagIds: Seq[String]): Try[StoreSubmitted] = {
    lazy val stats = s"Updated ${ bagIds.size } bags for $storeName "
    bagIds.toStream.map(update(storeName, _)).takeUntilFailure match {
      case (Some(t), Seq()) => Failure(t)
      case (Some(t), results) => Failure(MixedResultsException(stats, results, t))
      case (None, results) =>
        results.foreach(fb => logger.info(fb.getMessage))
        Success(StoreSubmitted(stats, results))
    }
  }

  /**
   * Files submitted without content are logged immediately as warning.
   * Files submitted with content are logged as info
   * if and when another file in the same bag failed.
   */
  private def updateFiles(bag: Bag, ddm: DDM, filesXML: Elem): Try[BagSubmitted] = {
    lazy val statsPrefix = s"Bag ${ bag.bagId }: "
    (filesXML \ "file")
      .map(FileItem(bag, ddm, _))
      .filter(_.shouldIndex)
      .toStream
      .map(f => createDoc(f, getSize(f.bag.storeName, f.bag.bagId, f.path)))
      .takeUntilFailure match {
      case (Some(t), Seq()) => Failure(t)
      case (None, results) => Success(BagSubmitted(statsPrefix, results))
      case (Some(t), results) =>
        results
          .withFilter(_.isInstanceOf[FilesSubmittedWithContent])
          .foreach(x => logger.info(x.toString))
        Failure(MixedResultsException(statsPrefix, results, t))
    }
  }
}
