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
package nl.knaw.dans.easy

import nl.knaw.dans.lib.error.CompositeException

import scala.util.{ Failure, Success, Try }

package object solr4files {

  type FeedBackMessage = String
  type FileToShaMap = Map[String, String]
  type SolrLiterals = Seq[(String, String)]

  def FileToShaMap(xs: (String, String)*): FileToShaMap = Seq(xs: _*).toMap

  abstract sealed class Submission(solrId: String)
  case class SubmittedWithContent(solrId: String) extends Submission(solrId)
  case class SubmittedJustMetadata(solrId: String) extends Submission(solrId)

  implicit class mixedResults(val left: Seq[Try[Submission]]) extends AnyVal {
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

  implicit class TryExtensions2[T](val t: Try[T]) extends AnyVal {
    // TODO candidate for dans-scala-lib
    def unsafeGetOrThrow: T = {
      t match {
        case Success(value) => value
        case Failure(throwable) => throw throwable
      }
    }
  }
}

