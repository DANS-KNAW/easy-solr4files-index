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

import java.security.MessageDigest
import java.util.Base64
import javax.naming.directory.{ SearchControls, SearchResult }
import javax.naming.ldap.LdapContext

import nl.knaw.dans.easy.solr4files.{ AuthorisationNotAvailableException, InvalidUserPasswordException }

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

trait LdapAuthenticationComponent extends AuthenticationComponent {
  val ldapContext: Try[LdapContext]
  val ldapUsersEntry: String

  trait LdapAuthentication extends Authentication {

    def getUser(userName: String, password: String): Try[User] = {

      def toUser(searchResult: SearchResult) = {
        val roles = searchResult.getAttributes.get("easyRoles")
        User(userName,
          isArchivist = roles.contains("ARCHIVIST"),
          isAdmin = roles.contains("ADMIN"),
          groups = searchResult.getAttributes.get("easyGroups")
            .getAll.asScala.toList.map(_.toString)
        )
      }

      val hashedPassword = {
        val algorithm = "SHA"
        val md = MessageDigest.getInstance(algorithm.toUpperCase)
        md.update(password.getBytes)
        val base64 = Base64.getEncoder.encodeToString(md.digest)
        s"{$algorithm}$base64"
      }
      logger.info(s"looking for user [$userName] with hashedPassword [$hashedPassword]")

      val searchFilter = s"(&(objectClass=easyUser)(uid=$userName))"
      val searchControls = new SearchControls() {
        setSearchScope(SearchControls.SUBTREE_SCOPE)
      }

      def findUser(entries: Iterator[SearchResult]): Try[User] = {
        entries.toList.headOption
          .filter(_.getAttributes.get("userPassword").contains(hashedPassword)) match {
          case Some(sr) => Success(toUser(sr))
          case None => Failure(InvalidUserPasswordException(userName, new Exception("not found")))
        }
      }

      (for {
        context <- ldapContext
        entries <- Try(context.search(ldapUsersEntry, searchFilter, searchControls).asScala)
        user <- findUser(entries)
      } yield user).recoverWith {
        case t: InvalidUserPasswordException => Failure(t)
        case t => Failure(AuthorisationNotAvailableException(t))
      }
    }
  }
}
