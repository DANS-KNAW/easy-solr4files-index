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

import org.scalatest.Inside.inside
import org.scalatest.{ FlatSpec, Matchers }

import scala.util.Success

class BagSpecs extends FlatSpec with Matchers {

  private val bag = Bag("pdbs", "9da0541a-d2c8-432e-8129-979a9830b427", MockedVault("vault"))

  "loadXml" should "load files.xml" in {
    bag.loadFilesXML shouldBe a[Success[_]]
  }

  "getFileShas" should "read the shas of the files" in {
    inside(bag.getFileShas) {
      case Success(shas) => shas.keys.size shouldBe 9
    }
  }
}
