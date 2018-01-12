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

import java.text.SimpleDateFormat

import org.joda.time.DateTime
import org.json4s.ext.{ EnumNameSerializer, JodaTimeSerializers }
import org.json4s.native.JsonMethods.parse
import org.json4s.{ DefaultFormats, Formats }

import scala.util.{ Failure, Try }

/**
 * @param itemId uuid of the bag + path of payload item from files.xml
 * @param owner  depositor of the bag
 */
case class AuthInfoItem(itemId: String,
                        owner: String,
                        dateAvailable: DateTime,
                        accessibleTo: RightsFor.Value,
                        visibleTo: RightsFor.Value
                       ) {
  val path: String = itemId.replaceAll("^[^/]+/","")
  val isAccessible: Boolean = {
    accessibleTo != RightsFor.NONE
  }
}

object AuthInfoItem {
  private implicit val jsonFormats: Formats = new DefaultFormats {
    override protected def dateFormatter: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
  } + new EnumNameSerializer(RightsFor) ++ JodaTimeSerializers.all

  def fromJson(input: String): Try[AuthInfoItem] = {
    Try(parse(input).extract[AuthInfoItem]).recoverWith { case t =>
      Failure(new Exception(s"parse error [${ t.getClass }: ${ t.getMessage }] for: $input", t))
    }
  }
}


