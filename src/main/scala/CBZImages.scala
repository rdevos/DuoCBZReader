package be.afront.reader

import java.awt.image.BufferedImage
import java.util.zip.{ZipEntry, ZipFile}
import java.io.File
import javax.imageio.ImageIO
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using

class CBZImages(file: File) extends AutoCloseable {
  private val zipFile: ZipFile = new ZipFile(file)

  private val rootEntries: List[ZipEntry] = {
    zipFile.entries().asScala.toList
      .filter(entry => !entry.isDirectory && isImageEntry(entry.getName))
      .sortBy(_.getName)
  }

  val totalPages: Int = rootEntries.size

  val entries: List[String] = rootEntries.map(_.getName)

  private val cache: mutable.Map[Int, BufferedImage] = mutable.Map.empty

  def getImage(page: Int): BufferedImage = {

    if (page < 0 || page >= totalPages) {
      throw new IndexOutOfBoundsException(s"Page index $page out of bounds [0, ${totalPages - 1}]")
    }

    cache.getOrElseUpdate(page, {
      val entry = rootEntries(page)
      val img = Using(zipFile.getInputStream(entry)) { inputStream =>
        ImageIO.read(inputStream)
      }.getOrElse(null)

      if(img == null) 
        throw new ImageDecodingException("could not decode file "+entry);
      
      img
    })
  }

  override def close(): Unit = {
    zipFile.close()
  }

  private def isImageEntry(name: String): Boolean = {
    val lower = name.toLowerCase
    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")
  }
}