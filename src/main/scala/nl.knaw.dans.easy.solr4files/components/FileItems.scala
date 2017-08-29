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

import java.net.URI

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.xml.{ Elem, Node }

class FileItems(xml: Elem, bag: Bag) extends DebugEnhancedLogging {
  private val fileNodes = xml \ "file"

  def openAccessTextFiles(): Seq[FileItem] = {
    fileNodes
      .filter(isOpenText)
      .map(toFileItem)
      .filter(_.isDefined) // file had attribute FilePath
      .map(_.get) // get is safe because of the filter
  }

  private def toFileItem(fileNode: Node): Option[FileItem] = {
    getPath(fileNode)
      .map(path =>
        new FileItem(bag.fileShas.get(path), bag.fileUrl(path), fileNode)
      ) // TODO catch exception of toURL, friendly message if SHA's are missing
  }


  private def getPath(fileNode: Node): Option[String] = {
    fileNode
      .attribute("filepath")
      .map(_.head.text.trim)
  }

  private def isOpenText(fileItem: Node): Boolean =
    hasTextFormat(fileItem) && !hasOtherAccessThanOpen(fileItem)

  private def hasTextFormat(fileItem: Node): Boolean =
    (fileItem \\ "format").text == "text/plain"

  private def hasOtherAccessThanOpen(fileItem: Node): Boolean = {
    val seq = fileItem \\ "accessRights"
    seq.nonEmpty && seq.text != "OPEN_ACCESS"
  }
}
