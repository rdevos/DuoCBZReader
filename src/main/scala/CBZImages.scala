/*
  Copyright 2025 Paul Janssens

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package be.afront.reader

import CBZImages.{getHalf, isJPGStartOfFrameMarker, readBigEndianInt16, readLittleEndianInt16}

import java.awt.image.BufferedImage
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipFile}

import java.awt.Graphics2D
import java.io.{BufferedInputStream, File, InputStream}
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

class CBZImages(file: File) extends AutoCloseable {

  private val zipFile: ZipFile = ZipFile.builder()
    .setFile(file)
    .get()
  
  private val rootEntries: List[ZipArchiveEntry] = {
    zipFile.getEntries.asScala.toList
      .filter(entry => !entry.isDirectory && isImageEntry(entry.getName))
      .sortBy(_.getName)
  }

  type EntryName = String

  private val rawPages: Int = rootEntries.size

  private val entries: List[EntryName] = rootEntries.map(_.getName)

  private val aspectRatios: Map[EntryName, Double] = {
    rootEntries
      .flatMap { entry =>
        Try {
          val is = zipFile.getInputStream(entry)
          Using(new BufferedInputStream(is)) { bis =>
            val buffer = new Array[Byte](8*1024)
            val bytesRead = bis.read(buffer)
            parseDimensions(entry.getName.toLowerCase, buffer, bytesRead)
          }.get
        }.toOption.flatten.map { case (w, h) =>
          entry.getName -> (w.toDouble / h.toDouble)
        }
      }.toMap
  }

  private val widepages: Int = aspectRatios.values.count(_ > 1)

  private val wideIndices: Set[Int] = entries.indices
    .filter(i => aspectRatios.get(entries(i)).exists(_ > 1))
    .toSet

  val totalPages: Int = rawPages + widepages

  private val pageMap: Map[Int, (Int, Option[Int])] = {
    entries.indices.flatMap { rawIdx =>
      if (wideIndices.contains(rawIdx)) {
        Seq((rawIdx, Some(0)), (rawIdx, Some(1)))
      } else {
        Seq((rawIdx, None))
      }
    }.zipWithIndex.map { case ((rawIdx, side), pageIdx) =>
      pageIdx -> (rawIdx, side)
    }.toMap
  }

  private def parseDimensions(name: String, buffer: Array[Byte], len: Int): Option[(Int, Int)] = {
    if (name.endsWith(".png") || buffer.startsWith(Array(0x89.toByte, 'P', 'N', 'G'))) {
      // PNG: Width at 16-19 (big-endian), height 20-23
      val bb = ByteBuffer.wrap(buffer, 16, 8)
      Some(bb.getInt, bb.getInt)
    } else if (name.endsWith(".gif") || buffer.startsWith("GIF".getBytes)) {
      // GIF: Width at 6-7 (little-endian), height 8-9
      val width = readLittleEndianInt16(buffer, 6)
      val height = readLittleEndianInt16(buffer, 8)
      Some(width, height)
    } else if (name.endsWith(".jpg") || name.endsWith(".jpeg") || buffer.startsWith(Array(0xFF.toByte, 0xD8.toByte))) {
      // JPEG: Scan for SOF marker
      var pos = 2
      while (pos < len - 5) {

        if (isJPGStartOfFrameMarker(buffer, pos)) {
          val height = readBigEndianInt16(buffer, pos + 5)
          val width = readBigEndianInt16(buffer, pos + 7)
          return Some(width, height)
        }
        val sizeWithHeader = 2+ readBigEndianInt16(buffer, pos + 2)
        pos += sizeWithHeader
      }
      None
    } else {
      None
    }
  }

  private val cache: mutable.Map[Int, BufferedImage] = mutable.Map.empty

  def partiallyClearCache():Unit =
    pageMap.collect { case (page, (raw, Some(side))) => page }.foreach(cache.remove)

  def getImage(page: Int, direction:Int): BufferedImage = {

    if (page < 0 || page >= totalPages) {
      throw new IndexOutOfBoundsException(s"Page index $page out of bounds [0, ${totalPages - 1}]")
    }

    cache.getOrElseUpdate(page, {
      val (rawIdx, optSide) = pageMap(page)
      val entry = rootEntries(rawIdx)
      val fullImg = Using(zipFile.getInputStream(entry)) { inputStream =>
        ImageIO.read(inputStream)
      }.getOrElse(null)
      val img = optSide match {
        case None => fullImg
        case Some(side) => getHalf(fullImg, side, direction)
      }

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

object CBZImages {

  def readBigEndianInt16(bytes: Array[Byte], offset: Int = 0): Int =
    ((bytes(offset) & 0xFF) << 8) | (bytes(offset + 1) & 0xFF)

  def readLittleEndianInt16(bytes: Array[Byte], offset: Int = 0): Int =
    ((bytes(offset + 1) & 0xFF) << 8) | (bytes(offset) & 0xFF)

  def isJPGStartOfFrameMarker(buffer: Array[Byte], pos:Int):Boolean =
    (buffer(pos) == 0xFF.toByte &&
    (buffer(pos + 1) & 0xFF) >= 0xC0 &&
    (buffer(pos + 1) & 0xFF) <= 0xCF &&
    buffer(pos + 1) != 0xC4.toByte &&
    buffer(pos + 1) != 0xC8.toByte &&
    buffer(pos + 1) != 0xCC.toByte)


  def getHalf(img: BufferedImage, side: Int, direction: Int): BufferedImage = {
    val signedSide = if (direction==0) side else 1-side
    val fullWidth = img.getWidth
    val halfWidth = fullWidth / 2
    val height = img.getHeight
    val halfImg = new BufferedImage(halfWidth, height, img.getType)
    val g: Graphics2D = halfImg.createGraphics()
    val srcX = if (signedSide == 0) 0 else fullWidth - halfWidth
    g.drawImage(img, 0, 0, halfWidth, height, srcX, 0, srcX + halfWidth, height, null)
    g.dispose()
    halfImg
  }
}