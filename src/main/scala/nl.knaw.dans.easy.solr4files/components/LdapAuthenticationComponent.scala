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
import java.util
import java.util.Base64
import javax.naming.directory.{ SearchControls, SearchResult }
import javax.naming.ldap.{ InitialLdapContext, LdapContext }
import javax.naming.{ AuthenticationException, Context, NamingEnumeration }

import nl.knaw.dans.easy.solr4files.{ AuthorisationNotAvailableException, InvalidUserPasswordException }

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

trait LdapAuthenticationComponent extends AuthenticationComponent {
  val ldapContext: Try[LdapContext]
  val ldapUsersEntry: String
  val ldapProviderUrl: String

  trait LdapAuthentication extends Authentication {

    def getUser(userName: String, password: String): Try[User] = {

      def toUser(searchResult: SearchResult) = {
        def getAttrs(key: String) = {
          Option(searchResult.getAttributes.get(key)).map(
            _.getAll.asScala.toList.map(_.toString)
          ).getOrElse(Seq.empty)
        }
        val roles = getAttrs("easyRoles")
        User(userName,
          isArchivist = roles.contains("ARCHIVIST"),
          isAdmin = roles.contains("ADMIN"),
          groups = getAttrs("easyGroups")
        )
      }

      val hashedPassword = {
        val algorithm = "SHA"
        val md = MessageDigest.getInstance(algorithm.toUpperCase)
        md.update(password.getBytes)
        val base64 = Base64.getEncoder.encodeToString(md.digest)
        s"{$algorithm}$base64"
      }
      logger.info(s"looking for user [$userName] with hashedPassword [$hashedPassword] plain=[$password]")

      def validPassword: Try[InitialLdapContext] = Try {
        val env = new util.Hashtable[String, String]() {
          put(Context.PROVIDER_URL, ldapProviderUrl)
          put(Context.SECURITY_AUTHENTICATION, "simple")
          put(Context.SECURITY_PRINCIPAL, s"uid=$userName, $ldapUsersEntry")
          put(Context.SECURITY_CREDENTIALS, password)
          put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        }
        new InitialLdapContext(env, null)
      }.recoverWith {
        case t: AuthenticationException => Failure(InvalidUserPasswordException(userName, new Exception("invalid password", t)))
        case t => Failure(t)
      }

      def findUser(entries: NamingEnumeration[SearchResult]): Try[User] = {
        logger.info(s"looking up user attributes")
        entries.asScala.toList.headOption match {
          case Some(sr) =>
            logger.info(s"found user attributes")
            Success(toUser(sr))
          case None => Failure(InvalidUserPasswordException(userName, new Exception("not found")))
        }
      }

      val searchFilter = s"(&(objectClass=easyUser)(uid=$userName))"
      val searchControls = new SearchControls() {
        setSearchScope(SearchControls.SUBTREE_SCOPE)
      }
      (for {
        context <- ldapContext
        _ <- validPassword // TODO can we search on this one?
        _ = logger.info("validated password")
        entries <- Try(context.search(ldapUsersEntry, searchFilter, searchControls))
        user <- findUser(entries)
      } yield user).recoverWith {
        case t: InvalidUserPasswordException => Failure(t)
        case t => Failure(AuthorisationNotAvailableException(t))
      }
    }
  }
}
