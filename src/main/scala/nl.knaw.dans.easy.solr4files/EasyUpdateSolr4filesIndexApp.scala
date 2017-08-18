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

import java.net.URL

import scala.util.{ Failure, Success, Try }

class EasyUpdateSolr4filesIndexApp(wiring: ApplicationWiring) extends AutoCloseable {

  def init(storeUrl: URL): Try[String] =
    Failure(new NotImplementedError(s"init not implemented ($storeUrl)"))

  def add(baseUrl: URL): Try[String] =
    Failure(new NotImplementedError(s"add not implemented ($baseUrl)"))

  final def update(baseUrl: URL): Try[String] = for {
    _ <- delete(baseUrl)
    s <- add(baseUrl)
  } yield s

  def delete(baseUrl: URL): Try[String] =
    Failure(new NotImplementedError(s"delete not implemented ($baseUrl)"))

  def init(): Try[Unit] = {
    // Do any initialization of the application here. Typical examples are opening
    // databases or connecting to other services.
    Success(())
  }

  override def close(): Unit = {

  }
}