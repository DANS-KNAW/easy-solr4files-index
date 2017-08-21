package nl.knaw.dans.easy.solr4files.components

import java.net.URI
import java.nio.file.Path

import org.apache.commons.io.IOUtils.readLines

import scala.collection.JavaConverters._
import scala.util.{ Success, Try }
import scala.xml.{ Elem, XML }

trait Vault {

  def getStores(uri: URI): Try[Seq[URI]] = Try {
    val vault: URI = new URI(uri.getScheme + "://" + uri.getAuthority + "/") // TODO close
    readLines(uri.toURL.openStream()).asScala
      .map(line => {
        val trimmed = line.trim.replace("<", "").replace(">", "")
        val path = new URI(trimmed).getPath
        vault.resolve(path)
      }
      )
  }

  def getFilesXml(baseUri: URI): Try[Elem] = Try {
    XML.load(baseUri.resolve("metadata/files.xml").toURL.openStream()) // TODO close
  }


  def textFiles(filesXML: Elem): Try[Seq[Path]] = {
    // TODO filter on mime type
    println((filesXML \ "file").size)
    Success(Seq[Path]())
  }
}
